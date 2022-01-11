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
 *   Jan 17, 2022 (benjamin): created
 */
package org.knime.python3.views.node;

import java.io.Flushable;
import java.io.IOException;

import org.knime.core.columnar.arrow.ArrowBatchReadStore;
import org.knime.core.columnar.arrow.ArrowBatchStore;
import org.knime.core.data.columnar.schema.ColumnarValueSchemaUtils;
import org.knime.core.data.columnar.table.ColumnarBatchReadStore;
import org.knime.core.data.columnar.table.ColumnarContainerTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.Node;
import org.knime.python3.arrow.PythonArrowDataSource;
import org.knime.python3.arrow.PythonArrowDataUtils;

/*
 * Mostly copied from Python3KernelBackend.
 *
 * TODO Make work for legacy tables (See Python3KernelBackend)
 * TODO Make a general utility that works for all
 */

public class BufferedDataTablePythonArrowUtils {

    public static PythonArrowDataSource toPythonDataSource(final BufferedDataTable table) throws IOException {
        final var columnarStore = extractStoreCopyTableIfNecessary(table);
        return convertStoreIntoSource(columnarStore, table.getDataTableSpec().getColumnNames());
    }

    // Store will be closed along with table. If it is a copy, it will have already been closed.
    @SuppressWarnings("resource")
    private static ColumnarBatchReadStore extractStoreCopyTableIfNecessary(final BufferedDataTable table)
        throws IOException {
        final KnowsRowCountTable delegate = Node.invokeGetDelegate(table);
        if (delegate instanceof ColumnarContainerTable) {
            var columnarTable = (ColumnarContainerTable)delegate;
            final var baseStore = columnarTable.getStore().getDelegateBatchReadStore();
            final boolean isLegacyArrow;
            if (baseStore instanceof ArrowBatchReadStore) {
                isLegacyArrow = ((ArrowBatchReadStore)baseStore).isUseLZ4BlockCompression()
                    || ColumnarValueSchemaUtils.storesDataCellSerializersSeparately(columnarTable.getSchema());
            } else if (baseStore instanceof ArrowBatchStore) {
                // Write stores shouldn't be using the old compression format or the old ValueSchema anymore
                isLegacyArrow = false;
            } else {
                // Not Arrow at all (= a new storage back end), treat like legacy, i.e. copy.
                isLegacyArrow = true;
            }
            if (!isLegacyArrow) {
                return ((ColumnarContainerTable)delegate).getStore();
            }
        }
        // TODO Make work for legacy tables (See Python3KernelBackend)
        throw new IllegalStateException("Legacy tables are not yet implemented");
    }

    // Store will be closed along with table. If it is a copy, it will have already been closed.
    @SuppressWarnings("resource")
    private static PythonArrowDataSource convertStoreIntoSource(final ColumnarBatchReadStore columnarStore,
        final String[] columnNames) throws IOException {
        // Unwrap the underlying physical Arrow store from the table. Along the way, flush any cached table
        // content to disk to make it available to Python.
        //
        // TODO: ideally, we want to be able to flush per batch/up to some batch index. Once this is supported,
        // defer flushing until actually needed (i.e. when Python pulls data).
        if (columnarStore instanceof Flushable) {
            ((Flushable)columnarStore).flush();
        }
        final var baseStore = columnarStore.getDelegateBatchReadStore();
        if (baseStore instanceof ArrowBatchReadStore) {
            final ArrowBatchReadStore store = (ArrowBatchReadStore)baseStore;
            return PythonArrowDataUtils.createSource(store, columnNames);
        } else if (baseStore instanceof ArrowBatchStore) {
            final ArrowBatchStore store = (ArrowBatchStore)baseStore;
            return PythonArrowDataUtils.createSource(store, store.numBatches(), columnNames);
        } else {
            // Any non-Arrow store should already have been copied into an Arrow store further above.
            throw new IllegalStateException(
                "Unrecognized store type: " + baseStore.getClass().getName() + ". This is an implementation error.");
        }
    }
}
