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
 *   Apr 27, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.python3.nodes.extension;

import java.math.BigInteger;

import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDescription41Proxy;
import org.knime.node.v41.FullDescription;
import org.knime.node.v41.KnimeNode;
import org.knime.node.v41.KnimeNodeDocument;
import org.knime.node.v41.Ports;
import org.knime.python3.nodes.extension.ExtensionNode.Option;
import org.knime.python3.nodes.extension.ExtensionNode.Port;
import org.knime.python3.nodes.extension.ExtensionNode.Tab;
import org.knime.python3.nodes.extension.ExtensionNode.View;

/**
 * Creates a {@link NodeDescription} for an {@link ExtensionNode}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ExtensionNodeDescriptionFactory {

    private ExtensionNodeDescriptionFactory() {

    }

    static NodeDescription createDescription(final ExtensionNode node) {
        var doc = KnimeNodeDocument.Factory.newInstance();
        var nodeDoc = doc.addNewKnimeNode();
        nodeDoc.setIcon(node.getIconPath().toAbsolutePath().toString());
        nodeDoc.setType(org.knime.node.v41.NodeType.Enum.forString(node.getType()));
        nodeDoc.setName(node.getName());

        nodeDoc.setShortDescription(node.getShortDescription());

        var fullDescription = nodeDoc.addNewFullDescription();
        fullDescription.addNewIntro().addNewP().newCursor().setTextValue(node.getFullDescription());

        addTabs(node.getTabs(), fullDescription);
        addPorts(node, nodeDoc);
        // TODO what's the difference between views and interactive views?
        addViews(node.getViews(), nodeDoc.addNewViews());

        return new NodeDescription41Proxy(doc);
    }

    private static void addPorts(final ExtensionNode node, final KnimeNode nodeDoc) {
        var ports = nodeDoc.addNewPorts();
        addInPorts(node.getInputPorts(), ports);
        addOutPorts(node.getOutputPorts(), ports);
    }

    private static void addViews(final View[] views, final org.knime.node.v41.Views viewsDoc) {
        for (int i = 0; i < views.length; i++) {//NOSONAR
            var view = views[i];
            var viewDoc = viewsDoc.addNewView();
            viewDoc.setIndex(BigInteger.valueOf(i));
            viewDoc.setName(view.getName());
            viewDoc.newCursor().setTextValue(view.getDescription());
        }
    }

    private static void addInPorts(final Port[] inPorts, final Ports portsDoc) {
        for (int i = 0; i < inPorts.length; i++) {//NOSONAR
            var inPortDoc = portsDoc.addNewInPort();
            var inPort = inPorts[i];
            inPortDoc.setIndex(BigInteger.valueOf(i));
            inPortDoc.setName(inPort.getName());
            inPortDoc.newCursor().setTextValue(inPort.getDescription());
        }
    }

    private static void addOutPorts(final Port[] outPorts, final Ports portsDoc) {
        for (int i = 0; i < outPorts.length; i++) {//NOSONAR
            var outPortDoc = portsDoc.addNewInPort();
            var outPort = outPorts[i];
            outPortDoc.setIndex(BigInteger.valueOf(i));
            outPortDoc.setName(outPort.getName());
            outPortDoc.newCursor().setTextValue(outPort.getDescription());
        }
    }

    private static void addTabs(final Tab[] tabs, final FullDescription fullDescription) {
        for (var tab : tabs) {
            fillTab(tab, fullDescription.addNewTab());
        }
    }

    private static void fillTab(final Tab tab, final org.knime.node.v41.Tab tabDoc) {
        tabDoc.setName(tab.getName());
        // TODO figure out how tab descriptions work
        var options = tab.getOptions();
        for (var option : options) {
            fillOption(option, tabDoc.addNewOption());
        }
    }

    private static void fillOption(final Option option, final org.knime.node.v41.Option optionDoc) {
        optionDoc.setName(option.getName());
        optionDoc.addNewP().newCursor().setTextValue(option.getDescription());
    }


}
