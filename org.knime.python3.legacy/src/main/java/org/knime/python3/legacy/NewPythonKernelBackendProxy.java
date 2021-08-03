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

import java.util.List;
import java.util.Map;

import org.knime.python2.generic.ImageContainer;
import org.knime.python2.kernel.PythonIOException;
import org.knime.python2.port.PickledObject;
import org.knime.python3.PythonDataSink;
import org.knime.python3.PythonDataSource;
import org.knime.python3.PythonEntryPoint;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public interface NewPythonKernelBackendProxy extends PythonEntryPoint {

    // TODO: generalize serialization of flow variables. Treat them similarly to ordinary data.

    void putFlowVariables(String name, Map<String, Object> flowVariables);

    Map<String, Object> getFlowVariables(String name);

    // --

    void putTableIntoWorkspace(String name, PythonDataSource dataSource, int rowLimit);

    void putTableIntoWorkspace(String name, PythonDataSource dataSource);

    void getTableFromWorkspace(String name, PythonDataSink dataSink);

    // TODO: how to handle objects and images?
    // - objects should directly be read from file store; take care of byte order. Reuse Arrow functionality for
    //   reading/writing?

    void putObject(String name, PickledObject object);

    PickledObject getObject(String name);

    ImageContainer getImage(String name);

    // --

    List<Map<String, String>> listVariables();

    List<Map<String, String>> autoComplete(String sourceCode, int line, int column) throws PythonIOException;

    List<String> executeOnMainThread(String sourceCode);

    List<String> executeOnCurrentThread(String sourceCode);
}
