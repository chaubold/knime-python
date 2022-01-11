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
package org.knime.python3.views.node.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.PathUtils;
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
import org.knime.python3.arrow.PythonArrowExtension;
import org.knime.python3.views.PythonNodeViewSink;
import org.knime.python3.views.PythonViewsExtension;
import org.knime.python3.views.node.BufferedDataTablePythonArrowUtils;
import org.knime.python3.views.node.example.PythonViewNodeFactory.PythonViewNodeModel;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public final class PythonViewNodeFactory extends NodeFactory<PythonViewNodeModel>
    implements NodeViewFactory<PythonViewNodeModel> {

    // TODO Use the configured Python executable
    private static final String PYTHON_PATH = "/home/benjamin/apps/miniconda3/envs/knime-plotly/bin/python";

    @Override
    public PythonViewNodeModel createNodeModel() {
        return new PythonViewNodeModel();
    }

    @Override
    public NodeView createNodeView(final PythonViewNodeModel model) {
        return new PythonViewNodeView(model);
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public org.knime.core.node.NodeView<PythonViewNodeModel> createNodeView(final int viewIndex,
        final PythonViewNodeModel nodeModel) {
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

    static class PythonViewNodeView implements NodeView {

        private final PythonViewNodeModel m_model;

        public PythonViewNodeView(final PythonViewNodeModel model) {
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
            final var directory = m_model.m_outputFilePath.getParent().toAbsolutePath().toString();
            final var fileName = m_model.m_outputFilePath.getFileName().toString();
            return Page.builder(getClass(), directory, fileName).build();
        }

        @Override
        public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        }

        @Override
        public void loadValidatedSettingsFrom(final NodeSettingsRO settings) {
        }
    }

    public interface PythonViewEntryPoint extends PythonEntryPoint {
        void plot(PythonDataSource dataSource, PythonNodeViewSink viewSink);
    }

    static class PythonViewNodeModel extends NodeModel {

        private Path m_outputFilePath;

        protected PythonViewNodeModel() {
            super(1, 0);
        }

        @Override
        protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
            return new DataTableSpec[0];
        }

        @Override
        protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {

            final var launcherPath = PythonSourceDirectoryLocator //
                .getPathFor(PythonViewNodeFactory.class) //
                .resolve("example_view_launcher.py") //
                .toAbsolutePath().toString();
            final var pythonPath = (new PythonPathBuilder()) //
                .add(Python3SourceDirectory.getPath()) //
                .add(Python3ArrowSourceDirectory.getPath()) //
                .build();
            final List<PythonExtension> extensions = Arrays.asList(PythonArrowExtension.INSTANCE, PythonViewsExtension.INSTANCE);

            m_outputFilePath = PathUtils.createTempDir("python_view").resolve("index.html");

            try (final PythonGateway<PythonViewEntryPoint> gateway =
                new PythonGateway<>(new SimplePythonCommand(PYTHON_PATH).createProcessBuilder(), launcherPath,
                    PythonViewEntryPoint.class, extensions, pythonPath)) {

                // Get the entry point
                final PythonViewEntryPoint entryPoint = gateway.getEntryPoint();

                // Send the data
                final var dataSource =
                    BufferedDataTablePythonArrowUtils.toPythonDataSource(inData[0]);
                final var nodeViewSink = new PythonNodeViewSink(m_outputFilePath.toAbsolutePath().toString());
                entryPoint.plot(dataSource, nodeViewSink);
            }
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
            if (m_outputFilePath != null) {
                try {
                    PathUtils.deleteDirectoryIfExists(m_outputFilePath);
                } catch (final IOException ex) {
                    // TODO handle
                }
            }
        }
    }
}
