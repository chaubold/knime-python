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
import time

import knime_gateway as kg
import knime_arrow_table as kat
import knime_views as kv

import py4j.clientserver

# TODO Move to knime_gateway?
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


def plot_plotly(df, view_sink):
    import plotly.express as px

    col1 = "Universe_0_0"
    col2 = "Universe_0_1"
    col_label = "Cluster Membership"
    fig = px.scatter(df, x=col1, y=col2, color=col_label, custom_data=[df.index])
    view_sink.display(fig)


def plot_pandas(df, view_sink):
    view_sink.display(df)


def plot_shapely(df, view_sink):
    from shapely.geometry import Polygon

    shape = Polygon(
        [(30, 2.01), (31.91, 0.62), (31.18, -1.63), (28.82, -1.63), (28.09, 0.62)]
    )
    view_sink.display(shape)


def plot_pygal(df, view_sink):
    import pygal

    col1 = "Universe_0_0"
    col2 = "Universe_0_1"
    chart = pygal.XY(stroke=False)
    chart.title = "Scatter"
    chart.add("A", list(zip(df[col1], df[col2])))
    view_sink.display(chart)


def plot_pyvis(df, view_sink):
    from pyvis.network import Network
    import networkx as nx

    nx_graph = nx.cycle_graph(10)
    nx_graph.nodes[1]["title"] = "Number 1"
    nx_graph.nodes[1]["group"] = 1
    nx_graph.nodes[3]["title"] = "I belong to a different group!"
    nx_graph.nodes[3]["group"] = 10
    nx_graph.add_node(20, size=20, title="couple", group=2)
    nx_graph.add_node(21, size=15, title="couple", group=2)
    nx_graph.add_edge(20, 21, weight=5)
    nx_graph.add_node(25, size=25, label="lonely", title="lonely node", group=3)
    nt = Network("500px", "500px")
    nt.from_nx(nx_graph)
    nt.write_html("tmp.html")
    with open("tmp.html", "r") as f:
        html = f.read()
    view_sink.display(kv.view_html(html))


class ViewEntryPoint(kg.EntryPoint):
    def plot(self, java_data_source, java_view_sink):
        view_sink = kg.data_sink_mapper(java_view_sink)
        with kg.data_source_mapper(java_data_source) as data_source:
            df = kat.ArrowReadTable(data_source).to_pandas()

            plot_plotly(df, view_sink)
            # plot_pandas(df, view_sink)
            # plot_shapely(df, view_sink)
            # plot_pygal(df, view_sink)
            # plot_pyvis(df, view_sink)


entry_point = ViewEntryPoint()
kg.connect_to_knime(entry_point)
wait_for_py4j_shutdown()
