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
 *   Apr 22, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.python3.nodes;

import java.io.IOException;
import java.nio.file.Path;

import org.knime.python3.FreshPythonGatewayFactory;
import org.knime.python3.Python3SourceDirectory;
import org.knime.python3.PythonEntryPointUtils;
import org.knime.python3.PythonGateway;
import org.knime.python3.PythonGatewayFactory;
import org.knime.python3.PythonGatewayFactory.PythonGatewayDescription;
import org.knime.python3.arrow.Python3ArrowSourceDirectory;
import org.knime.python3.data.PythonValueFactoryModule;
import org.knime.python3.data.PythonValueFactoryRegistry;

/**
 * Creates {@link PythonGateway PythonGateways} for nodes written purely in Python.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class PythonNodeGatewayFactory {

    private static final Path LAUNCHER = PythonNodesSourceDirectory.getPath()//
        .resolve("knime_node_backend.py");

    private static final PythonGatewayFactory FACTORY = new FreshPythonGatewayFactory();

    private PythonNodeGatewayFactory() {
    }

    /**
     * Creates a {@link PythonGateway} with the {@link KnimeNodeBackend} entry point.
     *
     * @param extensionId id of the extension for which to create the gateway
     * @param pathToExtension location of the extension on disc
     * @param environmentName name of the environment the extension uses
     * @return a {@link PythonGateway} for the provided extension
     * @throws IOException if creation fails due to I/O problems
     * @throws InterruptedException if the creation is interrupted
     */
    public static PythonGateway<KnimeNodeBackend> create(final String extensionId, final Path pathToExtension,
        final String environmentName) throws IOException, InterruptedException {
        var command = PythonNodeCommandFactory.createCommand(extensionId, environmentName);
        var gatewayDescriptionBuilder = PythonGatewayDescription.builder(command, LAUNCHER, KnimeNodeBackend.class)//
                .addToPythonPath(Python3SourceDirectory.getPath())//
                .addToPythonPath(Python3ArrowSourceDirectory.getPath())
            .addToPythonPath(pathToExtension);
        PythonValueFactoryRegistry.getModules().stream().map(PythonValueFactoryModule::getParentDirectory)
            .forEach(gatewayDescriptionBuilder::addToPythonPath);
        var gateway = FACTORY.create(gatewayDescriptionBuilder.build());
        PythonEntryPointUtils.registerPythonValueFactories(gateway.getEntryPoint());
        return gateway;
    }

}
