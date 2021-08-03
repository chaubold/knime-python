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
 *   Jul 22, 2021 (marcel): created
 */
package org.knime.python3.legacy;

import java.io.Flushable;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.columnar.arrow.ArrowBatchReadStore;
import org.knime.core.columnar.arrow.ArrowBatchStore;
import org.knime.core.columnar.arrow.ArrowColumnStoreFactory;
import org.knime.core.columnar.batch.BatchReadable;
import org.knime.core.columnar.batch.DelegatingBatchReadable;
import org.knime.core.columnar.store.BatchReadStore;
import org.knime.core.columnar.store.ColumnStoreFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.columnar.schema.ColumnarValueSchema;
import org.knime.core.data.columnar.schema.DefaultColumnarValueSchema;
import org.knime.core.data.columnar.table.ColumnarContainerTable;
import org.knime.core.data.columnar.table.UnsavedColumnarContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.RowKeyType;
import org.knime.core.data.v2.ValueSchema;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.VariableTypeRegistry;
import org.knime.core.table.schema.BooleanDataSpec;
import org.knime.core.table.schema.ByteDataSpec;
import org.knime.core.table.schema.ColumnarSchema;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.DoubleDataSpec;
import org.knime.core.table.schema.DurationDataSpec;
import org.knime.core.table.schema.FloatDataSpec;
import org.knime.core.table.schema.IntDataSpec;
import org.knime.core.table.schema.ListDataSpec;
import org.knime.core.table.schema.LocalDateDataSpec;
import org.knime.core.table.schema.LocalDateTimeDataSpec;
import org.knime.core.table.schema.LocalTimeDataSpec;
import org.knime.core.table.schema.LongDataSpec;
import org.knime.core.table.schema.PeriodDataSpec;
import org.knime.core.table.schema.StringDataSpec;
import org.knime.core.table.schema.StructDataSpec;
import org.knime.core.table.schema.VarBinaryDataSpec;
import org.knime.core.table.schema.VoidDataSpec;
import org.knime.core.table.schema.ZonedDateTimeDataSpec;
import org.knime.python2.PythonCommand;
import org.knime.python2.PythonVersion;
import org.knime.python2.generic.ImageContainer;
import org.knime.python2.kernel.NodeContextManager;
import org.knime.python2.kernel.PythonCancelable;
import org.knime.python2.kernel.PythonIOException;
import org.knime.python2.kernel.PythonKernel;
import org.knime.python2.kernel.PythonKernelBackend;
import org.knime.python2.kernel.PythonKernelCleanupException;
import org.knime.python2.kernel.PythonKernelOptions;
import org.knime.python2.kernel.PythonOutputListeners;
import org.knime.python2.port.PickledObject;
import org.knime.python3.PythonDataSource;
import org.knime.python3.PythonExtension;
import org.knime.python3.PythonGateway;
import org.knime.python3.PythonPath;
import org.knime.python3.PythonPath.PythonPathBuilder;
import org.knime.python3.SimplePythonCommand;
import org.knime.python3.arrow.DefaultPythonArrowDataSink;
import org.knime.python3.arrow.PythonArrowDataUtils;
import org.knime.python3.arrow.PythonArrowExtension;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public final class NewPythonKernelBackend implements PythonKernelBackend {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(PythonKernel.class);

    // Do not change. Used on Python side. TODO: factor out output listening into own class. Can be shared by back ends
    private static final String WARNING_MESSAGE_PREFIX = "[WARN]";

    private final PythonGateway<NewPythonKernelBackendProxy> m_gateway;

    private final NewPythonKernelBackendProxy m_proxy;

    private final PythonOutputListeners m_outputListeners;

    /**
     * Properly initialized by {@link #setOptions(PythonKernelOptions)}. Holds the node context that was active at the
     * time when that method was called (if any).
     */
    private NodeContextManager m_nodeContextManager = new NodeContextManager();

    private PythonKernelOptions m_currentOptions;

    public NewPythonKernelBackend(final PythonCommand command) throws IOException {
        if (command.getPythonVersion() == PythonVersion.PYTHON2) {
            throw new IllegalArgumentException("The new Python kernel back end does not support Python 2 anymore. If " +
                "you still want to use Python 2, please change your settings to use the legacy kernel back end.");
        }

        // TODO: Consolidate knime.python2 and knime.python3 "PythonCommand"s?
        // Note that the current HACK only works with Manual preferences due to the use of toString.
        final SimplePythonCommand convertedCommand = new SimplePythonCommand(command.toString());
        final String launcherPath = // TODO
            "/home/marcel/git/knime-python/org.knime.python3.legacy/src/main/python/knime_kernel.py";
        final List<PythonExtension> extensions = Collections.singletonList(PythonArrowExtension.INSTANCE);
        final PythonPath pythonPath = new PythonPathBuilder() // FIXME: these do not resolve to absolute paths
            // .add(PythonModuleKnime.getPythonModule()) //
            // .add(PythonModuleKnimeArrow.getPythonModule()) //
            // .add(PythonModuleKnime.getPythonModuleFor(NewPythonKernelBackend.class)) //
            .add("/home/marcel/git/knime-python/org.knime.python3/src/main/python/") // TODO: see fix-me above
            .add(
                "/home/marcel/git/knime-python/org.knime.python3.arrow/src/main/python/") // TODO
            .add(
                "/home/marcel/git/knime-python/org.knime.python3.legacy/src/main/python/") // TODO
            .build();

        m_gateway = new PythonGateway<>(convertedCommand, launcherPath,
            NewPythonKernelBackendProxy.class, extensions, pythonPath);

        m_outputListeners = new PythonOutputListeners(m_gateway.getStandardOutputStream(),
            m_gateway.getStandardErrorStream(), m_nodeContextManager);
        m_outputListeners.startListening();

        m_proxy = m_gateway.getEntryPoint();

        // TODO: generalize module path. Ideally, devs would be able to debug their Python code outside of eclipse
        // See https://www.pydev.org/manual_adv_remote_debugger.html
        // m_proxy.enableDebugging(
        //    "/home/marcel/applications/eclipse/eclipse-java/plugins/org.python.pydev.core_8.3.0.202104101217/pysrc");
    }

    @Override
    public PythonCommand getPythonCommand() {
        // TODO: conversion between python2 and python3 "PythonCommand"s
        throw new IllegalStateException("not yet implemented"); // TODO: implement
    }

    @Override
    public PythonOutputListeners getOutputListeners() {
        return m_outputListeners;
    }

    @Override
    public PythonKernelOptions getOptions() {
        return m_currentOptions;
    }

    @Override
    public void setOptions(final PythonKernelOptions options) throws PythonIOException {
        m_currentOptions = options;
        // TODO: actually apply options

        m_nodeContextManager.setNodeContext(NodeContext.getContext()); // TODO: could be extracted to PythonKernel
    }

    @Override
    public void putFlowVariables(final String name, final Collection<FlowVariable> flowVariables)
        throws PythonIOException {
        final LinkedHashMap<String, Object> flowVariablesMap = new LinkedHashMap<>(flowVariables.size());
        for (final FlowVariable flowVariable : flowVariables) {
            final String key = flowVariable.getName();
            // HACK: let py4j figure out the Python types. TODO: real mapping mechanism. Also do that via Arrow?
            final Object value = flowVariable.getValue(flowVariable.getVariableType());
            flowVariablesMap.put(key, value);
        }
        m_proxy.putFlowVariables(name, flowVariablesMap);
    }

    @Override
    public Collection<FlowVariable> getFlowVariables(final String name) throws PythonIOException {
        final Map<String, Object> flowVariablesMap = m_proxy.getFlowVariables(name);
        final VariableType<?>[] variableTypes = VariableTypeRegistry.getInstance().getAllTypes();
        final Set<FlowVariable> flowVariables = new LinkedHashSet<>(flowVariablesMap.size());
        for (final var entry : flowVariablesMap.entrySet()) {
            final String variableName = entry.getKey();
            if (!isValidFlowVariableName(variableName)) {
                // TODO: I'd rather like to fail here or at least print a warning. But the old kernel just swallows
                // variables with invalid names.
                continue;
            }
            final Object variableValue = entry.getValue();
            VariableType<?> matchingVariableType = null;
            for (final var variableType : variableTypes) {
                if (variableType.getSimpleType().isInstance(variableValue)) {
                    matchingVariableType = variableType;
                    break;
                }
            }
            if (matchingVariableType == null) {
                continue;
                // TODO: Again, I'd rather like to fail, which is not what the old kernel does, though.
            }
            @SuppressWarnings({"unchecked", "rawtypes"})
            final FlowVariable flowVariable =
                new FlowVariable(variableName, (VariableType)matchingVariableType, variableValue);
            flowVariables.add(flowVariable);
        }
        return flowVariables;
    }

    private static boolean isValidFlowVariableName(final String name) {
        return !(name.startsWith(FlowVariable.Scope.Global.getPrefix()) ||
            name.startsWith(FlowVariable.Scope.Local.getPrefix()));
    }

    @Override
    public void putDataTable(final String name, final BufferedDataTable table, final ExecutionMonitor executionMonitor,
        final int rowLimit) throws PythonIOException, CanceledExecutionException {
        // TODO: see method below, make cancelable
        try {
            // TODO: the table-to-provider step could be optimized by only flushing as far as necessary to reach
            // rowLimit (once per-batch flushing is possible).
            m_proxy.putTableIntoWorkspace(name, tableToSource(table), rowLimit);
        } catch (IOException ex) {
            throw new PythonIOException(ex);
        }
    }

    @Override
    public void putDataTable(final String name, final BufferedDataTable table, final ExecutionMonitor executionMonitor)
        throws PythonIOException, CanceledExecutionException {
        // TODO: make this step cancelable (e.g. in case flushing caches or similar takes long)? If so, what are we
        // supposed to do upon cancellation? Pass on to the caches? Or simply continue execution here and let them
        // continue in the background?
        try {
            m_proxy.putTableIntoWorkspace(name, tableToSource(table));
        } catch (IOException ex) {
            throw new PythonIOException(ex);
        }
    }

    @Override
    public BufferedDataTable getDataTable(final String name, final ExecutionContext exec,
        final ExecutionMonitor executionMonitor) throws PythonIOException, CanceledExecutionException {
        // TODO: cancellation
        // TODO: consolidate creation of temp. file, etc. with logic from ColumnarRowContainer
        try {
            final DefaultPythonArrowDataSink callback =
                PythonArrowDataUtils.createSink(DataContainer.createTempFile(".knable").toPath());
            m_proxy.getTableFromWorkspace(name, callback);
            return sinkToTable(callback, exec);
        } catch (final IOException ex) {
            throw new PythonIOException(ex);
        }
    }

    @Override
    public void putObject(final String name, final PickledObject object) throws PythonIOException {
        // Should be done in a file-based way. We need to store the objects in file stores anyway.
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI
    }

    @Override
    public void putObject(final String name, final PickledObject object, final ExecutionMonitor executionMonitor)
        throws PythonIOException, CanceledExecutionException {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI
    }

    @Override
    public PickledObject getObject(final String name, final ExecutionMonitor executionMonitor)
        throws PythonIOException, CanceledExecutionException {
        // Again, should be done in a file-based way.
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI
    }

    @Override
    public void putSql(final String name, final DatabaseQueryConnectionSettings conn, final CredentialsProvider cp,
        final Collection<String> jars) throws PythonIOException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public String getSql(final String name) throws PythonIOException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public ImageContainer getImage(final String name) throws PythonIOException {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI
    }

    @Override
    public ImageContainer getImage(final String name, final ExecutionMonitor executionMonitor)
        throws PythonIOException, CanceledExecutionException {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI
    }

    @Override
    public List<Map<String, String>> listVariables() throws PythonIOException {
        return m_proxy.listVariables();
    }

    @Override
    public List<Map<String, String>> autoComplete(final String sourceCode, final int line, final int column)
        throws PythonIOException {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI
    }

    @Override
    public String[] execute(final String sourceCode) throws PythonIOException {
        return m_proxy.executeOnMainThread(sourceCode).toArray(String[]::new);
    }

    @Override
    public String[] execute(final String sourceCode, final PythonCancelable cancelable)
        throws PythonIOException, CanceledExecutionException {
        // TODO: cancellation
        return m_proxy.executeOnMainThread(sourceCode).toArray(String[]::new);
    }

    @Override
    public String[] executeAsync(final String sourceCode) throws PythonIOException {
        return m_proxy.executeOnCurrentThread(sourceCode).toArray(String[]::new);
    }

    @Override
    public String[] executeAsync(final String sourceCode, final PythonCancelable cancelable)
        throws PythonIOException, CanceledExecutionException {
        // TODO: cancellation
        return m_proxy.executeOnCurrentThread(sourceCode).toArray(String[]::new);
    }

    @Override
    public void close() throws PythonKernelCleanupException {
        // TODO: make operations safe, exception delivery, multi-threading & mutex, etc. See old backend.
        try {
            m_outputListeners.close();
            m_gateway.close();
        } catch (final Exception ex) {
            throw new PythonKernelCleanupException(ex);
        }
    }

    // --

    // TODO: implement in Arrow plugin ---> TICKET
    private static PythonDataSource tableToSource(final BufferedDataTable table) throws IOException {
        final KnowsRowCountTable delegate = table.getDelegate();
        BatchReadable readable = null;
        if (delegate instanceof ColumnarContainerTable) {
            readable = ((ColumnarContainerTable)delegate).getStore();
            // Traverse decorators until reaching the actual underlying store which must be an Arrow store in order for
            // IPC to work. During traversal, flush caches to disk to make the data available to Python.
            // TODO: ideally, we want to be able to flush per batch/up to some batch index. Once this is possible, defer
            // flushing until actually needed.
            // TODO: do we need to flush all caches or only the outermost one which in turn triggers the inner ones?
            while (readable instanceof DelegatingBatchReadable) {
                if (readable instanceof Flushable) {
                    ((Flushable)readable).flush();
                }
                readable = ((DelegatingBatchReadable)readable).getDelegateBatchReadable();
            }
        }
        if (readable instanceof ArrowBatchReadStore) {
            return PythonArrowDataUtils.createSource((ArrowBatchReadStore)readable);
        } else if (readable instanceof ArrowBatchStore) {
            final ArrowBatchStore store = (ArrowBatchStore)readable;
            return PythonArrowDataUtils.createSource(store, store.numBatches());
        } else {
            throw new IllegalStateException(
                "Arrow IPC requires the Arrow implementation of the columnar table back end to be enabled.");
        }
    }

    // TODO: move to Arrow plugin
    private static BufferedDataTable sinkToTable(final DefaultPythonArrowDataSink sink, final ExecutionContext exec) {
        final ColumnStoreFactory storeFactory = new ArrowColumnStoreFactory();
        // TODO: this step won't be necessary anymore once we have logical types on the Python side. This is just a
        // temporary HACK for demonstration purposes. We will also need to get the column names (and domains, etc.) from
        // Python!
        final ColumnarValueSchema schema =
            physicalToLogicalSchema(sink.getSchema(), sink.getMinsMaxs(), sink.getNominalValues());

        // TODO: this HACK only works because we write out the entire pandas.DataFrame as pyarrow batches at once.
        // Otherwise, we'd have to create a ArrowPartialFileBatchReadable.
        final BatchReadStore store = storeFactory.createReadStore(sink.getSchema(), sink.getPath());
        NodeLogger.getLogger(NewPythonKernelBackend.class)
            .coding("FIXME: also support tables whose last batch is shorter");
        final long tableSize = store.numBatches() * (long)store.batchLength(); // FIXME
        // TODO: logical types, duplicate checking, domain
        return UnsavedColumnarContainerTable.create(Paths.get(sink.getAbsolutePath()), -1 /* HACK */, storeFactory,
            schema, store, () -> {} /* HACK */, tableSize).create(exec);
    }

    // TODO: remove once we have logical types on the Python side
    private static ColumnarValueSchema physicalToLogicalSchema(final ColumnarSchema schema,
        final Map<Integer, List<Object>> minsMaxs, final Map<Integer, List<Object>> nominalValues) {
        // Skip first column, which is the row key. Will be specified further below.
        final DataColumnSpec[] columnSpecs = new DataColumnSpec[schema.numColumns() - 1];
        for (int i = 1; i < schema.numColumns(); i++) {
            final DataSpec spec = schema.getSpec(i);
            final DataColumnSpecCreator columnCreator =
                new DataColumnSpecCreator("unnamed_column_" + (i - 1), spec.accept(DataSpecToDataTypeMapper.INSTANCE));
            DataColumnDomainCreator domainCreator = new DataColumnDomainCreator();
            final List<Object> minMax = minsMaxs.get(i);
            if (minMax != null) {
                final DataCell lower;
                final DataCell upper;
                Object min = minMax.get(0);
                Object max = minMax.get(1);
                // TODO: make extensible
                if (max instanceof Double) {
                    lower = new DoubleCell((double)min);
                    upper = new DoubleCell((double)max);
                } else if (max instanceof Integer) {
                    lower = new IntCell((int)min);
                    upper = new IntCell((int)max);
                } else if (max instanceof Long) {
                    lower = new LongCell((long)min);
                    upper = new LongCell((long)max);
                } else {
                    throw new IllegalStateException("Unsupported domain type: " + max.getClass());
                }
                domainCreator.setLowerBound(lower);
                domainCreator.setUpperBound(upper);
            }
            final List<Object> nominals = nominalValues.get(i);
            if (nominals != null) {
                Set<? extends DataCell> values;
                final Object first = nominals.get(0);
                if (first instanceof Boolean) {
                    values = nominals.stream().map(o -> ((boolean)o) ? BooleanCell.TRUE : BooleanCell.FALSE)
                        .collect(Collectors.toSet());
                } else if (first instanceof String) {
                    values = nominals.stream().map(o -> new StringCell((String)o)).collect(Collectors.toSet());
                } else {
                    throw new IllegalStateException("Unsupported domain type: " + first.getClass());
                }
                domainCreator.setValues(values);
            }
            columnCreator.setDomain(domainCreator.createDomain());
            // TODO: what about DataColumnMetaData?
            columnSpecs[i - 1] = columnCreator.createSpec();
        }
        final DataTableSpec tableSpec = new DataTableSpec(columnSpecs);
        return new DefaultColumnarValueSchema(ValueSchema.create(tableSpec, RowKeyType.CUSTOM, null /* HACK */));
    }

    private static final class DataSpecToDataTypeMapper implements DataSpec.Mapper<DataType> {

        private static final DataSpecToDataTypeMapper INSTANCE = new DataSpecToDataTypeMapper();

        @Override
        public DataType visit(final BooleanDataSpec spec) {
            return BooleanCell.TYPE;
        }

        @Override
        public DataType visit(final ByteDataSpec spec) {
            throw new UnsupportedOperationException("not yet implemented");
        }

        @Override
        public DataType visit(final DoubleDataSpec spec) {
            return DoubleCell.TYPE;
        }

        @Override
        public DataType visit(final DurationDataSpec spec) {
            throw new UnsupportedOperationException("not yet implemented");
        }

        @Override
        public DataType visit(final FloatDataSpec spec) {
            throw new UnsupportedOperationException("not yet implemented");
        }

        @Override
        public DataType visit(final IntDataSpec spec) {
            return IntCell.TYPE;
        }

        @Override
        public DataType visit(final LocalDateDataSpec spec) {
            throw new UnsupportedOperationException("not yet implemented");
        }

        @Override
        public DataType visit(final LocalDateTimeDataSpec spec) {
            throw new UnsupportedOperationException("not yet implemented");
        }

        @Override
        public DataType visit(final LocalTimeDataSpec spec) {
            throw new UnsupportedOperationException("not yet implemented");
        }

        @Override
        public DataType visit(final LongDataSpec spec) {
            return LongCell.TYPE;
        }

        @Override
        public DataType visit(final PeriodDataSpec spec) {
            throw new UnsupportedOperationException("not yet implemented");
        }

        @Override
        public DataType visit(final VarBinaryDataSpec spec) {
            throw new UnsupportedOperationException("not yet implemented");
        }

        @Override
        public DataType visit(final VoidDataSpec spec) {
            throw new UnsupportedOperationException("not yet implemented");
        }

        @Override
        public DataType visit(final StructDataSpec spec) {
            throw new UnsupportedOperationException("StructData or dict-encoding not yet implemented");
        }

        @Override
        public DataType visit(final ListDataSpec listDataSpec) {
            throw new UnsupportedOperationException("not yet implemented");
        }

        @Override
        public DataType visit(final ZonedDateTimeDataSpec spec) {
            throw new UnsupportedOperationException("not yet implemented");
        }

        @Override
        public DataType visit(final StringDataSpec spec) {
            return StringCell.TYPE;
        }
    }
}
