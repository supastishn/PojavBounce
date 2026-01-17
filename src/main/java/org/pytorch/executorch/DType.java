/*
 * Decompiled with CFR 0.152.
 */
package org.pytorch.executorch;

import org.pytorch.executorch.annotations.Experimental;

@Experimental
public enum DType {
    UINT8(0),
    INT8(1),
    INT16(2),
    INT32(3),
    INT64(4),
    HALF(5),
    FLOAT(6),
    DOUBLE(7),
    COMPLEX_HALF(8),
    COMPLEX_FLOAT(9),
    COMPLEX_DOUBLE(10),
    BOOL(11),
    QINT8(12),
    QUINT8(13),
    QINT32(14),
    BFLOAT16(15),
    QINT4X2(16),
    QINT2X4(17),
    BITS1X8(18),
    BITS2X4(19),
    BITS4X2(20),
    BITS8(21),
    BITS16(22);

    final int jniCode;

    private DType(int jniCode) {
        this.jniCode = jniCode;
    }

    public static DType fromJniCode(int jniCode) {
        for (DType dtype : DType.values()) {
            if (dtype.jniCode != jniCode) continue;
            return dtype;
        }
        throw new IllegalArgumentException("No DType found for jniCode " + jniCode);
    }
}
