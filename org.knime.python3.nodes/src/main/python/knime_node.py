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
Provides base implementations and utilities for the development of KNIME nodes in Python.

@author Adrian Nembach, KNIME GmbH, Konstanz, Germany
"""
from abc import ABC, abstractmethod
from dataclasses import dataclass
from numbers import Number
from enum import Enum, auto
from typing import List, Optional, Tuple, Type
import knime_table as kt

# TODO currently not part of our dependencies but maybe worth adding instead of reimplementing here
from packaging.version import Version
from typing import Callable


class PythonNode(ABC):
    """
    Extend this class to provide a pure Python based node extension to KNIME Analytics Platform.
    """

    @abstractmethod
    def configure(self, inSchemas: List[str]) -> List[str]:
        # TODO: We need something akin to PortObjectSpecs in Python.
        #       See https://knime-com.atlassian.net/browse/AP-18368

        # TODO: Make flow variables available during configuration.
        #       See https://knime-com.atlassian.net/browse/AP-18641
        pass

    @abstractmethod
    def execute(
        self, tables: List[kt.ReadTable], objects: List, exec_context
    ) -> Tuple[List[kt.WriteTable], List]:
        # TODO: Allow mixing tables and port objects in inputs and outputs.
        #       See https://knime-com.atlassian.net/browse/AP-18640

        # TODO: Make flow variables available during execution.
        #       See https://knime-com.atlassian.net/browse/AP-18641
        pass

    def get_description(self) -> dict:
        """
        Retrieves the description of this node.
        The returned dict should have a general description of the node, the individual parameters
        as well as inputs and outputs.
        """
        # TODO generate from doc string
        pass


class _PortType(Enum):
    PICKLED = auto()
    TABLE = auto()


@dataclass
class _Port:
    type: _PortType
    name: str
    description: str


@dataclass
class _View:
    name: str
    description: str


class _Node:
    """Class representing an actual node in KNIME AP."""

    node_class: Type
    id: str
    name: str
    node_type: str
    icon_path: str
    category: str
    after: str
    input_ports: List[_Port]
    output_ports: List[_Port]
    views: List[_View]

    def __init__(
        self,
        node_class,
        id: str,
        name: str,
        node_type: str,
        icon_path: str,
        category: str,
        after: str,
    ) -> None:
        self.node_class = node_class
        self.id = id
        self.name = name
        self.node_type = node_type
        self.icon_path = icon_path
        self.category = category
        self.after = after
        self.input_ports = _get_ports(node_class, "input_ports")
        self.output_ports = _get_ports(node_class, "output_ports")
        self.view = _get_view(node_class)

    def to_dict(self):
        return {
            "id": self.id,
            "name": self.name,
            "node_type": self.node_type,
            "icon_path": self.icon_path,
            "category": self.category,
            "after": self.after,
        }


_nodes = {}

# TODO allow to pass in other nodes as after?
# TODO allow to define categories
def node(
    name: str, node_type: str, icon_path: str, category: str, after: str = None
) -> Type:
    """
    Use this decorator to annotate a PythonNode class or function that creates a PythonNode instance that should correspond to a node in KNIME.
    """

    def register(node_class):
        id = f"{category}/{name}"
        _nodes[id] = _Node(
            node_class=node_class,
            id=id,
            name=name,
            node_type=node_type,
            icon_path=icon_path,
            category=category,
            after=after,
        )
        return node_class

    return register


def _initialize_knime_config(node_class):
    if not hasattr(node_class, "__knime_configuration__"):
        node_class.__knime_configuration__ = {}


def _add_port(node_class, port_slot: str, port: _Port):
    _initialize_knime_config(node_class)
    if port_slot not in node_class.__knime_configuration__:
        node_class.__knime_configuration__[port_slot] = []

    # because decorators are called last to first, we insert new ports at the front
    node_class.__knime_configuration__[port_slot].insert(0, port)

    return node_class


def _get_ports(node_class, port_slot) -> List[_Port]:
    if not hasattr(node_class, "__knime_configuration__"):
        return []

    return node_class.__knime_configuration__[port_slot]


def _get_view(node_class, port_slot) -> Optional[_View]:
    if not hasattr(node_class, "__knime_configuration__"):
        return None

    return node_class.__knime_configuration__["view"]


def input_port(name: str, description: str):
    """
    Use this decorator to define a (Pickled) port object input port of a node.
    """
    return lambda node_class: _add_port(
        node_class, "input_ports", _Port(_PortType.PICKLED, name, description)
    )


def input_table(name: str, description: str):
    """
    Use this decorator to define an input port of type "Table" of a node.
    """
    return lambda node_class: _add_port(
        node_class, "input_ports", _Port(_PortType.TABLE, name, description)
    )


def output_port(name: str, description: str):
    """
    Use this decorator to define a (Pickled) port object output port of a node.
    """
    return lambda node_class: _add_port(
        node_class, "output_ports", _Port(_PortType.PICKLED, name, description)
    )


def output_table(name: str, description: str):
    """
    Use this decorator to define an output port of type "Table" of a node.
    """
    return lambda node_class: _add_port(
        node_class, "output_ports", _Port(_PortType.TABLE, name, description)
    )


def view(name: str, description: str):
    """
    Use this decorator to specify that this node has a view
    """

    def add_view(node_class):
        _initialize_knime_config(node_class)
        node_class.__knime_configuration__["view"] = _View(name, description)
        return node_class

    return add_view


# ---------------------------------------------------------------------------------------------------------
class Parameter:
    """
    Decorator class that essentially is an extension of property that includes the type as well as other information useful
    for e.g. validation.
    """

    def __init__(
        self,
        fget,
        fset=None,
        _type: str = None,
        since_version: Version = None,
        doc=None,
    ) -> None:
        if fget is None:
            raise ValueError("fget must always be defined for a Parameter")
        self.fget = fget
        self.fset = fset
        self._type = _type
        self.since_version = since_version
        if doc is None and fget is not None:
            doc = fget.__doc__
        self.__doc__ = doc
        self._name = ""

    def __set_name__(self, owner, name):
        self._name = name

    def __get__(self, obj, objtype=None):
        if obj is None:
            return self
        return self.fget(obj)

    def __set__(self, obj, value):
        if self.fset is None:
            raise NotImplementedError("Every parameter must have a setter.")
        self.fset(obj, value)

    def validate(self, obj, value):
        current_val = self.fget(obj)
        try:
            self.__set__(obj, value)
        finally:
            self.__set__(obj, current_val)

    def setter(self, fset):
        param = type(self)(
            self.fget, fset, self._type, self.since_version, self.__doc__
        )
        param._name = self._name
        return param

    def get_parameter(self):
        return self

    def exists_in_version(self, version):
        return self.since_version is None or self.since_version <= version


def parameter(
    _func=None,
    *,
    fset=None,
    _type: str = None,
    since_version: Version = None,
    doc: str = None,
):
    def decorator_parameter(fget):
        return Parameter(fget, fset, _type, since_version, doc)

    if _func is None:
        return decorator_parameter
    else:
        return decorator_parameter(_func)


class BaseDescriptor:
    def __init__(self, descriptor) -> None:
        self.descriptor = descriptor

    def __get__(self, obj, objtype=None):
        if obj is None:
            return self
        return self.descriptor.__get__(obj, objtype)

    def __set__(self, obj, value):
        self.descriptor.__set__(obj, value)

    def validate(self, obj, value):
        self.descriptor.validate(obj, value)

    def get_parameter(self):
        return self.descriptor.get_parameter()

    # TODO figure out if we can also implement setter here


# TODO maybe this could be implemented as class decorator
class UI(BaseDescriptor):
    """
    Decorator class defining the UI for a parameter.
    """

    def __init__(
        self, parameter: Parameter = None, label: str = None, options: dict = None
    ) -> None:
        super().__init__(parameter)
        self._label = label
        self._options = options

    def setter(self, fset):
        param = self.descriptor.setter(fset)
        return UI(param, self._label, self._options)


def ui(label: str = None, options: dict = None):
    return lambda parameter: UI(parameter, label, options)


class Rule(BaseDescriptor):
    """
    Decorator class defining a rule for a parameter.
    """

    def __init__(self, descriptor, effect: str, scope: Parameter, schema: str) -> None:
        super().__init__(descriptor)
        self.effect = effect
        self.scope = scope
        self.schema = schema

    def setter(self, fset):
        descriptor = self.descriptor.setter(fset)
        return Rule(descriptor, self.effect, self.scope, self.schema)


def rule(effect: str, scope: Parameter, schema: dict):
    return lambda descriptor: Rule(descriptor, effect, scope, schema)
