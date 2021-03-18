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

# Fully functional PoC that demonstrates how the raw KNIME Noding API can be used from within Python via py4j.
# TODO: In the long term, we will want to remove all the py4j-specific (and/or tedious Java-specific) stuff from user
#  code and introduce pythonic intermediate API layers.

import math
import os
import sys
import threading

import numpy as np
import pyarrow as pa
import pyarrow.ipc as pi
import pyarrow.compute as pc

from pandas import DataFrame
from py4j.java_gateway import get_java_class
from py4j.java_gateway import java_import

# Import KNIME classes:
from knime.gateway import client_server as cs
from knime.gateway import get_next_table_id
from knime.gateway import workspace

knime = cs.new_jvm_view()
java_import(knime, 'java.io.File')
java_import(knime, 'org.knime.core.data.DataCell')
java_import(knime, 'org.knime.core.data.DataColumnSpec')
java_import(knime, 'org.knime.core.data.DataColumnSpecCreator')
java_import(knime, 'org.knime.core.data.DataTableSpec')
java_import(knime, 'org.knime.core.data.DoubleValue')
java_import(knime, 'org.knime.core.data.IntValue')
java_import(knime, 'org.knime.core.data.container.filter.TableFilter')
java_import(knime, 'org.knime.core.data.def.DefaultRow')
java_import(knime, 'org.knime.core.data.def.DoubleCell')
java_import(knime, 'org.knime.core.data.def.IntCell')
java_import(knime, 'org.knime.core.data.columnar.table.ColumnarRowContainer')
java_import(knime, 'org.knime.core.node.BufferedDataTable')
java_import(knime, 'org.knime.core.node.ExecutionContext')
java_import(knime, 'org.knime.core.node.ExecutionMonitor')
java_import(knime, 'org.knime.core.node.NodeLogger')
java_import(knime, 'org.knime.core.node.NodeSettingsRO')
java_import(knime, 'org.knime.core.node.NodeSettingsWO')
java_import(knime, 'org.knime.core.node.defaultnodesettings.SettingsModelInteger')
java_import(knime, 'org.knime.core.node.defaultnodesettings.SettingsModelString')
java_import(knime, 'org.knime.core.node.port.PortObject')
java_import(knime, 'org.knime.core.node.port.PortObjectSpec')
java_import(knime, 'org.knime.core.node.port.PortType')

java_import(knime, 'org.knime.python2.mynode.ArrowIPCBatchConsumer')

_logger = knime.NodeLogger.getLogger("MyStandardKnimeNodeModel")

_operators = cs.new_array(knime.java.lang.String, 4)
_operators[0] = '+'
_operators[1] = '-'
_operators[2] = '*'
_operators[3] = '/'


def _create_operand1_settings() -> knime.SettingsModelString:
    return knime.SettingsModelString("first_operand", "First operand")


def _create_operand2_settings() -> knime.SettingsModelString:
    return knime.SettingsModelString("second_operand", "Second operand")


def _create_operator_settings() -> knime.SettingsModelString:
    return knime.SettingsModelString("operator", _operators[0])


def _create_constant_settings() -> knime.SettingsModelInteger:
    return knime.SettingsModelInteger("constant", 42)


