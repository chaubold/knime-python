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
 *   Mar 1, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.python3.nodes.extension;

import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeFactory.NodeType;
import org.w3c.dom.Element;

/**
 * Dummy implementation to be replaced.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ExtensionNodeDescription extends NodeDescription {

    private final ExtensionNode m_node;

    ExtensionNodeDescription(final ExtensionNode node) {
        m_node = node;
    }

    @Override
    public String getIconPath() {
        return m_node.getIconPath().toAbsolutePath().toString();
    }

    @Override
    public String getInportDescription(final int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getInportName(final int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getInteractiveViewName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNodeName() {
        return m_node.getName();
    }

    @Override
    public String getOutportDescription(final int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getOutportName(final int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NodeType getType() {
        return NodeType.valueOf(m_node.getType());
    }

    @Override
    public int getViewCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getViewDescription(final int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getViewName(final int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Element getXMLDescription() {
        // TODO Auto-generated method stub
        return null;
    }

}
