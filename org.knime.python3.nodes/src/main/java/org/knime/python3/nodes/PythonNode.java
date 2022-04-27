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
package org.knime.python3.nodes;

/**
 * Represents a PythonNode.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class PythonNode { // TODO record in Java 17

    private final String m_id;

    private final String m_categoryPath;

    private final String m_afterId;

    private final String m_iconPath;

    private final String m_name;

    private final String m_type;

    private final String m_shortDescription;

    private final String m_fullDescription;

    /**
     * Constructor.
     *
     * @param id of the node
     * @param categoryPath path to the category the node is contained in in the node repository
     * @param afterId id of the node after which to insert this node
     * @param iconPath to the icon of this node
     * @param name human-readable name of the node
     * @param type of the node e.g. Manipulator
     * @param shortDescription a short (one-line) description of what the node does
     * @param fullDescription an in-depth description of what the node does
     */
    public PythonNode(final String id, final String categoryPath, final String afterId, final String iconPath,
        final String name, final String type, final String shortDescription, final String fullDescription) {
        m_id = id;
        m_categoryPath = categoryPath;
        m_afterId = afterId;
        m_iconPath = iconPath;
        m_name = name;
        m_type = type;
        m_shortDescription = shortDescription;
        m_fullDescription = fullDescription;
    }

    /**
     * @return id of the node
     */
    public String getId() {
        return m_id;
    }

    /**
     * @return category path
     */
    public String getCategoryPath() {
        return m_categoryPath;
    }

    /**
     * @return id of the node after which to insert this node
     */
    public String getAfterId() {
        return m_afterId;
    }

    /**
     * @return path to this node's icon
     */
    public String getIconPath() {
        return m_iconPath;
    }

    /**
     * @return human-readable name of this node
     */
    public String getName() {
        return m_name;
    }

    /**
     * @return type of this node
     */
    public String getType() {
        return m_type;
    }

    public String getShortDescription() {
        return m_shortDescription;
    }

    public String getFullDescription() {
        return m_fullDescription;
    }
}