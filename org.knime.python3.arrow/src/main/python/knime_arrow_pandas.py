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

from typing import Type, Union

from pandas.core.dtypes.dtypes import register_extension_dtype
import pandas.api.extensions as pdext

import pyarrow as pa
import knime_gateway as kg
import knime_types as kt
import knime_arrow_types as kat
import knime_arrow_struct_dict_encoding as kas
import pandas as pd
import numpy as np
import psutil
import os
import time


def get_process_mb() -> float:
    process = psutil.Process(os.getpid())
    return process.memory_info().rss / float(2**20)


def pandas_df_to_arrow(
    data_frame: pd.DataFrame, to_batch=False
) -> Union[pa.Table, pa.RecordBatch]:

    if data_frame.shape == (
        0,
        0,
    ):
        if to_batch:
            return pa.RecordBatch.from_arrays([])
        else:
            return pa.table([])

    col_names = [str(c) for c in data_frame.columns]
    row_keys = data_frame.index.to_series().astype(str)

    if True:
        start = time.time()
        print(f"1. Memory usage before concat: {get_process_mb()} mb")

        # Way 1 (tests in the testflow are passed):
        # data_frame.insert(0, column="Row ID", value=data_frame.index.values.astype(str))

        # Way 2 (tests in the testflow are passed):
        # data_frame = data_frame.rename_axis("Row ID").reset_index()
        # data_frame["Row ID"] = data_frame["Row ID"].astype("string", copy=False)

        # Way 3 (tests in the testflow are NOT passed):
        data_frame = pd.concat(
            [row_keys, data_frame],
            axis=1,
            copy=False,
            ignore_index=True,
        )
        data_frame.columns = ["Row ID"] + col_names

        data_frame1 = data_frame

        if to_batch:
            result = pa.RecordBatch.from_pandas(data_frame1, preserve_index=False)
        else:
            result = pa.Table.from_pandas(data_frame1, preserve_index=False)

        print(f"1. Memory usage after concat: {get_process_mb()} mb")
        end = time.time()
        print(f"1. Took {end - start}")

        # This produces wrong results! The Table Difference Checker will find differences after a plain roundtrip

    if False:
        start = time.time()
        print(f"2. Memory usage before concat: {get_process_mb()} mb")

        data_frame2 = pd.concat(
            [row_keys.reset_index(drop=True), data_frame.reset_index(drop=True)], axis=1
        )
        data_frame2.columns = ["<Row Key>"] + col_names

        if to_batch:
            result2 = pa.RecordBatch.from_pandas(data_frame2)
        else:
            result2 = pa.Table.from_pandas(data_frame2)

        print(f"2. Memory usage after concat: {get_process_mb()} mb")
        end = time.time()
        print(f"2. Took {end - start}")

        # if result.schema != result2.schema:
        #     print(f"New: {result.schema}")
        #     print(f"Old: {result2.schema}")
        #     raise AssertionError

        result = result2

    if False:
        start = time.time()
        print(f"3. Memory usage before concat: {get_process_mb()} mb")

        if to_batch:
            arrow = pa.RecordBatch.from_pandas(data_frame)
        else:
            arrow = pa.Table.from_pandas(data_frame)

        arrow_keys = pa.array(row_keys, type=pa.string())
        schema = arrow.schema.remove(len(arrow.schema) - 1)
        schema = schema.insert(0, pa.field("<Row Key>", pa.string()))
        columns = [arrow_keys] + arrow.columns[:-1]

        if to_batch:
            result3 = pa.RecordBatch.from_arrays(columns, schema=schema)
        else:
            result3 = pa.Table.from_arrays(columns, schema=schema)

        print(f"3. Memory usage after concat: {get_process_mb()} mb")
        end = time.time()
        print(f"3. Took {end - start}")

        result = result3

    return result


def arrow_data_to_pandas_df(data: Union[pa.Table, pa.RecordBatch]) -> pd.DataFrame:
    # Use Pandas' String data type if available instead of "object" if we're using a
    # Pandas version that is new enough. Gives better type safety and preserves its
    # type even if all values are missing in a column.

    if hasattr(pd, "StringDtype"):

        def mapper(dtype):
            if dtype == pa.string():
                return pd.StringDtype()
            else:
                return None

        data_frame = data.to_pandas(types_mapper=mapper)
    else:
        data_frame = data.to_pandas()

    # The first column is interpreted as the index (row keys)
    data_frame.set_index(data_frame.columns[0], inplace=True)

    return data_frame


@register_extension_dtype
class PandasLogicalTypeExtensionType(pdext.ExtensionDtype):
    def __init__(self, storage_type: pa.DataType, logical_type: str, converter):
        self._storage_type = storage_type
        self._logical_type = logical_type
        self._converter = converter

        # used by pandas to query all attributes of this ExtensionType
        self._metadata = ("_storage_type", "_logical_type", "_converter")

    na_value = None
    type = bytes  # We just say that this is raw data?! No need to be interpreted :)

    @property
    def name(self):
        return f"PandasLogicalTypeExtensionType({self._storage_type}, {self._logical_type})"

    def construct_array_type(self):
        return KnimePandasExensionArray

    def construct_from_string(cls: Type[pdext.ExtensionDtype], string: str):
        # TODO implement this?
        raise NotImplementedError("construct from string not available yet")

    def __from_arrow__(self, arrow_array):
        return KnimePandasExensionArray(
            self._storage_type, self._logical_type, self._converter, arrow_array
        )

    def __str__(self):
        return f"PandasLogicalTypeExtensionType({self._storage_type}, {self._logical_type})"


