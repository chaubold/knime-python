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
 *   May 6, 2020 (marcel): created
 */
package org.knime.python3.scripting.nodes.prefs;

import java.util.ArrayList;
import java.util.List;

import org.knime.python2.AbstractCondaPythonCommand;
import org.knime.python2.CondaPythonCommand;
import org.knime.python2.PythonCommand;
import org.knime.python2.PythonVersion;

/**
 * Conda-specific implementation of {@link PythonCommand} that works with bundled Python environments. Allows to build
 * Python processes for a given Conda installation and environment name. Takes care of resolving PATH-related issues on
 * Windows.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
public final class BundledPythonCommand extends AbstractCondaPythonCommand {
    /**
     * Constructs a {@link PythonCommand} that describes a Python process that is run in the bundled Conda environment
     * identified by the given Conda installation directory and the given Conda environment name.<br>
     * The validity of the given arguments is not tested.
     *
     * @param environmentDirectoryPath The path to the directory of the bundled Conda environment.
     */
    public BundledPythonCommand(final String environmentDirectoryPath) {
        super(PythonVersion.PYTHON3, environmentDirectoryPath);
    }

    /**
     * For user-provided Conda environments we add the path to the "condabin" dir inside the conda installation
     * directory for the {@link CondaPythonCommand}. But for bundled conda environments, we do not have a conda
     * installation available, so we don't need to add anything
     */
    @Override
    protected List<String> getAdditionalPathPrefixesForWindows() {
        return new ArrayList<String>();
    }
}
