/*
 * ------------------------------------------------------------------------
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
 * ----------------------------------------------------------------------------
 */
package org.knime.python3.condaenv.p2.actions;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.knime.python3.CondaChannel;
import org.knime.python3.CondaExecutable;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Custom p2 action that can be used by plugins in order to execute arbitrary commands during their installation.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @since 4.6
 */
public class CreateCondaEnv extends ProvisioningAction {
    private static final Bundle bundle = FrameworkUtil.getBundle(CreateCondaEnv.class);

    private final static ILog logger = Platform.getLog(bundle);

    @Override
    public IStatus execute(final Map<String, Object> parameters) {
        String os = null; // Operating System the Command is for. Null means
                          // all.

        if (parameters.containsKey("os")) {
            os = (String)parameters.get("os");
        }

        if (verifyOS(os)) {
            String envPath = null;
            StringBuilder channelPaths = new StringBuilder();
            String packages = "";

            if (parameters.containsKey("envPath")) {
                envPath = (String)parameters.get("envPath");
                logger.log(new Status(IStatus.INFO, bundle.getSymbolicName(), "CreateCondaEnv envPath: " + envPath));
            } else {
            	logger.log(new Status(IStatus.ERROR, bundle.getSymbolicName(), "No Conda environment path specified"));
            	return Status.CANCEL_STATUS;
            }

            if (parameters.containsKey("channels")) {
                String[] channels = ((String)parameters.get("channels")).split("#");
                logger.log(new Status(IStatus.INFO, bundle.getSymbolicName(), "CreateCondaEnv channels: " + channels));
                
                for (var c : channels) {
                	try {
	                	final var channelPath = getCondaChannelPath(c);
	                	logger.log(new Status(IStatus.INFO, bundle.getSymbolicName(), "\tChannel: " + c + " at " + channelPath));
	                	channelPaths.append("-c " + channelPath + " ");
                	} catch(IllegalStateException e) {
                		logger.log(new Status(IStatus.ERROR, bundle.getSymbolicName(), "\tChannel: " + c + " not found!"));
                	}
                }
            }

            if (parameters.containsKey("packages")) {
            	String[] ps = ((String)parameters.get("packages")).split("#");
            	for (var p : ps) {
            		packages = packages + " " + p;
            	}
                logger.log(new Status(IStatus.INFO, bundle.getSymbolicName(), "CreateCondaEnv packages: " + packages));
	        } else {
	        	logger.log(new Status(IStatus.ERROR, bundle.getSymbolicName(), "No Conda packages specified"));
	        	return Status.CANCEL_STATUS;
	        }
            
            final var condaExePath = getCondaExePath();
            logger.log(new Status(IStatus.INFO, bundle.getSymbolicName(), "CreateCondaEnv command: " + condaExePath));
            File condaExeFile = condaExePath.toFile();
            if (!condaExeFile.canExecute()) {
            	logger.log(new Status(IStatus.INFO, bundle.getSymbolicName(), "CondaExe was not executable, fixing that...."));
            	condaExeFile.setExecutable(true);
            }
            
            String command = condaExePath + " create -p " + envPath + " -y --override-channels " +  channelPaths.toString() + " " + packages;
            
            logger.log(new Status(IStatus.INFO, bundle.getSymbolicName(), "Running post-install: " + command));
            try {
                Process p = Runtime.getRuntime().exec(command, null, null);
                int exitVal = p.waitFor();
                if (exitVal != 0) {
                    logger.log(new Status(IStatus.ERROR, bundle
                            .getSymbolicName(),
                            "ShellExec command exited non-zero exit value"));
                    return Status.CANCEL_STATUS;
                }
            } catch (Exception e) {
                logger.log(new Status(IStatus.ERROR, bundle.getSymbolicName(),
                        "Exception occured", e));
                return Status.CANCEL_STATUS;
            }
        }
        return Status.OK_STATUS;
    }

    private boolean verifyOS(final String os) {
        return (os == null) || Platform.getOS().equals(os);
    }

    private static Path getCondaExePath() throws IllegalStateException {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint("org.knime.python3.BundledCondaExecutable");
        
        if (point == null) {
        	throw new IllegalStateException("Could not find required extension point 'BundledCondaExecutable'");
        }
        
        if (point.getConfigurationElements().length != 1) {
            throw new IllegalStateException("No bundled conda executable available");
        }

        final var ext = point.getConfigurationElements()[0];
        CondaExecutable condaExe = null;
        try {
        	condaExe = (CondaExecutable)ext.createExecutableExtension("executable");
        } catch (CoreException e) {
        	throw new IllegalStateException("Found extension point with conda executable, but could not instantiate it", e);
        }
        
        if (condaExe == null) {
            throw new IllegalStateException(
                "Found extension point with conda executable, but it did not contain an executable path");
        }

        final var condaPath = condaExe.getPath();
        if (!Files.exists(condaPath)) {
            throw new IllegalStateException(
                "Found extension point with conda executable, but there is no file at the specified path " + condaExe);
        }

        return condaPath;
    }

    private static Path getCondaChannelPath(final String condaChannelName) throws IllegalStateException {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint("org.knime.python3.BundledCondaChannel");
        
        if (point == null) {
        	throw new IllegalStateException("Could not find required extension point 'BundledCondaChannel'");
        }

        CondaChannel channel = null;
        
        for (var ext: point.getConfigurationElements()) {
            try {
            	CondaChannel c = (CondaChannel)ext.createExecutableExtension("channel");
            	if (c.getName().equals(condaChannelName))
            	{
            		channel = c;
            	}
            } catch (CoreException e) {
            	throw new IllegalStateException("Ignoring extension because it could not be instantiated", e);
            }
        }
        
        if (channel == null) {
            throw new IllegalStateException("Did not find extension point for conda channel " + condaChannelName);
        }

        final var condaChannelPath = channel.getPath();
        if (!Files.exists(condaChannelPath)) {
            throw new IllegalStateException("Found extension point for conda channel " + condaChannelName
                + ", but it did not contain a valid path " + condaChannelPath);
        }

        return condaChannelPath;
    }

    @Override
    public IStatus undo(final Map<String, Object> parameters) {
    	// TODO: implement me? delete conda env!
        return Status.OK_STATUS;
    }
}
