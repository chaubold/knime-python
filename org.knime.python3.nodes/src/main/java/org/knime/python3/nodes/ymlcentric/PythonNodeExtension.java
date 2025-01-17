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
 *   Feb 22, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.python3.nodes.ymlcentric;

import java.util.stream.Stream;

import org.knime.python3.nodes.KnimeNodeBackend;
import org.knime.python3.nodes.PyNodeExtension;
import org.knime.python3.nodes.PythonNode;
import org.knime.python3.nodes.proxy.NodeProxy;

/**
 * Represents a Python extension that defines nodes.
 *
 * TODO also allow to define port objects
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class PythonNodeExtension implements PyNodeExtension {

    private String m_author;

    private String m_environmentName;

    private String m_factoryModule;

    private String m_factoryMethod;

    private String m_name;

    // TODO how can we ensure uniqueness? Could this be a combination of m_name and m_author?
    private String m_id;

    private PythonNode[] m_nodes;

    public PythonNodeExtension(final String id, final String name, final String author, final String environmentName,
        final String factoryModule, final String factoryMethod, final PythonNode[] nodes) {
        m_id = id;
        m_name = name;
        m_author = author;
        m_environmentName = environmentName;
        m_factoryModule = factoryModule;
        m_factoryMethod = factoryMethod;
        m_nodes = nodes;
    }

    @Override
    public Stream<PythonNode> getNodeStream() {
        return Stream.of(m_nodes);
    }

    @Override
    public String getName() {
        return m_name;
    }

    @Override
    public String getId() {
        return m_id;
    }

    @Override
    public PythonNode getNode(final String id) {
        return Stream.of(m_nodes)//
            .filter(n -> id.equals(n.getId()))//
            .findFirst()//
            .orElseThrow();
    }

    @Override
    public String getEnvironmentName() {
        return m_environmentName;
    }

    String getFactoryModule() {
        return m_factoryModule;
    }

    String getFactoryMethod() {
        return m_factoryMethod;
    }

    @Override
    public NodeProxy createNodeProxy(final KnimeNodeBackend backend, final String nodeId) {
        return backend.createNodeExtensionProxy(m_factoryModule, m_factoryMethod, nodeId);
    }

}
