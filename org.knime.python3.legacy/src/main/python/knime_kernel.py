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
import itertools
import pandas as pd
import py4j.clientserver as cs
import pyarrow as pa
import sys
import threading
# Use Concurrent futures?
from concurrent.futures._base import Future  # TODO: both Future and _WorkItem are internal API, use something else?
from concurrent.futures.thread import _WorkItem
from io import StringIO
from queue import Queue, Full
from typing import List, Dict

from timeit import default_timer as timer

import knime_gateway



class PythonKernel(knime_gateway.EntryPoint):

    def __init__(self):
        self._workspace = {}
        self._main_loop_queue = Queue()
        self._main_loop_lock = threading.Lock()
        self._main_loop_stopped = False

    def enter_main_loop(self):
        while True:
            work_item = self._main_loop_queue.get()
            if work_item is not None:
                work_item.run()
                self._main_loop_queue.task_done()
                del work_item
                continue
            else:
                # Poison pill: exit loop
                self._main_loop_queue.task_done()
                return

    def stop_main_loop(self):
        with self._main_loop_lock:
            self._main_loop_stopped = True
            queue = self._main_loop_queue
            while True:
                with queue.all_tasks_done:
                    queue.queue.clear()
                    queue.unfinished_tasks = 0
                try:
                    # Poison pill
                    queue.put_nowait(None)
                except Full:
                    continue
                else:
                    break

    def putFlowVariables(self, name: str, flow_variables) -> None:
        self._workspace[name] = dict(flow_variables)

    def getFlowVariables(self, name: str) -> Dict[str, object]:
        # FIXME: why does py4j's auto-conversion not work? It should be enabled...
        from py4j.java_collections import MapConverter
        return MapConverter().convert(self._workspace[name],
                                      knime_gateway.client_server._gateway_client)
        # return self._workspace[name]

    def putTableIntoWorkspace(self, name: str, data_source, row_limit: int = None) -> None:

        source = knime_gateway.data_source_mapper(data_source)
        try:
            tic = timer()
            batches = []
            if row_limit is not None:
                if row_limit > 0:
                    first_batch = source[0]
                    num_batches = (row_limit + first_batch.num_rows - 1) // first_batch.num_rows  # Ceiling division
                    num_batches = min(num_batches, len(source))
                    batches = itertools.chain([first_batch], (source[i] for i in range(1, num_batches)))
            else:
                batches = (source[i] for i in range(len(source)))
            table = pa.Table.from_batches(batches)
            if row_limit is not None and table.num_rows != row_limit:
                table = table.slice(0, row_limit)
            toc = timer()
            print("PYTHON: PUT: BATCHES TO TABLE:", toc - tic)
            tic = timer()
            data_frame = table.to_pandas()
            # The first column of a KNIME table makes up its index (row keys).
            data_frame.set_index(data_frame.columns[0], inplace=True)
            self._workspace[name] = data_frame
            toc = timer()
            print("PYTHON: PUT: TABLE TO DATAFRAME:", toc - tic)
        finally:
            tic = timer()
            source.close()
            toc = timer()
            print("PYTHON: GET: CLOSE SOURCE:", toc - tic)

    def getTableFromWorkspace(self, name: str, data_sink) -> None:
        sink = knime_gateway.data_sink_mapper(data_sink)
        try:
            tic = timer()
            data_frame = PythonKernel._standardize_index(self._workspace[name])
            table = pa.Table.from_pandas(data_frame, preserve_index=True)
            toc = timer()
            print("PYTHON: GET: DATAFRAME TO TABLE:", toc - tic)
            # TODO: release data_frame as early as possible. However, if a node has multiple outputs, they could
            #  reference the same table several times...

            # Index is appended as last column, move to front to comply with KNIME's table format.
            tic = timer()
            last_column_index = table.num_columns - 1
            index_field = table.field(last_column_index)
            index_column = table.column(last_column_index)
            table = table.remove_column(last_column_index)
            table = table.add_column(0, index_field, index_column)
            batches = table.to_batches()
            toc = timer()
            print("PYTHON: GET: TABLE TO BATCHES:", toc - tic)
            tic = timer()
            for batch in batches:
                sink.write(batch)
            toc = timer()
            print("PYTHON: GET: WRITE BATCHES:", toc - tic)
        finally:
            tic = timer()
            sink.close()
            toc = timer()
            print("PYTHON: GET: CLOSE SINK:", toc - tic)

    # HACK: copied from old code base (FromPandasTable)
    @staticmethod
    def _standardize_index(data_frame: pd.DataFrame) -> pd.DataFrame:
        index = data_frame.index
        if not index.is_unique:
            # Get unique duplicate row keys, limit to three to keep the error message short.
            duplicate_row_keys = set(index[index.duplicated()])
            truncate_items = len(duplicate_row_keys) > 3
            duplicate_row_keys = ", ".join("'" + str(k) + "'" for k in list(duplicate_row_keys)[:3])
            if truncate_items:
                duplicate_row_keys += ", and others"
            raise RuntimeError(
                "Output DataFrame contains duplicate values in its index: " + duplicate_row_keys +
                ". This is not supported. Please make sure that each"
                " entry in the index (i.e., each row key) is unique.")
        row_indices = []
        for i in range(len(index)):
            if type(index[i]) == int and index[i] == i:
                row_indices.append(u'Row' + str(i))
            else:
                row_indices.append(str(index[i]))
        import pandas as pd
        data_frame.set_index(pd.Index(row_indices), inplace=True)
        return data_frame

    def listVariables(self) -> List[Dict[str, str]]:
        """
        List all currently loaded modules and defined classes, functions and variables.
        """

        def object_to_string(data_object):
            try:
                object_as_string = str(data_object)
                return (object_as_string[:996] + '\n...') if len(object_as_string) > 1000 else object_as_string
            except Exception:
                return ''

        # create lists of modules, classes, functions and variables
        modules = []
        classes = []
        functions = []
        variables = []
        # iterate over dictionary to and put modules, classes, functions and variables in their respective lists
        for key, value in dict(self._workspace).items():
            # get name of the type
            var_type = type(value).__name__
            if var_type == 'module':
                modules.append({'name': key, 'type': var_type, 'value': ''})
            elif var_type == 'type':
                classes.append({'name': key, 'type': var_type, 'value': ''})
            elif var_type == 'function':
                functions.append({'name': key, 'type': var_type, 'value': ''})
            elif key != '__builtins__':
                value = object_to_string(value)
                variables.append({'name': key, 'type': var_type, 'value': value})
        # sort lists by name
        modules = sorted(modules, key=lambda k: k['name'])
        classes = sorted(classes, key=lambda k: k['name'])
        functions = sorted(functions, key=lambda k: k['name'])
        variables = sorted(variables, key=lambda k: k['name'])
        # create response list and add contents of the other lists in the order they should be displayed
        response = []
        response.extend(modules)
        response.extend(classes)
        response.extend(functions)
        response.extend(variables)
        # FIXME: why does py4j's auto-conversion not work? It should be enabled...
        from py4j.java_collections import ListConverter
        return ListConverter().convert(response, knime_gateway.client_server._gateway_client)

    def executeOnMainThread(self, source_code: str) -> List[str]:
        with self._main_loop_lock:
            if self._main_loop_stopped:
                raise RuntimeError('cannot schedule new work items after main loop stopped')
            future = Future()
            work_item = _WorkItem(future, self._execute, (source_code,), {})
            # Blocks until item is put in queue.
            self._main_loop_queue.put(work_item)
            return future.result()

    def executeOnCurrentThread(self, source_code: str) -> List[str]:
        return self._execute(source_code)

    def _execute(self, source_code: str) -> List[str]:
        # Log outputs/errors(/warnings) to both stdout/stderr and variables. Note that we do _not_ catch any otherwise
        # uncaught exceptions here for the purpose of logging. That is, uncaught exceptions will regularly lead to the
        # exceptional termination of this method and need to be handled, and possibly logged, by the callers of this
        # method.

        print("Executing code in thread:", threading.current_thread().ident, "Is main thread:",
              threading.current_thread() is threading.main_thread())

        stdout = sys.stdout
        output = StringIO()
        sys.stdout = PythonKernel._TeeingLogger(stdout, output)
        stderr = sys.stderr
        error = StringIO()
        sys.stderr = PythonKernel._TeeingLogger(stderr, error)

        try:
            exec(source_code, self._workspace)
        finally:
            sys.stdout = stdout
            sys.stderr = stderr

        # FIXME: why does py4j's auto-conversion not work? It should be enabled...
        from py4j.java_collections import ListConverter
        return ListConverter().convert([output.getvalue(), error.getvalue()],
                                       knime_gateway.client_server._gateway_client)
        # return [output.getvalue(), error.getvalue()]

    class _TeeingLogger(object):

        def __init__(self, original, branch):
            self._original = original
            self._branch = branch

        def write(self, message):
            self._original.write(message)
            self._branch.write(message)

        def writelines(self, sequence):
            self._original.writelines(sequence)
            self._branch.writelines(sequence)

        def flush(self):
            self._original.flush()
            self._branch.flush()

        def isatty(self):
            return False

    class Java:
        implements = ["org.knime.python3.legacy.NewPythonKernelBackendProxy"]


