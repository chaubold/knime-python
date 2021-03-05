/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 5, 2021 (marcel): created
 */
package org.knime.python2.mynode;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import org.knime.core.columnar.batch.ReadBatch;
import org.knime.core.columnar.filter.ColumnSelection;
import org.knime.core.columnar.store.BatchFactory;
import org.knime.core.columnar.store.BatchReader;
import org.knime.core.columnar.store.BatchWriter;
import org.knime.core.columnar.store.ColumnStore;
import org.knime.core.columnar.store.ColumnStoreFactory;
import org.knime.core.columnar.store.ColumnStoreSchema;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.columnar.ColumnStoreFactoryRegistry;
import org.knime.core.data.columnar.domain.DefaultDomainStoreConfig;
import org.knime.core.data.columnar.domain.DomainColumnStore;
import org.knime.core.data.columnar.domain.DuplicateCheckColumnStore;
import org.knime.core.data.columnar.schema.ColumnarValueSchema;
import org.knime.core.data.columnar.schema.ColumnarValueSchemaUtils;
import org.knime.core.data.columnar.schema.DefaultColumnarValueSchema;
import org.knime.core.data.columnar.table.ColumnarRowContainer;
import org.knime.core.data.columnar.table.UnsavedColumnarContainerTable;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.data.meta.DataColumnMetaData;
import org.knime.core.data.v2.RowKeyType;
import org.knime.core.data.v2.ValueSchema;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.DuplicateChecker;
import org.knime.core.util.ThreadUtils;

public final class BatchConsumer {

    private final ColumnStoreFactory m_storeFactory;

    private final ColumnarValueSchema m_tableSchema;

    private final String m_storeRootDir;

    private final ColumnStore m_store;

    private final BatchReader m_storeReader;

    private final DomainColumnStore m_domainStore;

    private int m_currentBatchIndex = 0;

    private int m_tableSize = 0;

    private ColumnStore m_nopStore = new ColumnStore() {

        private final BatchWriter m_nopWriter = new BatchWriter() {

            @Override
            public void write(final ReadBatch batch) throws IOException {}

            @Override
            public void close() throws IOException {}
        };

        @Override
        public ColumnStoreSchema getSchema() {
            return null;
        }

        @Override
        public File getFile() {
            return null;
        }

        @Override
        public BatchReader createReader(final ColumnSelection selection) {
            return null;
        }

        @Override
        public void close() throws IOException {}

        @Override
        public void save(final File file) throws IOException {}

        @Override
        public BatchWriter getWriter() {
            return m_nopWriter;
        }

        @Override
        public BatchFactory getFactory() {
            return null;
        }
    };

    public BatchConsumer(final DataTableSpec spec) throws Exception {
        m_storeFactory = ColumnStoreFactoryRegistry.getOrCreateInstance().getFactorySingleton();
        m_tableSchema = new DefaultColumnarValueSchema(ValueSchema.create(spec, RowKeyType.CUSTOM,
            null /* TODO: why do we need a file-store handler in a _schema_?*/));
        // TODO: this should only need to be a read store
        m_store = m_storeFactory.createStore(m_tableSchema, ColumnarRowContainer.createTempDir());
        m_storeRootDir = m_store.getFile().getAbsolutePath();
        m_storeReader = m_store.createReader();

        // TOOD: in general: all of this must be consolidated with ColumnarRowContainer
        m_domainStore = new DomainColumnStore(new DuplicateCheckColumnStore(m_nopStore, new DuplicateChecker(),
            // TODO: must adhere to #threads set in preferences
            ThreadUtils.executorServiceWithContext(Executors.newFixedThreadPool(4))),
            // TODO: must be configurable
            new DefaultDomainStoreConfig(m_tableSchema, DataContainerSettings.getDefault().getMaxDomainValues(),
                DataContainerSettings.getDefault().getInitializeDomain()),
            // TODO: must adhere to #threads set in preferences
            ThreadUtils.executorServiceWithContext(Executors.newFixedThreadPool(4)));
    }

    public String getNextWriteFile() {
        return m_storeRootDir + File.separatorChar + "batch" + Integer.toString(m_currentBatchIndex++);
    }

    public void notifyDoneWritingBatch(final int batchSize) throws IOException {
        final ReadBatch batch = m_storeReader.readRetained(m_currentBatchIndex - 1);
        try {
            m_domainStore.getWriter().write(batch);
        } finally {
            batch.release();
        }
        m_tableSize += batchSize;
    }

    BufferedDataTable createTable(final ExecutionContext exec) {
        final Map<Integer, DataColumnDomain> domains = new HashMap<>();
        final Map<Integer, DataColumnMetaData[]> metadata = new HashMap<>();
        final int numColumns = m_tableSchema.numColumns();
        for (int i = 1; i < numColumns; i++) {
            domains.put(i, m_domainStore.getDomain(i));
            metadata.put(i, m_domainStore.getMetadata(i));
        }
        final ColumnarValueSchema schemaWithDomain =
            ColumnarValueSchemaUtils.updateSource(m_tableSchema, domains, metadata);
        return UnsavedColumnarContainerTable.create(-1, m_storeFactory, schemaWithDomain, m_store, m_tableSize)
            .create(exec);
    }
}