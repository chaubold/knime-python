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
import java.util.ArrayList;
import java.util.List;

import org.knime.core.columnar.arrow.ArrowColumnStore;
import org.knime.core.columnar.cache.AsyncFlushCachedColumnStore;
import org.knime.core.columnar.cache.SmallColumnStore;
import org.knime.core.columnar.cache.heap.HeapCachedColumnStore;
import org.knime.core.columnar.store.ColumnReadStore;
import org.knime.core.columnar.store.DelegatingColumnReadStore;
import org.knime.core.columnar.store.DelegatingColumnStore;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.columnar.table.ColumnarContainerTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;

public final class ArrowIPCBatchSupplier {

    @FunctionalInterface
    private interface CacheFlusher {

        void flushBatch(int batchIndex) throws IOException;
    }

    private final DataTableSpec m_tableSpec;

    private final ArrowColumnStore m_store;

    private final List<CacheFlusher> m_cacheFlushers;

    private int m_currentBatchIndex = 0;

    public ArrowIPCBatchSupplier(final BufferedDataTable table) {
        m_tableSpec = table.getDataTableSpec();
        final KnowsRowCountTable delegate = table.getDelegate();
        if (delegate instanceof ColumnarContainerTable) {
            // Traverse store decorators until reaching the actual underlying store which must be an Arrow store in
            // order for IPC to work. During traversal, keep track of caches which may need to be flushed to disked to
            // make the data available to other processes.
            m_cacheFlushers = new ArrayList<>();
            ColumnReadStore store = ((ColumnarContainerTable)delegate).getColumnStore();
            while (store instanceof DelegatingColumnReadStore || store instanceof DelegatingColumnStore) {
                final ColumnReadStore finalStore = store;
                // TODO: ideally, we want to be able to flush per batch
                // TODO: do we need to keep track of all caches or only the outermost one?
                if (store instanceof AsyncFlushCachedColumnStore) {
                    m_cacheFlushers.add(ignoreBatchIndex -> finalStore.close());
                } else if (store instanceof SmallColumnStore) {
                    m_cacheFlushers.add(ignoreBatchIndex -> ((SmallColumnStore)finalStore).flush());
                } else if (store instanceof HeapCachedColumnStore) {
                    m_cacheFlushers.add(ignoreBatchIndex -> finalStore.close());
                }

                if (store instanceof DelegatingColumnReadStore) {
                    store = ((DelegatingColumnReadStore)store).getDelegate();
                } else {
                    store = ((DelegatingColumnStore)store).getDelegate();
                }
            }
            if (store instanceof ArrowColumnStore) {
                m_store = (ArrowColumnStore)store;
                return;
            }
        }
        throw new IllegalStateException(
            "Arrow IPC requires the Arrow implementation of the columnar table back end to be enabled.");
    }

    public DataTableSpec getTableSpec() {
        return m_tableSpec;
    }

    public String getSchemaFile() {
        return m_store.getSchemaFile().getAbsolutePath();
    }

    public String getNextBatchFile() throws IOException {
        for (final CacheFlusher flusher : m_cacheFlushers) {
            flusher.flushBatch(m_currentBatchIndex);
        }

        // TODO: store needs to notify clients if the end of the table is reached
        final File batchFile = m_store.getBatchFile(m_currentBatchIndex);
        m_currentBatchIndex++;

        return batchFile.getAbsolutePath();
    }
}