if __name__ == "__main__":
    try:
        # Hook into warning delivery.
        import warnings

        default_showwarning = warnings.showwarning


        def showwarning_hook(message, category, filename, lineno, file=None, line=None):
            """
            Copied from warnings.showwarning.
            We use this hook to prefix warning messages with "[WARN]". This makes them easier identifiable on Java
            side and helps printing them using the correct log level.
            Providing a custom hook is supported as per the API documentations:
            https://docs.python.org/2/library/warnings.html#warnings.showwarning
            https://docs.python.org/3/library/warnings.html#warnings.showwarning
            """
            try:
                if file is None:
                    file = sys.stderr
                    if file is None:
                        # sys.stderr is None when run with pythonw.exe - warnings get lost
                        return
                try:
                    # Do not change the prefix. Expected on Java side.
                    file.write("[WARN]" + warnings.formatwarning(message, category, filename, lineno, line))
                except OSError:
                    pass  # the file (probably stderr) is invalid - this warning gets lost.
            except Exception:
                # Fall back to default implementation.
                return default_showwarning(message, category, filename, lineno, file, line)


        warnings.showwarning = showwarning_hook
    except Exception:
        pass

    print("Setting up connection on Python side in thread:", threading.current_thread().ident, "Is main thread:",
          threading.current_thread() is threading.main_thread())

    # import logging
    # logger = logging.getLogger("py4j")
    # logger.setLevel(logging.DEBUG)
    # logger.addHandler(logging.StreamHandler())

    kernel = PythonKernel()
    knime_gateway.connect_to_knime(kernel)
    cs.server_connection_stopped.connect(lambda **kwargs: kernel.stop_main_loop())
    kernel.enter_main_loop()
    print("Shutting down py4j and exiting main thread...", flush=True)
    knime_gateway.client_server.shutdown()
