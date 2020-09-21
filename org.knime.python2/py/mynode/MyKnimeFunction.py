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

import inspect
from numbers import Real
from typing import Tuple, get_type_hints


# Draft of how (almost) KNIME agnostic code could look like that is still translatable into a KNIME node.

# TODO: See https://docs.python.org/3/library/typing.html for more type annotations that could be useful (e.g.,
#  Optional).
def my_knime_function(number1: Real, numbers23: Tuple[Real, Real], *,  # Positional arguments = node inputs. Different
                      # positional arguments come from different input tables. Tuples mean columns from the same table.
                      operation: str = ('+', '-', '*', '/'),
                      # Keyword arguments = parameters; set to default value or None if user interaction is mandatory,
                      # default value can be a tuple in which case the tuple entries are interpreted as choices, the
                      # first entry is used as the default value.
                      constant: Real = 42) -> Real:
    number2 = numbers23[0]
    number3 = numbers23[1]
    if operation == '+':
        return number1 + number2 + number3 + constant
    elif operation == '-':
        return number1 - number2 - number3 - constant
    elif operation == '*':
        return number1 * number2 * number3 * constant
    elif operation == '/':
        return number1 / number2 / number3 / constant
    else:
        raise ValueError('Unknown operation: ' + operation)


if __name__ == "__main__":
    # Parse signature of the above function. May be used to synthesize Node on Java side.
    signature = inspect.signature(my_knime_function, follow_wrapped=False)
    print(signature.parameters['numbers23'].annotation.__args__[0])
    print(signature.parameters['constant'].default)
    # type_hints = get_type_hints(my_knime_function)
    # print(type_hints)
