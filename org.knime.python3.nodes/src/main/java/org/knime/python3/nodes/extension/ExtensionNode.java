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
 *   Feb 28, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.python3.nodes.extension;

import java.nio.file.Path;

/**
 * Represents a node that is provided by a KNIME extension.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public interface ExtensionNode {

    /**
     * @return id identifying the node within the extension
     */
    String getId();

    /**
     * Defines where the node is located in the node repository
     * @return path in the node repository
     */
    String getCategoryPath();

    /**
     * @return id of the node after which this node should be inserted in the node repository
     */
    String getAfterId();

    /**
     * @return the human-readable name of the node
     */
    String getName();

    /**
     * @return path to the node's icon
     */
    Path getIconPath();

    /**
     * @return the type of node e.g. Manipulator or Learner
     */
    String getType();

    String getShortDescription();

    String getFullDescription();

    Port[] getInputPorts();

    Port[] getOutputPorts();

    Tab[] getTabs();

    View[] getViews();

    // TODO remove once Java 17 can be used to turn below classes into records
    public abstract static class Described {
        private final String m_name;

        private final String m_description;

        protected Described(final String name, final String description) {
            m_name = name;
            m_description = description;
        }

        public String getName() {
            return m_name;
        }

        public String getDescription() {
            return m_description;
        }
    }

    public static final class Port {

        private final String m_name;

        private final String m_description;

        public Port(final String name, final String description) {
            m_name = name;
            m_description = description;
        }

        public String getName() {
            return m_name;
        }

        public String getDescription() {
            return m_description;
        }
    }

    public static final class Tab extends Described {
        private final Option[] m_options;

        public Tab(final String name, final String description, final Option... options) {
            super(name, description);
            m_options = options.clone();
        }

        public Option[] getOptions() {
            return m_options.clone();
        }
    }

    public static final class Option extends Described {

        public Option(final String name, final String description) {
            super(name, description);
        }

    }

    public static final class View extends Described {

        public View(final String name, final String description) {
            super(name, description);
        }


    }

}