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

from typing import List, Tuple
import unittest
import pythonpath
import knime_node as kn
import knime_table as kt
import knime_schema as ks


@kn.node(name="My Test Node", node_type="Learner", icon_path="icon.png", category="/")
@kn.input_table(name="Input Data", description="We read data from here")
@kn.input_table(
    name="Second input table", description="We might also read data from there"
)
@kn.output_table(name="Output Data", description="Whatever the node has produced")
@kn.output_port(name="Some output port", description="Maybe a model")
class MyTestNode(kn.PythonNode):
    def __init__(self) -> None:
        super().__init__()
        self._some_param = 42

    @kn.Parameter
    def some_param(self):
        return self._some_param

    @some_param.setter
    def some_param(self, value):
        self._some_param = value

    def configure(self, input_schemas: List[ks.Schema]) -> List[ks.Schema]:
        return input_schemas

    def execute(
        self, tables: List[kt.ReadTable], objects: List, exec_context
    ) -> Tuple[List[kt.WriteTable], List]:
        return [kt.write_table(table) for table in tables], []


class NodeApiTest(unittest.TestCase):
    node_id = "//My Test Node"

    def setUp(self):
        self.node = kn._nodes.get(NodeApiTest.node_id, None)

    def test_node_registration(self):
        self.assertTrue(NodeApiTest.node_id in kn._nodes)

    def test_no_node_view(self):
        self.assertIsNone(self.node.view)

    def test_input_ports(self):
        self.assertEqual(2, len(self.node.input_ports))
        self.assertEqual(kn._PortType.TABLE, self.node.input_ports[0].type)
        self.assertEqual(kn._PortType.TABLE, self.node.input_ports[1].type)

    def test_output_ports(self):
        self.assertEqual(2, len(self.node.output_ports))
        self.assertEqual(kn._PortType.TABLE, self.node.output_ports[0].type)
        self.assertEqual(kn._PortType.PICKLED, self.node.output_ports[1].type)


if __name__ == "__main__":
    unittest.main()
