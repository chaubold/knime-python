// automatically generated by the FlatBuffers compiler, do not modify

package org.knime.python2.serde.flatbuffers.flatc;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class IntegerCollectionCell extends Table {
  public static IntegerCollectionCell getRootAsIntegerCollectionCell(ByteBuffer _bb) { return getRootAsIntegerCollectionCell(_bb, new IntegerCollectionCell()); }
  public static IntegerCollectionCell getRootAsIntegerCollectionCell(ByteBuffer _bb, IntegerCollectionCell obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; }
  public IntegerCollectionCell __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public int value(int j) { int o = __offset(4); return o != 0 ? bb.getInt(__vector(o) + j * 4) : 0; }
  public int valueLength() { int o = __offset(4); return o != 0 ? __vector_len(o) : 0; }
  public ByteBuffer valueAsByteBuffer() { return __vector_as_bytebuffer(4, 4); }
  public boolean missing(int j) { int o = __offset(6); return o != 0 ? 0!=bb.get(__vector(o) + j * 1) : false; }
  public int missingLength() { int o = __offset(6); return o != 0 ? __vector_len(o) : 0; }
  public ByteBuffer missingAsByteBuffer() { return __vector_as_bytebuffer(6, 1); }
  public boolean keepDummy() { int o = __offset(8); return o != 0 ? 0!=bb.get(o + bb_pos) : false; }

  public static int createIntegerCollectionCell(FlatBufferBuilder builder,
      int valueOffset,
      int missingOffset,
      boolean keepDummy) {
    builder.startObject(3);
    IntegerCollectionCell.addMissing(builder, missingOffset);
    IntegerCollectionCell.addValue(builder, valueOffset);
    IntegerCollectionCell.addKeepDummy(builder, keepDummy);
    return IntegerCollectionCell.endIntegerCollectionCell(builder);
  }

  public static void startIntegerCollectionCell(FlatBufferBuilder builder) { builder.startObject(3); }
  public static void addValue(FlatBufferBuilder builder, int valueOffset) { builder.addOffset(0, valueOffset, 0); }
  public static int createValueVector(FlatBufferBuilder builder, int[] data) { builder.startVector(4, data.length, 4); for (int i = data.length - 1; i >= 0; i--) builder.addInt(data[i]); return builder.endVector(); }
  public static void startValueVector(FlatBufferBuilder builder, int numElems) { builder.startVector(4, numElems, 4); }
  public static void addMissing(FlatBufferBuilder builder, int missingOffset) { builder.addOffset(1, missingOffset, 0); }
  public static int createMissingVector(FlatBufferBuilder builder, boolean[] data) { builder.startVector(1, data.length, 1); for (int i = data.length - 1; i >= 0; i--) builder.addBoolean(data[i]); return builder.endVector(); }
  public static void startMissingVector(FlatBufferBuilder builder, int numElems) { builder.startVector(1, numElems, 1); }
  public static void addKeepDummy(FlatBufferBuilder builder, boolean keepDummy) { builder.addBoolean(2, keepDummy, false); }
  public static int endIntegerCollectionCell(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
}

