// automatically generated by the FlatBuffers compiler, do not modify

package org.knime.flatbuffers.flatc;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class DoubleColumn extends Table {
  public static DoubleColumn getRootAsDoubleColumn(ByteBuffer _bb) { return getRootAsDoubleColumn(_bb, new DoubleColumn()); }
  public static DoubleColumn getRootAsDoubleColumn(ByteBuffer _bb, DoubleColumn obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; }
  public DoubleColumn __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public String name() { int o = __offset(4); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer nameAsByteBuffer() { return __vector_as_bytebuffer(4, 1); }
  public double values(int j) { int o = __offset(6); return o != 0 ? bb.getDouble(__vector(o) + j * 8) : 0; }
  public int valuesLength() { int o = __offset(6); return o != 0 ? __vector_len(o) : 0; }
  public ByteBuffer valuesAsByteBuffer() { return __vector_as_bytebuffer(6, 8); }

  public static int createDoubleColumn(FlatBufferBuilder builder,
      int nameOffset,
      int valuesOffset) {
    builder.startObject(2);
    DoubleColumn.addValues(builder, valuesOffset);
    DoubleColumn.addName(builder, nameOffset);
    return DoubleColumn.endDoubleColumn(builder);
  }

  public static void startDoubleColumn(FlatBufferBuilder builder) { builder.startObject(2); }
  public static void addName(FlatBufferBuilder builder, int nameOffset) { builder.addOffset(0, nameOffset, 0); }
  public static void addValues(FlatBufferBuilder builder, int valuesOffset) { builder.addOffset(1, valuesOffset, 0); }
  public static int createValuesVector(FlatBufferBuilder builder, double[] data) { builder.startVector(8, data.length, 8); for (int i = data.length - 1; i >= 0; i--) builder.addDouble(data[i]); return builder.endVector(); }
  public static void startValuesVector(FlatBufferBuilder builder, int numElems) { builder.startVector(8, numElems, 8); }
  public static int endDoubleColumn(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
}

