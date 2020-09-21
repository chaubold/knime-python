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
@author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
"""

import numpy as np
from pandas import DataFrame


# Draft of how a (slightly) simplified Noding API could look like in Python.

class MyEasyKnimeNode():

    def __init__(self):
        self._column1: str = column(0)
        self._column2: str = column(1)
        self._operation: str = parameter(str)
        self._constant: int = parameter(int)

    def configure(self, spec1: dict, spec2: dict) -> dict:
        spec1_type = spec1[self._column1]['type']
        spec2_type = spec2[self._column2]['type']
        if not np.issubdtype(spec1_type, np.number) or not np.issubdtype(spec2_type, np.number):
            # TODO: Translate to InvalidSettingsException
            raise TypeError()
        if np.issubdtype(spec1_type, np.integer) and np.issubdtype(spec2_type, np.integer):
            out_type = np.integer
        else:
            out_type = np.float64
        return {'name': "Calculated value", 'type': out_type}

    def execute(self, table1: DataFrame, table2: DataFrame) -> DataFrame:
        number1 = table1[self._column1]
        number2 = table1[self._column2]

        if self._operation == '+':
            return number1 + number2 / self._constant
        elif self._operation == '-':
            return number1 - number2 / self._constant
        elif self._operation == '*':
            return number1 * number2 / self._constant
        elif self._operation == '/':
            return number1 / number2 / self._constant
        else:
            raise ValueError('Unknown operation: ' + self._operation)