class MyStandardKnimeNodeModel():

    def __init__(self):
        self._operand1_settings = _create_operand1_settings()
        self._operand2_settings = _create_operand2_settings()
        self._operator_settings = _create_operator_settings()
        self._constant_settings = _create_constant_settings()

    def getInputPortTypes(self):
        types = cs.new_array(knime.PortType, 2)
        types[0] = knime.BufferedDataTable.TYPE
        types[1] = knime.BufferedDataTable.TYPE
        return types

    def getOutputPortTypes(self):
        types = cs.new_array(knime.PortType, 1)
        types[0] = knime.BufferedDataTable.TYPE
        return types

    def loadInternals(self, nodeInternDir: knime.File, exec: knime.ExecutionMonitor):
        # Nothing to do.
        pass

    def saveInternals(self, nodeInternDir: knime.File, exec: knime.ExecutionMonitor):
        # Nothing to do.
        pass

    def saveSettingsTo(self, settings: knime.NodeSettingsWO):
        self._operand1_settings.saveSettingsTo(settings)
        self._operand2_settings.saveSettingsTo(settings)
        self._operator_settings.saveSettingsTo(settings)
        self._constant_settings.saveSettingsTo(settings)

    def validateSettings(self, settings: knime.NodeSettingsRO):
        self._operand1_settings.validateSettings(settings)
        self._operand2_settings.validateSettings(settings)
        self._operator_settings.validateSettings(settings)
        self._constant_settings.validateSettings(settings)

    def loadValidatedSettingsFrom(self, settings: knime.NodeSettingsRO):
        self._operand1_settings.loadSettingsFrom(settings)
        self._operand2_settings.loadSettingsFrom(settings)
        self._operator_settings.loadSettingsFrom(settings)
        self._constant_settings.loadSettingsFrom(settings)

    def configure(self, inSpecs):
        _logger.debug("Configuring node on thread " + str(threading.get_ident()))

        column_spec1 = inSpecs[0].getColumnSpec(self._operand1_settings.getStringValue())
        column_spec2 = inSpecs[1].getColumnSpec(self._operand2_settings.getStringValue())

        if column_spec1 is None or column_spec2 is None:
            raise ValueError("Auto-configuration is not yet implemented. Please configure the node via the dialog.")

        result_table_spec = self._configure(column_spec1, column_spec2)
        outSpecs = cs.new_array(knime.PortObjectSpec, 1)
        outSpecs[0] = result_table_spec
        return outSpecs

    def _configure(self, column_spec1, column_spec2):
        column1_type = column_spec1.getType()
        column2_type = column_spec2.getType()

        int_value = get_java_class(knime.IntValue)
        double_value = get_java_class(knime.DoubleValue)

        if column1_type.isCompatible(int_value) and column2_type.isCompatible(int_value):
            if self._operator_settings.getStringValue() != "/":
                result_type = knime.IntCell.TYPE
            else:
                result_type = knime.DoubleCell.TYPE
        elif column1_type.isCompatible(double_value) and column2_type.isCompatible(double_value):
            result_type = knime.DoubleCell.TYPE
        else:
            # TODO: Translate to InvalidSettingsException?
            raise TypeError(
                "Operands must both be either of type 'Number (integer)' or of type 'Number (double)'.")

        result_columns = cs.new_array(knime.DataColumnSpec, 1)
        result_columns[0] = knime.DataColumnSpecCreator("Result", result_type).createSpec()
        return knime.DataTableSpec(result_columns)

    def execute(self, inObjects, exec_context: knime.ExecutionContext):
        _logger.debug(
            "Executing node using executable: " + str(sys.executable) + ", on thread: " + str(threading.get_ident()))

        for i, in_object in enumerate(inObjects):
            _logger.debug("Object at input port " + str(i) + ":" + str(in_object))

        # TODO: handle multiple input tables pointing to the same file; in general: virtual tables, etc.
        # TODO: handle compression. Seems to only be available when using streams? See pyarrow.open_stream(..., compression, ...)
        # TODO: handle different types of row keys
        # TODO: domain computation, duplicate key checks, etc.

        table1_batch_supplier = inObjects[0]
        table1_spec = table1_batch_supplier.getTableSpec()
        table1_schema = pi.read_schema(table1_batch_supplier.getSchemaFile())

        table2_batch_supplier = inObjects[1]
        table2_spec = table2_batch_supplier.getTableSpec()

        operand1_name = self._operand1_settings.getStringValue()
        operand2_name = self._operand2_settings.getStringValue()

        operand1_arrow_name = str(table1_spec.findColumnIndex(operand1_name) + 1)  # + 1 because of row key
        operand2_arrow_name = str(table2_spec.findColumnIndex(operand2_name) + 1)  # + 1 because of row key

        operator = self._operator_settings.getStringValue()
        constant = pa.scalar(self._constant_settings.getIntValue(), type=pa.float64())

        def do_computation(operand1, operand2):
            if operator == '+':
                result = pc.add(operand1, operand2)
            elif operator == '-':
                result = pc.subtract(operand1, operand2)
            elif operator == '*':
                result = pc.multiply(operand1, operand2)
            elif operator == '/':
                result = pc.divide(operand1, operand2)
            else:
                raise ValueError('Unknown operator: ' + operator)
            return pc.add(result, constant)

        column_spec1 = table1_spec.getColumnSpec(operand1_name)
        column_spec2 = table2_spec.getColumnSpec(operand2_name)
        out_table_spec = self._configure(column_spec1, column_spec2)

        out_table_batch_consumer = knime.ArrowIPCBatchConsumer(out_table_spec)

        # TODO: second table

        schema = None
        while True:
            table1_batch_file_path = table1_batch_supplier.getNextBatchFile()
            if table1_batch_file_path is None:
                break

            # TODO: compression
            # table1_batch_file = pa.input_stream(table1_batch_file_path, compression='lz4')
            table1_batch_file = pa.OSFile(table1_batch_file_path)
            # TODO: try out memory mapping; memory mapping vs compression
            table1_batch = pi.read_record_batch(table1_batch_file, table1_schema)

            result_column = do_computation(table1_batch[operand1_arrow_name], table1_batch[operand2_arrow_name])
            out_batch = pa.RecordBatch.from_arrays([table1_batch[0], result_column], names=["Row ID", "Result"])

            if schema is None:
                schema = out_batch.schema
                metadata = {"KNIME:basic:factoryVersions": "0,0",
                            "KNIME:basic:chunkSize": str(out_batch.num_rows)}
                schema = schema.add_metadata(metadata)

            out_batch_file_path = out_table_batch_consumer.getNextWriteFile()
            # TODO: compression; try out memory mapping
            # TODO: set Arrow format metadata to fixed version? (current default: V5)
            with pi.RecordBatchStreamWriter(out_batch_file_path, schema) as writer:
                writer.write_batch(out_batch)

            out_table_batch_consumer.notifyDoneWritingBatch(out_batch.num_rows)

        outObjects = cs.new_array(knime.Object, 1)
        outObjects[0] = out_table_batch_consumer
        return outObjects

    def reset(self):
        # Nothing to do.
        pass

    class Java:
        implements = ["org.knime.python2.mynode.PythonNodeModel"]
