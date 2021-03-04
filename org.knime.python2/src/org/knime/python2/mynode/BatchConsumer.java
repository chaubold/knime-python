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

import org.knime.core.columnar.store.ColumnStore;
import org.knime.core.columnar.store.ColumnStoreFactory;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.columnar.ColumnStoreFactoryRegistry;
import org.knime.core.data.columnar.schema.ColumnarValueSchema;
import org.knime.core.data.columnar.schema.DefaultColumnarValueSchema;
import org.knime.core.data.columnar.table.ColumnarRowContainer;
import org.knime.core.data.columnar.table.UnsavedColumnarContainerTable;
import org.knime.core.data.v2.RowKeyType;
import org.knime.core.data.v2.ValueSchema;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;

public final class BatchConsumer {

    private final ColumnStoreFactory m_storeFactory;

    private final ColumnarValueSchema m_tableSchema;

    private final String m_storeRootDir;

    private final ColumnStore m_store;

    private int m_currentBatchIndex = 0;

    private int m_tableSize = 0;

    public BatchConsumer(final DataTableSpec spec) throws Exception {
        m_storeFactory = ColumnStoreFactoryRegistry.getOrCreateInstance().getFactorySingleton();
        m_tableSchema = new DefaultColumnarValueSchema(ValueSchema.create(spec, RowKeyType.CUSTOM,
            null /* TODO: why do we need a file-store handler in a _schema_?*/));
        // TODO: this should only need to be a read store
        m_store = m_storeFactory.createStore(m_tableSchema, ColumnarRowContainer.createTempDir());
        m_storeRootDir = m_store.getFile().getAbsolutePath();
    }

    public String getNextWriteFile() {
        return m_storeRootDir + File.separatorChar + "batch" + Integer.toString(m_currentBatchIndex++);
    }

    public void notifyDoneWritingBatch(final int batchSize) {
        // TODO: pass to store; domain calculation, etc.
        m_tableSize += batchSize;
    }

    BufferedDataTable createTable(final ExecutionContext exec) {
        return UnsavedColumnarContainerTable.create(-1, m_storeFactory, m_tableSchema, m_store, m_tableSize)
            .create(exec);
    }
}