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
import sys
import streamlit.cli

import knime_gateway as kg

import streamlit_data_holder


# TODO Run a user supplied streamlit app:
# We could run streamlit on a temporary .py file and change the file by
# inserting user code. The streamlit server will recognize the file change and
# reexcute the app.
# Apart from the user code the file could always include the code to get the
# data (and possibly other stuff we need to prepare for the user).


class StreamlitEntryPoint(kg.EntryPoint):
    def setDataSource(self, java_data_source):
        # NOTE:
        # We set the data source as a global variable that can be used by
        # the streamlit app. See streamlit_app.py for a usage example
        streamlit_data_holder.data = kg.data_source_mapper(java_data_source)


if __name__ == "__main__":
    try:
        # Connect to KNIME
        entry_point = StreamlitEntryPoint()
        kg.connect_to_knime(entry_point)

        # Start the streamlit server
        app_file = os.path.normpath(os.path.join(__file__, "..", "streamlit_app.py"))
        sys.argv = [
            *("streamlit", "run", app_file),
            *("--server.headless", "true"),
            *("--server.port", "8501"),
        ]
        streamlit.cli.main()
    finally:
        if kg.client_server is not None:
            kg.client_server.shutdown()
