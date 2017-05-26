// automatically generated by the FlatBuffers compiler, do not modify

package org.knime.python2.serde.flatbuffers.flatc;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class ByteCell extends Table {
  public static ByteCell getRootAsByteCell(ByteBuffer _bb) { return getRootAsByteCell(_bb, new ByteCell()); }
  public static ByteCell getRootAsByteCell(ByteBuffer _bb, ByteCell obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; }
  public ByteCell __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public int value(int j) { int o = __offset(4); return o != 0 ? bb.get(__vector(o) + j * 1) & 0xFF : 0; }
  public int valueLength() { int o = __offset(4); return o != 0 ? __vector_len(o) : 0; }
  public ByteBuffer valueAsByteBuffer() { return __vector_as_bytebuffer(4, 1); }

  public static int createByteCell(FlatBufferBuilder builder,
      int valueOffset) {
    builder.startObject(1);
    ByteCell.addValue(builder, valueOffset);
    return ByteCell.endByteCell(builder);
  }

  public static void startByteCell(FlatBufferBuilder builder) { builder.startObject(1); }
  public static void addValue(FlatBufferBuilder builder, int valueOffset) { builder.addOffset(0, valueOffset, 0); }
  public static int createValueVector(FlatBufferBuilder builder, byte[] data) { builder.startVector(1, data.length, 1); for (int i = data.length - 1; i >= 0; i--) builder.addByte(data[i]); return builder.endVector(); }
  public static void startValueVector(FlatBufferBuilder builder, int numElems) { builder.startVector(1, numElems, 1); }
  public static int endByteCell(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
}

