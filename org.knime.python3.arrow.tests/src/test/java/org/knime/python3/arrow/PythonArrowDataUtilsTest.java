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
 *   Jul 16, 2021 (benjamin): created
 */
package org.knime.python3.arrow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.knime.core.table.schema.DataSpecs.BYTE;
import static org.knime.core.table.schema.DataSpecs.DOUBLE;
import static org.knime.core.table.schema.DataSpecs.INT;
import static org.knime.core.table.schema.DataSpecs.LIST;
import static org.knime.core.table.schema.DataSpecs.LONG;
import static org.knime.core.table.schema.DataSpecs.STRING;
import static org.knime.core.table.schema.DataSpecs.STRUCT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.columnar.arrow.ArrowBatchReadStore;
import org.knime.core.columnar.arrow.ArrowBatchStore;
import org.knime.core.columnar.arrow.ArrowColumnStoreFactory;
import org.knime.core.columnar.arrow.compress.ArrowCompressionUtil;
import org.knime.core.columnar.batch.BatchWriter;
import org.knime.core.columnar.batch.WriteBatch;
import org.knime.core.columnar.data.IntData.IntReadData;
import org.knime.core.columnar.data.IntData.IntWriteData;
import org.knime.core.table.schema.ColumnarSchema;

/**
 * Test different special cases for transferring Arrow data between Java and Python.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public class PythonArrowDataUtilsTest {

    private ArrowColumnStoreFactory m_storeFactory;

    private BufferAllocator m_allocator;

    /** Create allocator and storeFactory */
    @Before
    public void before() {
        m_allocator = new RootAllocator();
        m_storeFactory = new ArrowColumnStoreFactory(m_allocator, 0, m_allocator.getLimit(),
            ArrowCompressionUtil.ARROW_NO_COMPRESSION);
    }

    /** Close allocator */
    @After
    public void after() {
        m_allocator.close();
    }

    /**
     * Test
     * {@link PythonArrowDataUtils#createReadable(DefaultPythonArrowDataSink, ColumnarSchema, ArrowColumnStoreFactory)}.
     *
     * @throws Exception
     */
    @Test
    public void testExpectedSchema() throws Exception {
        final var path = TestUtils.createTmpKNIMEArrowPath();

        try (final var pythonGateway = TestUtils.openPythonGateway()) {
            final var entryPoint = pythonGateway.getEntryPoint();
            final DefaultPythonArrowDataSink dataSink = PythonArrowDataUtils.createSink(path);
            entryPoint.testExpectedSchema(dataSink);

            // Expected schema - should work
            final var trueSchema = ColumnarSchema.of(INT, STRING, STRUCT.of(LIST.of(INT), DOUBLE));
            try (var r = PythonArrowDataUtils.createReadable(dataSink, trueSchema, m_storeFactory)) {}

            // Schema too short - should fail
            final var falseSchema1 = ColumnarSchema.of(INT, STRING);
            assertThrows(IllegalStateException.class,
                () -> PythonArrowDataUtils.createReadable(dataSink, falseSchema1, m_storeFactory));

            // Schema wrong - should fail
            final var falseSchema2 = ColumnarSchema.of(LONG, STRING, STRUCT.of(LIST.of(INT), DOUBLE));
            assertThrows(IllegalStateException.class,
                () -> PythonArrowDataUtils.createReadable(dataSink, falseSchema2, m_storeFactory));

            // Schema wrong - should fail
            final var falseSchema3 = ColumnarSchema.of(INT, STRING, STRUCT.of(LIST.of(INT), BYTE));
            assertThrows(IllegalStateException.class,
                () -> PythonArrowDataUtils.createReadable(dataSink, falseSchema3, m_storeFactory));
        }
    }

    /**
     * Test transfer of multiple tables to Python and from Python as a List of sources/sinks.
     *
     * @throws Exception
     */
    @Test
    @SuppressWarnings({"resource", "null"})
    public void testMultipleInputsOutputs() throws Exception {
        try (final var pythonGateway = TestUtils.openPythonGateway()) {
            final var entryPoint = pythonGateway.getEntryPoint();

            // Create the data sources
            BatchWriter writer = null;
            final List<AutoCloseable> stores = new ArrayList<>();
            final List<PythonArrowDataSource> sources = new ArrayList<>();
            for (int idx = 0; idx < 4; idx++) {
                if (idx == 2) {
                    // Footer not written
                    final var store = createWriteStore();
                    writer = store.getWriter();
                    writeData(writer, idx);

                    stores.add(store);
                    sources.add(PythonArrowDataUtils.createSource(store, 1));
                } else {
                    // Footer written
                    final var store = createReadStore(idx);
                    stores.add(store);
                    sources.add(PythonArrowDataUtils.createSource(store));
                }
            }

            // Create the data sinks
            final List<DefaultPythonArrowDataSink> sinks = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                final Path p = TestUtils.createTmpKNIMEArrowPath();
                sinks.add(PythonArrowDataUtils.createSink(p));
            }
            entryPoint.testMultipleInputsOutputs(sources, sinks);

            // Close the stores
            writer.close(); // NOSONAR: We know that writer is not null
            for (final var s : stores) {
                s.close();
            }

            // Check the data from Python
            for (int idx = 0; idx < sinks.size(); idx++) {
                checkReadable(sinks.get(idx), idx);
            }
        }
    }

    private ArrowBatchStore createWriteStore() throws IOException {
        final var schema = ColumnarSchema.of(INT);
        final var path = TestUtils.createTmpKNIMEArrowFileHandle();
        return m_storeFactory.createStore(schema, path);
    }

    private ArrowBatchReadStore createReadStore(final int idx) throws IOException {
        final var path = TestUtils.createTmpKNIMEArrowPath();
        try (final var writeStore = createWriteStore()) {
            try (final var writer = writeStore.getWriter()) {
                writeData(writer, idx);
            }
            Files.copy(writeStore.getFileHandle().asPath(), path, StandardCopyOption.REPLACE_EXISTING);
        }

        return m_storeFactory.createReadStore(path);
    }

    private void checkReadable(final DefaultPythonArrowDataSink dataSink, final int idx) throws IOException {
        try (final var readable = PythonArrowDataUtils.createReadable(dataSink, m_storeFactory);
                var reader = readable.createRandomAccessReader()) {
            final var batch = reader.readRetained(0);
            assertEquals(1, batch.length());
            assertEquals(1, batch.numData());
            final var data = (IntReadData)batch.get(0);
            assertEquals(idx, data.getInt(0));
            batch.release();
        }
    }

    private static void writeData(final BatchWriter writer, final int idx) throws IOException {
        final WriteBatch batch = writer.create(1);
        ((IntWriteData)batch.get(0)).setInt(0, idx);
        final var readBatch = batch.close(1);
        writer.write(readBatch);
        readBatch.release();
    }
}
