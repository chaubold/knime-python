# automatically generated by the FlatBuffers compiler, do not modify

# namespace: flatc

import flatbuffers

class ByteCollectionColumn(object):
    __slots__ = ['_tab']

    @classmethod
    def GetRootAsByteCollectionColumn(cls, buf, offset):
        n = flatbuffers.encode.Get(flatbuffers.packer.uoffset, buf, offset)
        x = ByteCollectionColumn()
        x.Init(buf, n + offset)
        return x

    # ByteCollectionColumn
    def Init(self, buf, pos):
        self._tab = flatbuffers.table.Table(buf, pos)

    # ByteCollectionColumn
    def Name(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(4))
        if o != 0:
            return self._tab.String(o + self._tab.Pos)
        return ""

    # ByteCollectionColumn
    def Values(self, j):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(6))
        if o != 0:
            x = self._tab.Vector(o)
            x += flatbuffers.number_types.UOffsetTFlags.py_type(j) * 4
            x = self._tab.Indirect(x)
            from .ByteCollectionCell import ByteCollectionCell
            obj = ByteCollectionCell()
            obj.Init(self._tab.Bytes, x)
            return obj
        return None

    # ByteCollectionColumn
    def ValuesLength(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(6))
        if o != 0:
            return self._tab.VectorLen(o)
        return 0

def ByteCollectionColumnStart(builder): builder.StartObject(2)
def ByteCollectionColumnAddName(builder, name): builder.PrependUOffsetTRelativeSlot(0, flatbuffers.number_types.UOffsetTFlags.py_type(name), 0)
def ByteCollectionColumnAddValues(builder, values): builder.PrependUOffsetTRelativeSlot(1, flatbuffers.number_types.UOffsetTFlags.py_type(values), 0)
def ByteCollectionColumnStartValuesVector(builder, numElems): return builder.StartVector(4, numElems, 4)
def ByteCollectionColumnEnd(builder): return builder.EndObject()
