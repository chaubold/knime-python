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
 */
package org.knime.python3.condaenv.bin;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Activator class for the TensorFlow libraries.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public class CondaEnvPluginActivator extends AbstractUIPlugin {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(CondaEnvPluginActivator.class);
	
	private static final String CONDA_EXECUTABLE_NAME = "micromamba";

	/** The plug-in ID */
	public static final String PLUGIN_ID = "org.knime.python3.condaenv.bin"; //$NON-NLS-1$

	// The shared instance
	private static CondaEnvPluginActivator plugin;

	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		String osName = System.getProperty("os.name");
		String osVersion = System.getProperty("os.version");
		if(osName.toLowerCase().contains("mac")) {
			if(osVersion.toLowerCase().contains("10.11")) {
				Exception ex = new IllegalStateException("No bundled Python Conda Environment available for"
						+ "macOs 10.11 (El Capitan). Please use "
						+ "macOS 10.12.6 (Sierra) or later.");
				LOGGER.error(ex.getMessage());
				throw ex;
			}
		}

		final boolean forceCPU = true;

		// Get the path to the bundled conda channel
		final Bundle[] fragments = Platform.getFragments(getBundle());
		String condaExecutablePath = null;
		String condaChannelPath = null;
		for (Bundle fragment : fragments) {
			condaExecutablePath = getCondaExecutablePath(fragment);
			condaChannelPath = getCondaChannelPath(fragment);
		}
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * @return the shared instance
	 */
	public static CondaEnvPluginActivator getDefault() {
		return plugin;
	}

	private String getCondaExecutablePath(final Bundle fragmentBundle) throws IOException {
		final var binUrl = FileLocator.find(fragmentBundle, new Path("bin/"), null);
		final File binFile = FileUtil.getFileFromURL(FileLocator.toFileURL(binUrl));
		final File condaExecutable = binFile.listFiles((f, s) -> s.contains(CONDA_EXECUTABLE_NAME))[0];
		return condaExecutable.getAbsolutePath();
	}
	
	private String getCondaChannelPath(final Bundle fragmentBundle) throws IOException {
		final var pkgsUrl = FileLocator.find(fragmentBundle, new Path("pkgs/"), null);
		final File pkgsFile = FileUtil.getFileFromURL(FileLocator.toFileURL(pkgsUrl));
		return pkgsFile.getAbsolutePath();
	}
}
