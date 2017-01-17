// automatically generated by the FlatBuffers compiler, do not modify

package org.knime.flatbuffers.flatc;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class BooleanCollectionColumn extends Table {
  public static BooleanCollectionColumn getRootAsBooleanCollectionColumn(ByteBuffer _bb) { return getRootAsBooleanCollectionColumn(_bb, new BooleanCollectionColumn()); }
  public static BooleanCollectionColumn getRootAsBooleanCollectionColumn(ByteBuffer _bb, BooleanCollectionColumn obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; }
  public BooleanCollectionColumn __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public String name() { int o = __offset(4); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer nameAsByteBuffer() { return __vector_as_bytebuffer(4, 1); }
  public BooleanCollectionCell values(int j) { return values(new BooleanCollectionCell(), j); }
  public BooleanCollectionCell values(BooleanCollectionCell obj, int j) { int o = __offset(6); return o != 0 ? obj.__assign(__indirect(__vector(o) + j * 4), bb) : null; }
  public int valuesLength() { int o = __offset(6); return o != 0 ? __vector_len(o) : 0; }

  public static int createBooleanCollectionColumn(FlatBufferBuilder builder,
      int nameOffset,
      int valuesOffset) {
    builder.startObject(2);
    BooleanCollectionColumn.addValues(builder, valuesOffset);
    BooleanCollectionColumn.addName(builder, nameOffset);
    return BooleanCollectionColumn.endBooleanCollectionColumn(builder);
  }

  public static void startBooleanCollectionColumn(FlatBufferBuilder builder) { builder.startObject(2); }
  public static void addName(FlatBufferBuilder builder, int nameOffset) { builder.addOffset(0, nameOffset, 0); }
  public static void addValues(FlatBufferBuilder builder, int valuesOffset) { builder.addOffset(1, valuesOffset, 0); }
  public static int createValuesVector(FlatBufferBuilder builder, int[] data) { builder.startVector(4, data.length, 4); for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]); return builder.endVector(); }
  public static void startValuesVector(FlatBufferBuilder builder, int numElems) { builder.startVector(4, numElems, 4); }
  public static int endBooleanCollectionColumn(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
}

