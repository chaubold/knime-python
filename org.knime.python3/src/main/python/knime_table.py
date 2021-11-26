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
Defines the general Table object that is used to transfer data from and to KNIME.

@author Carsten Haubold, KNIME GmbH, Konstanz, Germany
"""


from abc import abstractmethod, ABC
from typing import Iterator, List, Optional, Tuple, Type, Union


class _FixedSizeListView:
    """
    A list view that allows element access but no deletion or append.

    Attention: if the underlying list grows, more values will also be available in this view!
    """

    def __init__(self, data, name):
        if not isinstance(data, list):
            raise TypeError("Can only convert a list into a FixedSizeList")
        self._data = data
        self._name = name

    @property
    def _name_for_len(self):
        if len(self._data) == 1:
            return self._name
        else:
            return self._name + "s"

    def __getitem__(self, idx):
        if not 0 <= idx < len(self._data):
            raise KeyError(
                f"Invalid port index {idx}, only {len(self._data)} {self._name_for_len} are available"
            )
        return self._data[idx]

    def __setitem__(self, idx, value):
        if not 0 <= idx < len(self._data):
            raise KeyError(
                f"Invalid port index {idx}, only {len(self._data)} {self._name_for_len} are available"
            )
        self._data[idx] = value

    def __iter__(self):
        return iter(self._data)

    def __len__(self):
        return len(self._data)

    def __str__(self):
        return f"{len(self._data)} {self._name_for_len}: [{', '.join([str(v) for v in self._data])}]"


class _Backend(ABC):
    """
    The backend instanciates the appropriate types of Tables and Batches.
    """

    @abstractmethod
    def batch(
        data: Union["pandas.DataFrame", "pyarrow.RecordBatch"],
        sentinel: Optional[Union[str, int]] = None,
    ) -> "Batch":
        pass

    @abstractmethod
    def write_table(
        data: Union["ReadTable", "pandas.DataFrame", "pyarrow.Table"],
        sentinel: Optional[Union[str, int]] = None,
    ) -> "WriteTable":
        pass

    @abstractmethod
    def batch_write_table(
        sentinel: Optional[Union[str, int]] = None,
    ) -> "BatchWriteTable":
        pass


_backend: _Backend = (
    None  # The globally instanciated backend for creating tables and batches
)


class _Tabular(ABC):
    """
    Baseclass for tabular shaped data.
    """

    @property
    def shape(self) -> Tuple[int, int]:
        """
        Returns a tuple in the form (numRows, numColumns) representing the shape of this Table.

        If the Table is not completely available yet because Batches are still appended to it,
        querying the shape blocks until all data is available
        """
        return (self.num_rows, self.num_columns)

    @property
    @abstractmethod
    def num_rows(self) -> int:
        """
        Return the number of rows in the Table.

        If the Table is not completely available yet because Batches are still appended to it,
        querying the number of rows blocks until all data is available.
        """
        pass

    @property
    @abstractmethod
    def num_columns(self) -> int:
        """
        Return the number of columns in the Table.
        """
        pass

    @property
    @abstractmethod
    def column_names(self) -> Tuple[str, ...]:
        """
        Return the list of column names
        """
        pass


class _SlicedTabular(_Tabular):
    """
    An instance of sliced data that can not be sliced any further.
    """

    def __init__(
        self,
        delegate: _Tabular,
        row_slice: slice,
        column_slice: Union[slice, List[int], List[str]],
    ):
        if not isinstance(row_slice, slice):
            raise TypeError(
                f"Rows can only be sliced by slice objects but got {type(slice)}"
            )
        self._row_slice = row_slice
        self._column_slice = column_slice
        if isinstance(column_slice, slice):
            self._column_names = delegate.column_names[column_slice]
        elif isinstance(column_slice, list) or isinstance(column_slice, tuple):
            if len(column_slice) == 0:
                raise IndexError("Empty column selection is invalid")
            elif isinstance(column_slice[0], int):
                self._column_names = [delegate.column_names[i] for i in column_slice]
            elif isinstance(column_slice[0], str):
                self._column_names = column_slice
        else:
            raise TypeError(f"Columns cannot be sliced by {type(column_slice)}")

        self._num_rows = len(range(*row_slice.indices(delegate.num_rows)))
        self._delegate = delegate

    @property
    def num_rows(self) -> int:
        """
        Return the number of rows in the Table.

        If the Table is not completely available yet because Batches are still appended to it,
        querying the number of rows blocks until all data is available.
        """
        return self._num_rows

    @property
    def num_columns(self) -> int:
        """
        Return the number of columns in the Table.
        """
        return len(self._column_names)

    @property
    def column_names(self) -> Tuple[str, ...]:
        """
        Return the list of column names
        """
        return self._column_names


class _ReadData(ABC):
    """
    Baseclass of data that can be converted to Pandas or PyArrow
    """

    @abstractmethod
    def to_pandas(
        self,
        sentinel: Optional[Union[str, int]] = None,
    ) -> "pandas.DataFrame":
        """
        Access the tabular data as a pandas.DataFrame.

        ### Arguments:
        - sentinel:  Replace missing values in integral columns by the given value, one of:
                - "min" min int32 or min int64 depending on the type of the column
                - "max" max int32 or max int64 depending on the type of the column
                - An integer value that should be inserted for each missing value

        ### Raises:
        - IndexError: If rows or columns were requested outside of the available shape
        """
        pass

    @abstractmethod
    def to_pyarrow(
        self,
        sentinel: Optional[Union[str, int]] = None,
    ) -> Union["pyarrow.RecordBatch", "pyarrow.Table"]:
        """
        Access this tabular data as a pyarrow.RecordBatch or pyarrow.Table. The returned
        type depends on the type of the underlying object. When called on a ReadTable,
        returns a pyarrow.Table.

        ### Arguments:
        - sentinel: Replace missing values in integral columns by the given value, one of:
            - "min" min int32 or min int64 depending on the type of the column
            - "max" max int32 or max int64 depending on the type of the column
            - An integer value that should be inserted for each missing value

        ### Raises:
        - IndexError: If rows or columns were requested outside of the available shape
        """
        pass


class SlicedDataView(_SlicedTabular, _ReadData):
    """
    A sliced view of tabular data. The data can be converted to pandas.DataFrame or to pyarrow,
    but not be sliced any further.
    """

    def __init__(self, delegate, row_slice, column_slice):
        super().__init__(delegate, row_slice, column_slice)

    def to_pandas(
        self,
        sentinel: Optional[Union[str, int]] = None,
    ) -> "pandas.DataFrame":
        return self._delegate.to_pandas(sentinel, self._row_slice, self._column_slice)

    def to_pyarrow(
        self,
        sentinel: Optional[Union[str, int]] = None,
    ) -> Union["pyarrow.RecordBatch", "pyarrow.Table"]:
        return self._delegate.to_pyarrow(sentinel, self._row_slice, self._column_slice)


class Batch(_Tabular, _ReadData):
    """
    A Batch is a part of a table containing data. A Batch should always fit into system memory,
    thus all methods accessing the data will be processed immediately and synchronously.

    It can be sliced before the data is accessed as pandas DataFrame or pyarrow.RecordBatch.
    """

    def __getitem__(
        self, slicing: Union[slice, Tuple[slice, Union[slice, List[int], List[str]]]]
    ) -> SlicedDataView:
        """
        Create a row and column sliced view of this Batch, with slicing syntax similar to that of numpy arrays,
        but columns can also be addressed as index lists or via a list of column names.

        ### Arguments:
        - row_slice: a slice object describing which rows to use
        - column_slice: Optional. A slice object, a list of column indices, or a list of column names

        ### Returns:
        - a SlicedDataView that can be converted to pandas or pyarrow.

        ### Examples:

        Get the first 100 rows of columns 1,2,3,4:
        >>> sliced_batch = batch[:100, 1:5]

        Get all rows of the columns "name" and "age":
        >>> sliced_batch = batch[:, ["name", "age"]]

        The returned `sliced_batches` cannot be sliced further. But they can be converted to pandas or pyarrow.
        """
        if isinstance(slicing, Tuple):
            if not 0 < len(slicing) <= 2:
                raise TypeError("Invalid slicing of 2-dimensional batch")
            return SlicedDataView(self, slicing[0], slicing[1])
        else:
            return SlicedDataView(self, slicing, slice(None))


class _Table(_Tabular):
    """
    A KNIME Table provides the general functionality to access KNIME tabular data which might be larger than the
    system's memory and/or not yet completely written to disk.

    The underlying data is available in batches of rows. At least one Batch of this Table is available, providing
    immediate access to the number of columns and the column names.
    """

    @property
    @abstractmethod
    def num_batches(self) -> int:
        """
        Return the number of Batches in this Table.

        If the Table is not completely available yet because Batches are still appended to it,
        querying the number of batches blocks until all data is available.
        """
        pass

    def __len__(self) -> int:
        """Return the number of Batches of this Table"""
        return self.num_batches


class ReadTable(_Table, _ReadData):
    """
    A KNIME ReadTable provides access to the data provided from KNIME, either in full (must fit into memory)
    or split into row-wise Batches.
    """

    @abstractmethod
    def batches(self) -> Iterator[Batch]:
        """
        Return an iterator over the Batches in this Table. If the iterator is advanced to a Batch
        that is not available yet, it will block until the data is present.
        """
        pass

    def __getitem__(
        self, slicing: Union[slice, Tuple[slice, Union[slice, List[int], List[str]]]]
    ) -> SlicedDataView:
        """
        Create a row and column sliced view of this ReadTable, with slicing syntax similar to that of numpy arrays,
        but columns can also be addressed as index lists or via a list of column names.

        ### Arguments:
        - row_slice: a slice object describing which rows to use
        - column_slice: Optional. A slice object, a list of column indices, or a list of column names

        ### Returns:
        - a SlicedDataView that can be converted to pandas or pyarrow.

        ### Examples:

        Get the first 100 rows of columns 1,2,3,4:
        >>> sliced_table = table[:100, 1:5]

        Get all rows of the columns "name" and "age":
        >>> sliced_table = table[:, ["name", "age"]]

        The returned `sliced_tables` cannot be sliced further. But they can be converted to pandas or pyarrow.
        """
        if isinstance(slicing, Tuple):
            if not 0 < len(slicing) <= 2:
                raise TypeError("Invalid slicing of 2-dimensional table")
            return SlicedDataView(self, slicing[0], slicing[1])
        else:
            return SlicedDataView(self, slicing, slice(None))


class BatchWriteTable(_Table):
    """
    A table that can be filled batch by batch.
    """

    @staticmethod
    def create() -> "BatchWriteTable":
        """Create an empty BatchWriteTable"""
        return _backend.batch_write_table()

    @abstractmethod
    def append(
        self,
        data: Union[Batch, "pandas.DataFrame", "pyarrow.RecordBatch"],
        sentinel: Optional[Union[str, int]] = None,
    ):
        """
        Appends a batch with the given data to the end of this Table. The number of columns, as well as their
        data types, must match that of the previous batches in this Table.

        ### Arguments:
        - data: A Batch, a pandas.DataFrame or a pyarrow.RecordBatch
        - sentinel:
          Only if data is a pandas.DataFrame or pyarrow.RecordBatch.
          Interpret the following values in integral columns as missing value:
            - "min" min int32 or min int64 depending on the type of the column
            - "max" max int32 or max int64 depending on the type of the column
            - a special integer value that should be interpreted as missing value

        ### Raise:
        - ValueError:
          If the new batch does not have the same columns as previous batches in this WriteTable.
        """
        pass


class WriteTable(_Table):
    """
    A Table that can be filled as a whole, or by appending individual batches. The data is serialized
    to disk batch by batch. Individual batches will be available to KNIME as soon as they are written.
    """

    pass


def write_table(
    data: Union[ReadTable, "pandas.DataFrame", "pyarrow.Table"],
    sentinel: Optional[Union[str, int]] = None,
) -> WriteTable:
    """
    Factory method to create a WriteTable given a pandas.DataFrame or a pyarrow.Table.
    Internally creates a WriteTable using its create, from_pandas or from_pyarrow methods respectively.

    ### Arguments:
    - data: A ReadTable, pandas.DataFrame or a pyarrow.Table
    - sentinel:
      Interpret the following values in integral columns as missing value:
        - "min" min int32 or min int64 depending on the type of the column
        - "max" max int32 or max int64 depending on the type of the column
        - a special integer value that should be interpreted as missing value
    """
    return _backend.write_table(data, sentinel)


def batch_write_table() -> BatchWriteTable:
    """
    Factory method to create an empty BatchWriteTable that can be filled batch by batch.
    """
    return _backend.batch_write_table()


def batch(
    data: Union["pandas.DataFrame", "pyarrow.Table"],
    sentinel: Optional[Union[str, int]] = None,
) -> Batch:
    """
    Create a Batch from the given data.

    ### Arguments:
    - data: A pandas.DataFrame or a pyarrow.RecordBatch
    - sentinel:
      Interpret the following values in integral columns as missing value:
        - "min" min int32 or min int64 depending on the type of the column
        - "max" max int32 or max int64 depending on the type of the column
        - a special integer value that should be interpreted as missing value
    """
    return _backend.batch(data, sentinel)
