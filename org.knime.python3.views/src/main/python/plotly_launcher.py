# -*- coding: utf-8 -*-
# ------------------------------------------------------------------------
#  Copyright by KNIME AG, Zurich, Switzerland
#  Website: http://www.knime.com; Email: contact@knime.com
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License, Version 3, as
#  published by the Free Software Foundation.
#
#  This program is distributed in the hope that it will be useful, but
#  WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program; if not, see <http://www.gnu.org/licenses>.
#
#  Additional permission under GNU GPL version 3 section 7:
#
#  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
#  Hence, KNIME and ECLIPSE are both independent programs and are not
#  derived from each other. Should, however, the interpretation of the
#  GNU GPL Version 3 ("License") under any applicable laws result in
#  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
#  you the additional permission to use and propagate KNIME together with
#  ECLIPSE with only the license terms in place for ECLIPSE applying to
#  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
#  license terms of ECLIPSE themselves allow for the respective use and
#  propagation of ECLIPSE together with KNIME.
#
#  Additional permission relating to nodes for KNIME that extend the Node
#  Extension (and in particular that are based on subclasses of NodeModel,
#  NodeDialog, and NodeView) and that only interoperate with KNIME through
#  standard APIs ("Nodes"):
#  Nodes are deemed to be separate and independent programs and to not be
#  covered works.  Notwithstanding anything to the contrary in the
#  License, the License does not apply to Nodes, you are not required to
#  license Nodes under the License, and you are granted a license to
#  prepare and propagate Nodes, in each case even if such Nodes are
#  propagated with or for interoperation with KNIME.  The owner of a Node
#  may freely choose the license terms applicable to such Node, including
#  when such Node is propagated with or for interoperation with KNIME.
# ------------------------------------------------------------------------
"""
@author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
"""
import os
import time

import knime_gateway as kg
import knime_arrow_table as kat

import plotly.express as px
import py4j.clientserver


KNIME_HIGHLIGHTING_JS = """
function selectionAdd(indices) {
    //...
    // JS code to send the event to KNIME
}
function selectionRemove (indices) {
}
function selectionReplace (indices) {
}

function registerSelectionListener(listener) {
    //...
    // JS code to register the listener/callback
}
"""

# TODO plotly_post_scritp - Get the RowId from the selected points:
# The RowId is needed to notify KNIME which rows have been selected. Currently
# it seems like the RowId is not available in the `data.points` object.

# TODO plotly_post_scritp - Fire an event to KNIME unsing the knimeService:
# Currently the knimeService is not available in the iFrame. This needs to be
# fixed in the js-pagebuilder. (Ask JS team)

# TODO plotly_post_scritp - Register a callback for selections from outside:
# We need to use the knimeService to register what happens when rows were
# selected in another node. In this case we need to update the selectedpoints
# in this plot.
# See https://plotly.com/javascript/reference/scatter/#scatter-selectedpoints

# See https://plotly.com/javascript/plotlyjs-events/
# and https://plotly.com/python-api-reference/generated/plotly.io.to_html.html
PLOTLY_POST_JS = (
    KNIME_HIGHLIGHTING_JS
    + """
d = document.getElementById('{plot_id}');
d.on('plotly_selected', function(data) {

    // TODO get the RowId out of the data points
    console.log(data.points);

    // TODO fire a selection event to KNIME
    // window.knimeSelectionService['add'](['Row0']);
});

registerSelectionListener(function (indices) {
    // TODO this or something that really works with plotly
    d.select(indices)
});
"""
)


# TODO move to knime_gateway?
def wait_for_py4j_shutdown():
    stopped = False

    def stop(*args, **kwargs):
        global stopped
        stopped = True

    py4j.clientserver.server_connection_stopped.connect(stop)

    while not stopped:
        time.sleep(1)

    if kg.client_server is not None:
        kg.client_server.shutdown()


class PlotlyEntryPoint(kg.EntryPoint):
    def plot(self, java_data_source, output_path):
        # TODO Run user supplied plotly code.
        with kg.data_source_mapper(java_data_source) as data_source:
            table = kat.ArrowReadTable(data_source)
            df = table.to_pandas()
            fig = px.scatter(df, x="Universe_0_0", y="Universe_0_1")
            fig.write_html(
                os.path.join(output_path, "index.html"),
                post_script=PLOTLY_POST_JS,
            )


entry_point = PlotlyEntryPoint()
kg.connect_to_knime(entry_point)
wait_for_py4j_shutdown()