class KnimePandasExensionArray(pdext.ExtensionArray):
    def __init__(
        self,
        storage_type: pa.DataType,
        logical_type: str,
        converter,
        data: Union[pa.Array, pa.ChunkedArray],
    ):
        if data is None:
            raise ValueError("Cannot create empty KnimePandasExtensionArray")
        self._data = data
        self._converter = converter
        self._storage_type = storage_type
        self._logical_type = logical_type

    def __arrow_array__(self, type=None):
        return self._data

    @classmethod
    def _from_sequence(
        cls,
        scalars,
        dtype=None,
        copy=None,
        storage_type=None,
        logical_type=None,
        converter=None,
    ):
        if scalars is None:
            raise ValueError("Cannot create KnimePandasExtensionArray from empty data")

        # easy case
        if isinstance(scalars, pa.Array) or isinstance(scalars, pa.ChunkedArray):
            if not isinstance(scalars.type, kat.LogicalTypeExtensionType):
                raise ValueError(
                    "KnimePandasExtensionArray must be backed by LogicalTypeExtensionType values"
                )
            return KnimePandasExensionArray(
                scalars.type.storage_type,
                scalars.type.logical_type,
                scalars.type._converter,
                scalars,
            )

        if isinstance(dtype, PandasLogicalTypeExtensionType):
            # in this case we can extract storage, logical_type and converter
            storage_type = dtype._storage_type
            logical_type = dtype._logical_type
            converter = dtype._converter
            if converter is not None and converter.needs_conversion():
                scalars = [converter.encode(s) for s in scalars]

        if storage_type is None:
            raise ValueError(
                "Can only create KnimePandasExtensionArray from a sequence if the storage type is given."
            )

        # needed for pandas ExtensionArray API
        arrow_type = kat.LogicalTypeExtensionType(converter, storage_type, logical_type)

        a = pa.array(scalars, type=storage_type)
        extension_array = pa.ExtensionArray.from_storage(arrow_type, a)
        return KnimePandasExensionArray(
            storage_type, logical_type, converter, extension_array
        )

    @classmethod
    def _from_factorized(cls, values, original):
        # needed for pandas ExtensionArray API
        raise NotImplementedError(
            "KnimePandasExtensionArray cannot be created from factorized yet."
        )

    def __getitem__(self, item):
        if isinstance(item, int):
            return self._data[item].as_py()
        elif isinstance(item, slice):
            (start, stop, step) = item.indices(len(self._data))
            # if step == 1:
            #     return self._data.slice(offset=start, length=stop - start)

            indices = list(range(start, stop, step))
            return self.take(indices)
        elif isinstance(item, list):
            # fetch objects at the individual indices
            return self.take(item)
        elif isinstance(item, np.ndarray):
            # masked array
            raise NotImplementedError("Cannot index using masked array from numpy yet.")

    def __setitem__(self, item, value):
        raise NotImplementedError(
            "Columns with non-primitive KNIME data types do not support modification yet."
        )

    def __len__(self):
        return len(self._data)

    def __eq__(self, other) -> bool:
        if not isinstance(other, KnimePandasExensionArray):
            return False
        return (
            other._storage_type == self._storage_type
            and other._logical_type == self._logical_type
            and other._converter == self._converter
            and other._data == self._data
        )

    @property
    def dtype(self):
        # needed for pandas ExtensionArray API
        return PandasLogicalTypeExtensionType(
            self._storage_type, self._logical_type, self._converter
        )

    @property
    def nbytes(self):
        # needed for pandas ExtensionArray API
        return self._data.nbytes

    def isna(self):
        # needed for pandas ExtensionArray API
        return self._data.is_null().to_numpy()

    def take(self, indices, *args, **kwargs) -> "KnimePandasExensionArray":
        # needed for pandas ExtensionArray API
        # TODO: handle allow_fill and fill_value kwargs?

        storage = kat._to_storage_array(self._data)
        taken = storage.take(indices)
        wrapped = kat._to_extension_array(taken, self._data.type)

        return self._from_sequence(
            wrapped,
            storage_type=self._storage_type,
            logical_type=self._logical_type,
            converter=self._converter,
        )

    def copy(self):
        # needed for pandas ExtensionArray API
        # TODO: do we really want to copy the data? This thing is read only anyways... Unless we implement __setitem__ and concat
        return self

    @classmethod
    def _concat_same_type(cls, to_concat):
        # needed for pandas ExtensionArray API

        if len(to_concat) < 1:
            raise ValueError("Nothing to concatenate")
        elif len(to_concat) == 1:
            return to_concat[0]

        chunks = []
        for pandas_ext_array in to_concat:
            d = pandas_ext_array._data
            if isinstance(d, pa.ChunkedArray):
                chunks += d.chunks
            else:
                chunks.append(d)

        combined_data = pa.chunked_array(chunks)
        first = to_concat[0]
        return KnimePandasExensionArray(
            first._storage_type, first._logical_type, first._converter, combined_data
        )
