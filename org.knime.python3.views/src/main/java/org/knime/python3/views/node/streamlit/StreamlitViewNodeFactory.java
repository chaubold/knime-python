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
 *   Jul 9, 2021 (hornm): created
 */
package org.knime.python3.views.node.streamlit;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.data.ApplyDataService;
import org.knime.core.webui.data.DataService;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.node.view.NodeView;
import org.knime.core.webui.node.view.NodeViewFactory;
import org.knime.core.webui.page.Page;
import org.knime.python3.Python3SourceDirectory;
import org.knime.python3.PythonDataSource;
import org.knime.python3.PythonEntryPoint;
import org.knime.python3.PythonExtension;
import org.knime.python3.PythonGateway;
import org.knime.python3.PythonPath.PythonPathBuilder;
import org.knime.python3.PythonSourceDirectoryLocator;
import org.knime.python3.SimplePythonCommand;
import org.knime.python3.arrow.Python3ArrowSourceDirectory;
import org.knime.python3.arrow.PythonArrowDataSource;
import org.knime.python3.arrow.PythonArrowExtension;
import org.knime.python3.views.node.BufferedDataTablePythonArrowUtils;
import org.knime.python3.views.node.streamlit.StreamlitViewNodeFactory.StreamlitViewNodeModel;

/*
 * TODO If the workflow is executed on an executor the view cannot communicate with the
 * executor and therefore with the streamlit server.
 * We need to tunnel the communication, let the streamlit server run somewhere else, or
 * provide the API of the streamlit server at the KNIME Hub.
 *
 * TODO Handle the lifetime of the Python/Streamlit process:
 * Currently the process is started with NodeView#getPage and stopped at NodeModel#reset.
 * We should stop the streamlit server after the view has been closed for some time.
 * How do we know that the view was closed?
 *
 * NOTE:
 * Requires a Python environment with "python", "pyarrow", "py4j", "streamlit", "seaborn".
 * Replace the PYTHON_PATH variable with the path to the Python executable in this environment.
 *
 * NOTE:
 * The streamlit server is started on port 8501. Add the JVM Argument
 * "-Dorg.knime.ui.dev.node.view.url=http://localhost:8501/"
 * to map the view to this port.
 */

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public final class StreamlitViewNodeFactory extends NodeFactory<StreamlitViewNodeModel>
    implements NodeViewFactory<StreamlitViewNodeModel> {

    // TODO Use the configured Python executable
    private static final String PYTHON_PATH = "/home/benjamin/apps/miniconda3/envs/knime-streamlit/bin/python";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(StreamlitViewNodeFactory.class);

    @Override
    public StreamlitViewNodeModel createNodeModel() {
        return new StreamlitViewNodeModel();
    }

    @Override
    public NodeView createNodeView(final StreamlitViewNodeModel model) {
        return new StreamlitViewNodeView(model);
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public org.knime.core.node.NodeView<StreamlitViewNodeModel> createNodeView(final int viewIndex,
        final StreamlitViewNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return false;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return null;
    }

    public static interface StreamlitEntryPoint extends PythonEntryPoint {

        void setDataSource(PythonDataSource dataSource);
    }

    static class StreamlitViewNodeView implements NodeView {

        private final StreamlitViewNodeModel m_model;

        public StreamlitViewNodeView(final StreamlitViewNodeModel model) {
            m_model = model;
        }

        @Override
        public Optional<InitialDataService> createInitialDataService() {
            return Optional.empty();
        }

        @Override
        public Optional<DataService> createDataService() {
            return Optional.empty();
        }

        @Override
        public Optional<ApplyDataService> createApplyDataService() {
            return Optional.empty();
        }

        @Override
        public Page getPage() {
            m_model.startServer();
            // TODO look at the NodeViewManager:
            // There is no way to serve a URL other than with the VM Props
            return Page.builder(getClass(), "js-src/", "index.html").build();
        }

        @Override
        public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        }

        @Override
        public void loadValidatedSettingsFrom(final NodeSettingsRO settings) {
        }
    }

    static class StreamlitViewNodeModel extends NodeModel implements BufferedDataTableHolder {

        private PythonGateway<StreamlitEntryPoint> m_gateway;

        private BufferedDataTable m_table;

        protected StreamlitViewNodeModel() {
            super(1, 0);
        }

        private void startServer() {
            if (m_gateway != null) {
                // Server is already running
                return;
            }

            final var launcherPath = PythonSourceDirectoryLocator //
                .getPathFor(StreamlitViewNodeFactory.class) //
                .resolve("streamlit_launcher.py") //
                .toAbsolutePath().toString();
            final var pythonPath = (new PythonPathBuilder()) //
                .add(Python3SourceDirectory.getPath()) //
                .add(Python3ArrowSourceDirectory.getPath()) //
                .build();
            final List<PythonExtension> extensions = Collections.singletonList(PythonArrowExtension.INSTANCE);

            // Start the Python process
            try {
                m_gateway = new PythonGateway<>(new SimplePythonCommand(PYTHON_PATH).createProcessBuilder(),
                    launcherPath, StreamlitEntryPoint.class, extensions, pythonPath);
                LOGGER.warn("Streamlit launcher started.");
            } catch (final IOException ex) {
                // TODO handle
                throw new IllegalStateException(ex);
            }

            // Get the entry point
            final StreamlitEntryPoint entryPoint = m_gateway.getEntryPoint();

            // Send the data
            try {
                final PythonArrowDataSource pythonDataSource =
                    BufferedDataTablePythonArrowUtils.toPythonDataSource(m_table);
                entryPoint.setDataSource(pythonDataSource);
                LOGGER.warn("Data source given to Python: " + pythonDataSource);
            } catch (IOException ex) {
                // TODO handle
                throw new IllegalStateException(ex);
            }
        }

        @Override
        public BufferedDataTable[] getInternalTables() {
            LOGGER.warn("Called getInternalTables");
            return new BufferedDataTable[]{m_table};
        }

        @Override
        public void setInternalTables(final BufferedDataTable[] tables) {
            LOGGER.warn("Called setInternalTables");
            m_table = tables[0];
        }

        @Override
        protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
            return new DataTableSpec[0];
        }

        @Override
        protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
            m_table = inData[0];
            return new BufferedDataTable[0];
        }

        @Override
        protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        }

        @Override
        protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        }

        @Override
        protected void saveSettingsTo(final NodeSettingsWO settings) {
        }

        @Override
        protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        }

        @Override
        protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        }

        @Override
        protected void reset() {
            LOGGER.warn("Called reset");
            if (m_gateway != null) {
                try {
                    m_gateway.close();
                } catch (final IOException ex) {
                    // TODO handle
                    throw new IllegalStateException(ex);
                }
                m_gateway = null;
            }
            m_table = null;
        }
    }
}
