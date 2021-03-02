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
 *   Dec 21, 2019 (marcel): created
 */
package org.knime.python2.mynode;

import java.io.File;
import java.io.IOException;

import org.knime.core.columnar.store.ColumnStore;
import org.knime.core.columnar.store.ColumnStoreFactory;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.columnar.ColumnStoreFactoryRegistry;
import org.knime.core.data.columnar.schema.ColumnarValueSchema;
import org.knime.core.data.columnar.schema.DefaultColumnarValueSchema;
import org.knime.core.data.columnar.table.UnsavedColumnarContainerTable;
import org.knime.core.data.v2.RowKeyType;
import org.knime.core.data.v2.ValueSchema;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.ExtensionTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.python2.gateway.PythonGateway;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public class PythonWrappingNodeModel extends NodeModel {

    private final PythonGateway m_gateway;

    private final PythonNodeModel m_pythonNodeModel;

    public PythonWrappingNodeModel(final PythonGateway gateway, final PythonNodeModel pythonNodeModel) {
        super(pythonNodeModel.getInputPortTypes(), pythonNodeModel.getOutputPortTypes());
        m_gateway = gateway;
        m_pythonNodeModel = pythonNodeModel;
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        m_pythonNodeModel.loadInternals(nodeInternDir, exec);
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        m_pythonNodeModel.saveInternals(nodeInternDir, exec);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_pythonNodeModel.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_pythonNodeModel.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_pythonNodeModel.loadValidatedSettingsFrom(settings);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return m_pythonNodeModel.configure(inSpecs);
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final Object[] inObjectsWithBDTsReplacedByPythonHandles = new Object[inObjects.length];
        for (int i = 0; i < inObjects.length; i++) {
            final PortObject inObject = inObjects[i];
            if (inObject instanceof BufferedDataTable) {
                final KnowsRowCountTable delegate = ((BufferedDataTable)inObject).getDelegate();
                final String tableFilePath = ((ExtensionTable)delegate).getFile().getAbsolutePath();
                inObjectsWithBDTsReplacedByPythonHandles[i] = new Object[]{delegate.getDataTableSpec(), tableFilePath};
            } else {
                inObjectsWithBDTsReplacedByPythonHandles[i] = inObject;
            }
        }
        final Object[][] outObjectsWithPythonHandlesInsteadOfBDTs =
            m_pythonNodeModel.execute(inObjectsWithBDTsReplacedByPythonHandles, exec);
        final PortObject[] outObjects = new PortObject[outObjectsWithPythonHandlesInsteadOfBDTs.length];
        for (int i = 0; i < outObjectsWithPythonHandlesInsteadOfBDTs.length; i++) {
            final Object outObject = outObjectsWithPythonHandlesInsteadOfBDTs[i][1];
            if (outObject instanceof String) {
                final ColumnStoreFactory storeFactory =
                    ColumnStoreFactoryRegistry.getOrCreateInstance().getFactorySingleton();
                final DataTableSpec outTableSpec = (DataTableSpec)outObjectsWithPythonHandlesInsteadOfBDTs[i][0];
                final ColumnarValueSchema outTableSchema =
                    new DefaultColumnarValueSchema(ValueSchema.create(outTableSpec, RowKeyType.CUSTOM,
                        null /* TODO: why do we need a file-store handler in a _schema_?*/));
                // TODO: this should only be a read store
                final ColumnStore store = storeFactory.createStore(outTableSchema, new File((String)outObject));
                final long tableSize = ((Number)outObjectsWithPythonHandlesInsteadOfBDTs[i][2]).longValue();
                final UnsavedColumnarContainerTable outTable =
                    UnsavedColumnarContainerTable.create(-1, storeFactory, outTableSchema, store, tableSize);
                outObjects[i] = outTable.create(exec);
            } else {
                outObjects[i] = (PortObject)outObject;
            }
        }
        return outObjects;
    }

    @Override
    protected void reset() {
        m_pythonNodeModel.reset();
    }
}
