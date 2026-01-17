/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.util.Log
 *  com.facebook.jni.HybridData
 *  com.facebook.jni.annotations.DoNotStrip
 */
package org.pytorch.executorch;

import android.util.Log;
import com.facebook.jni.HybridData;
import com.facebook.jni.annotations.DoNotStrip;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Locale;
import org.pytorch.executorch.DType;
import org.pytorch.executorch.annotations.Experimental;

@Experimental
public abstract class Tensor {
    private static final String ERROR_MSG_DATA_BUFFER_NOT_NULL = "Data buffer must be not null";
    private static final String ERROR_MSG_DATA_ARRAY_NOT_NULL = "Data array must be not null";
    private static final String ERROR_MSG_SHAPE_NOT_NULL = "Shape must be not null";
    private static final String ERROR_MSG_SHAPE_NON_NEGATIVE = "Shape elements must be non negative";
    private static final String ERROR_MSG_DATA_BUFFER_MUST_HAVE_NATIVE_BYTE_ORDER = "Data buffer must have native byte order (java.nio.ByteOrder#nativeOrder)";
    private static final String ERROR_MSG_DATA_BUFFER_MUST_BE_DIRECT = "Data buffer must be direct (java.nio.ByteBuffer#allocateDirect)";
    @DoNotStrip
    final long[] shape;
    private static final int BYTE_SIZE_BYTES = 1;
    private static final int INT_SIZE_BYTES = 4;
    private static final int LONG_SIZE_BYTES = 8;
    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int DOUBLE_SIZE_BYTES = 8;
    @DoNotStrip
    private HybridData mHybridData;

    public static ByteBuffer allocateByteBuffer(int numElements) {
        return ByteBuffer.allocateDirect(numElements).order(ByteOrder.nativeOrder());
    }

    public static IntBuffer allocateIntBuffer(int numElements) {
        return ByteBuffer.allocateDirect(numElements * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
    }

    public static FloatBuffer allocateFloatBuffer(int numElements) {
        return ByteBuffer.allocateDirect(numElements * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    public static LongBuffer allocateLongBuffer(int numElements) {
        return ByteBuffer.allocateDirect(numElements * 8).order(ByteOrder.nativeOrder()).asLongBuffer();
    }

    public static DoubleBuffer allocateDoubleBuffer(int numElements) {
        return ByteBuffer.allocateDirect(numElements * 8).order(ByteOrder.nativeOrder()).asDoubleBuffer();
    }

    public static Tensor fromBlobUnsigned(byte[] data, long[] shape) {
        Tensor.checkArgument(data != null, ERROR_MSG_DATA_ARRAY_NOT_NULL, new Object[0]);
        Tensor.checkArgument(shape != null, ERROR_MSG_SHAPE_NOT_NULL, new Object[0]);
        Tensor.checkShape(shape);
        Tensor.checkShapeAndDataCapacityConsistency(data.length, shape);
        ByteBuffer byteBuffer = Tensor.allocateByteBuffer((int)Tensor.numel(shape));
        byteBuffer.put(data);
        return new Tensor_uint8(byteBuffer, shape);
    }

    public static Tensor fromBlob(byte[] data, long[] shape) {
        Tensor.checkArgument(data != null, ERROR_MSG_DATA_ARRAY_NOT_NULL, new Object[0]);
        Tensor.checkArgument(shape != null, ERROR_MSG_SHAPE_NOT_NULL, new Object[0]);
        Tensor.checkShape(shape);
        Tensor.checkShapeAndDataCapacityConsistency(data.length, shape);
        ByteBuffer byteBuffer = Tensor.allocateByteBuffer((int)Tensor.numel(shape));
        byteBuffer.put(data);
        return new Tensor_int8(byteBuffer, shape);
    }

    public static Tensor fromBlob(int[] data, long[] shape) {
        Tensor.checkArgument(data != null, ERROR_MSG_DATA_ARRAY_NOT_NULL, new Object[0]);
        Tensor.checkArgument(shape != null, ERROR_MSG_SHAPE_NOT_NULL, new Object[0]);
        Tensor.checkShape(shape);
        Tensor.checkShapeAndDataCapacityConsistency(data.length, shape);
        IntBuffer intBuffer = Tensor.allocateIntBuffer((int)Tensor.numel(shape));
        intBuffer.put(data);
        return new Tensor_int32(intBuffer, shape);
    }

    public static Tensor fromBlob(float[] data, long[] shape) {
        Tensor.checkArgument(data != null, ERROR_MSG_DATA_ARRAY_NOT_NULL, new Object[0]);
        Tensor.checkArgument(shape != null, ERROR_MSG_SHAPE_NOT_NULL, new Object[0]);
        Tensor.checkShape(shape);
        Tensor.checkShapeAndDataCapacityConsistency(data.length, shape);
        FloatBuffer floatBuffer = Tensor.allocateFloatBuffer((int)Tensor.numel(shape));
        floatBuffer.put(data);
        return new Tensor_float32(floatBuffer, shape);
    }

    public static Tensor fromBlob(long[] data, long[] shape) {
        Tensor.checkArgument(data != null, ERROR_MSG_DATA_ARRAY_NOT_NULL, new Object[0]);
        Tensor.checkArgument(shape != null, ERROR_MSG_SHAPE_NOT_NULL, new Object[0]);
        Tensor.checkShape(shape);
        Tensor.checkShapeAndDataCapacityConsistency(data.length, shape);
        LongBuffer longBuffer = Tensor.allocateLongBuffer((int)Tensor.numel(shape));
        longBuffer.put(data);
        return new Tensor_int64(longBuffer, shape);
    }

    public static Tensor fromBlob(double[] data, long[] shape) {
        Tensor.checkArgument(data != null, ERROR_MSG_DATA_ARRAY_NOT_NULL, new Object[0]);
        Tensor.checkArgument(shape != null, ERROR_MSG_SHAPE_NOT_NULL, new Object[0]);
        Tensor.checkShape(shape);
        Tensor.checkShapeAndDataCapacityConsistency(data.length, shape);
        DoubleBuffer doubleBuffer = Tensor.allocateDoubleBuffer((int)Tensor.numel(shape));
        doubleBuffer.put(data);
        return new Tensor_float64(doubleBuffer, shape);
    }

    public static Tensor fromBlobUnsigned(ByteBuffer data, long[] shape) {
        Tensor.checkArgument(data != null, ERROR_MSG_DATA_BUFFER_NOT_NULL, new Object[0]);
        Tensor.checkArgument(shape != null, ERROR_MSG_SHAPE_NOT_NULL, new Object[0]);
        Tensor.checkShape(shape);
        Tensor.checkShapeAndDataCapacityConsistency(data.capacity(), shape);
        Tensor.checkArgument(data.isDirect(), ERROR_MSG_DATA_BUFFER_MUST_BE_DIRECT, new Object[0]);
        Tensor.checkArgument(data.order() == ByteOrder.nativeOrder(), ERROR_MSG_DATA_BUFFER_MUST_HAVE_NATIVE_BYTE_ORDER, new Object[0]);
        return new Tensor_uint8(data, shape);
    }

    public static Tensor fromBlob(ByteBuffer data, long[] shape) {
        Tensor.checkArgument(data != null, ERROR_MSG_DATA_BUFFER_NOT_NULL, new Object[0]);
        Tensor.checkArgument(shape != null, ERROR_MSG_SHAPE_NOT_NULL, new Object[0]);
        Tensor.checkShape(shape);
        Tensor.checkShapeAndDataCapacityConsistency(data.capacity(), shape);
        Tensor.checkArgument(data.isDirect(), ERROR_MSG_DATA_BUFFER_MUST_BE_DIRECT, new Object[0]);
        Tensor.checkArgument(data.order() == ByteOrder.nativeOrder(), ERROR_MSG_DATA_BUFFER_MUST_HAVE_NATIVE_BYTE_ORDER, new Object[0]);
        return new Tensor_int8(data, shape);
    }

    public static Tensor fromBlob(IntBuffer data, long[] shape) {
        Tensor.checkArgument(data != null, ERROR_MSG_DATA_BUFFER_NOT_NULL, new Object[0]);
        Tensor.checkArgument(shape != null, ERROR_MSG_SHAPE_NOT_NULL, new Object[0]);
        Tensor.checkShape(shape);
        Tensor.checkShapeAndDataCapacityConsistency(data.capacity(), shape);
        Tensor.checkArgument(data.isDirect(), ERROR_MSG_DATA_BUFFER_MUST_BE_DIRECT, new Object[0]);
        Tensor.checkArgument(data.order() == ByteOrder.nativeOrder(), ERROR_MSG_DATA_BUFFER_MUST_HAVE_NATIVE_BYTE_ORDER, new Object[0]);
        return new Tensor_int32(data, shape);
    }

    public static Tensor fromBlob(FloatBuffer data, long[] shape) {
        Tensor.checkArgument(data != null, ERROR_MSG_DATA_BUFFER_NOT_NULL, new Object[0]);
        Tensor.checkArgument(shape != null, ERROR_MSG_SHAPE_NOT_NULL, new Object[0]);
        Tensor.checkShape(shape);
        Tensor.checkShapeAndDataCapacityConsistency(data.capacity(), shape);
        Tensor.checkArgument(data.isDirect(), ERROR_MSG_DATA_BUFFER_MUST_BE_DIRECT, new Object[0]);
        Tensor.checkArgument(data.order() == ByteOrder.nativeOrder(), ERROR_MSG_DATA_BUFFER_MUST_HAVE_NATIVE_BYTE_ORDER, new Object[0]);
        return new Tensor_float32(data, shape);
    }

    public static Tensor fromBlob(LongBuffer data, long[] shape) {
        Tensor.checkArgument(data != null, ERROR_MSG_DATA_BUFFER_NOT_NULL, new Object[0]);
        Tensor.checkArgument(shape != null, ERROR_MSG_SHAPE_NOT_NULL, new Object[0]);
        Tensor.checkShape(shape);
        Tensor.checkShapeAndDataCapacityConsistency(data.capacity(), shape);
        Tensor.checkArgument(data.isDirect(), ERROR_MSG_DATA_BUFFER_MUST_BE_DIRECT, new Object[0]);
        Tensor.checkArgument(data.order() == ByteOrder.nativeOrder(), ERROR_MSG_DATA_BUFFER_MUST_HAVE_NATIVE_BYTE_ORDER, new Object[0]);
        return new Tensor_int64(data, shape);
    }

    public static Tensor fromBlob(DoubleBuffer data, long[] shape) {
        Tensor.checkArgument(data != null, ERROR_MSG_DATA_BUFFER_NOT_NULL, new Object[0]);
        Tensor.checkArgument(shape != null, ERROR_MSG_SHAPE_NOT_NULL, new Object[0]);
        Tensor.checkShape(shape);
        Tensor.checkShapeAndDataCapacityConsistency(data.capacity(), shape);
        Tensor.checkArgument(data.isDirect(), ERROR_MSG_DATA_BUFFER_MUST_BE_DIRECT, new Object[0]);
        Tensor.checkArgument(data.order() == ByteOrder.nativeOrder(), ERROR_MSG_DATA_BUFFER_MUST_HAVE_NATIVE_BYTE_ORDER, new Object[0]);
        return new Tensor_float64(data, shape);
    }

    private Tensor(long[] shape) {
        Tensor.checkShape(shape);
        this.shape = Arrays.copyOf(shape, shape.length);
    }

    public long numel() {
        return Tensor.numel(this.shape);
    }

    public static long numel(long[] shape) {
        Tensor.checkShape(shape);
        int result = 1;
        for (long s : shape) {
            result = (int)((long)result * s);
        }
        return result;
    }

    public long[] shape() {
        return Arrays.copyOf(this.shape, this.shape.length);
    }

    public abstract DType dtype();

    @DoNotStrip
    int dtypeJniCode() {
        return this.dtype().jniCode;
    }

    public byte[] getDataAsByteArray() {
        throw new IllegalStateException("Tensor of type " + this.getClass().getSimpleName() + " cannot return data as byte array.");
    }

    public byte[] getDataAsUnsignedByteArray() {
        throw new IllegalStateException("Tensor of type " + this.getClass().getSimpleName() + " cannot return data as unsigned byte array.");
    }

    public int[] getDataAsIntArray() {
        throw new IllegalStateException("Tensor of type " + this.getClass().getSimpleName() + " cannot return data as int array.");
    }

    public float[] getDataAsFloatArray() {
        throw new IllegalStateException("Tensor of type " + this.getClass().getSimpleName() + " cannot return data as float array.");
    }

    public long[] getDataAsLongArray() {
        throw new IllegalStateException("Tensor of type " + this.getClass().getSimpleName() + " cannot return data as long array.");
    }

    public double[] getDataAsDoubleArray() {
        throw new IllegalStateException("Tensor of type " + this.getClass().getSimpleName() + " cannot return data as double array.");
    }

    @DoNotStrip
    Buffer getRawDataBuffer() {
        throw new IllegalStateException("Tensor of type " + this.getClass().getSimpleName() + " cannot return raw data buffer.");
    }

    private static void checkArgument(boolean expression, String errorMessage, Object ... args) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(Locale.US, errorMessage, args));
        }
    }

    private static void checkShape(long[] shape) {
        Tensor.checkArgument(shape != null, ERROR_MSG_SHAPE_NOT_NULL, new Object[0]);
        for (int i = 0; i < shape.length; ++i) {
            Tensor.checkArgument(shape[i] >= 0L, ERROR_MSG_SHAPE_NON_NEGATIVE, new Object[0]);
        }
    }

    private static void checkShapeAndDataCapacityConsistency(int dataCapacity, long[] shape) {
        long numel = Tensor.numel(shape);
        Tensor.checkArgument(numel == (long)dataCapacity, "Inconsistent data capacity:%d and shape number elements:%d shape:%s", dataCapacity, numel, Arrays.toString(shape));
    }

    @DoNotStrip
    private static Tensor nativeNewTensor(ByteBuffer data, long[] shape, int dtype, HybridData hybridData) {
        Tensor tensor = null;
        tensor = DType.FLOAT.jniCode == dtype ? new Tensor_float32(data.asFloatBuffer(), shape) : (DType.INT32.jniCode == dtype ? new Tensor_int32(data.asIntBuffer(), shape) : (DType.INT64.jniCode == dtype ? new Tensor_int64(data.asLongBuffer(), shape) : (DType.DOUBLE.jniCode == dtype ? new Tensor_float64(data.asDoubleBuffer(), shape) : (DType.UINT8.jniCode == dtype ? new Tensor_uint8(data, shape) : (DType.INT8.jniCode == dtype ? new Tensor_int8(data, shape) : new Tensor_unsupported(data, shape, DType.fromJniCode(dtype)))))));
        tensor.mHybridData = hybridData;
        return tensor;
    }

    public byte[] toByteArray() {
        Tensor thiz;
        int dtypeSize = 0;
        byte[] tensorAsByteArray = null;
        if (this.dtype() == DType.UINT8) {
            dtypeSize = 1;
            tensorAsByteArray = new byte[(int)this.numel()];
            thiz = (Tensor_uint8)this;
            ByteBuffer.wrap(tensorAsByteArray).put(((Tensor_uint8)thiz).getDataAsUnsignedByteArray());
        } else if (this.dtype() == DType.INT8) {
            dtypeSize = 1;
            tensorAsByteArray = new byte[(int)this.numel()];
            thiz = (Tensor_int8)this;
            ByteBuffer.wrap(tensorAsByteArray).put(((Tensor_int8)thiz).getDataAsByteArray());
        } else {
            if (this.dtype() == DType.INT16) {
                throw new IllegalArgumentException("DType.INT16 is not supported in Java so far");
            }
            if (this.dtype() == DType.INT32) {
                dtypeSize = 4;
                tensorAsByteArray = new byte[(int)this.numel() * dtypeSize];
                thiz = (Tensor_int32)this;
                ByteBuffer.wrap(tensorAsByteArray).asIntBuffer().put(((Tensor_int32)thiz).getDataAsIntArray());
            } else if (this.dtype() == DType.INT64) {
                dtypeSize = 8;
                tensorAsByteArray = new byte[(int)this.numel() * dtypeSize];
                thiz = (Tensor_int64)this;
                ByteBuffer.wrap(tensorAsByteArray).asLongBuffer().put(((Tensor_int64)thiz).getDataAsLongArray());
            } else if (this.dtype() == DType.FLOAT) {
                dtypeSize = 4;
                tensorAsByteArray = new byte[(int)this.numel() * dtypeSize];
                thiz = (Tensor_float32)this;
                ByteBuffer.wrap(tensorAsByteArray).asFloatBuffer().put(((Tensor_float32)thiz).getDataAsFloatArray());
            } else if (this.dtype() == DType.DOUBLE) {
                dtypeSize = 8;
                tensorAsByteArray = new byte[(int)this.numel() * dtypeSize];
                thiz = (Tensor_float64)this;
                ByteBuffer.wrap(tensorAsByteArray).asDoubleBuffer().put(((Tensor_float64)thiz).getDataAsDoubleArray());
            } else {
                throw new IllegalArgumentException("Unknown Tensor dtype");
            }
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(2 + 4 * this.shape.length + dtypeSize * (int)this.numel());
        byteBuffer.put((byte)this.dtype().jniCode);
        byteBuffer.put((byte)this.shape.length);
        for (long s : this.shape) {
            byteBuffer.putInt((int)s);
        }
        byteBuffer.put(tensorAsByteArray);
        return byteBuffer.array();
    }

    public static Tensor fromByteArray(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes cannot be null");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        if (!buffer.hasRemaining()) {
            throw new IllegalArgumentException("invalid buffer");
        }
        byte dtype = buffer.get();
        int shapeLength = buffer.get();
        long[] shape = new long[shapeLength];
        long numel = 1L;
        for (int i = 0; i < shapeLength; ++i) {
            int dim = buffer.getInt();
            if (dim < 0) {
                throw new IllegalArgumentException("invalid shape");
            }
            shape[i] = dim;
            numel *= (long)dim;
        }
        if (dtype == DType.UINT8.jniCode) {
            return new Tensor_uint8(buffer, shape);
        }
        if (dtype == DType.INT8.jniCode) {
            return new Tensor_int8(buffer, shape);
        }
        if (dtype == DType.INT32.jniCode) {
            return new Tensor_int32(buffer.asIntBuffer(), shape);
        }
        if (dtype == DType.INT64.jniCode) {
            return new Tensor_int64(buffer.asLongBuffer(), shape);
        }
        if (dtype == DType.FLOAT.jniCode) {
            return new Tensor_float32(buffer.asFloatBuffer(), shape);
        }
        if (dtype == DType.DOUBLE.jniCode) {
            return new Tensor_float64(buffer.asDoubleBuffer(), shape);
        }
        throw new IllegalArgumentException("Unknown Tensor dtype");
    }

    static class Tensor_uint8
    extends Tensor {
        private final ByteBuffer data;

        private Tensor_uint8(ByteBuffer data, long[] shape) {
            super(shape);
            this.data = data;
        }

        @Override
        public DType dtype() {
            return DType.UINT8;
        }

        @Override
        Buffer getRawDataBuffer() {
            return this.data;
        }

        @Override
        public byte[] getDataAsUnsignedByteArray() {
            this.data.rewind();
            byte[] arr = new byte[this.data.remaining()];
            this.data.get(arr);
            return arr;
        }

        public String toString() {
            return String.format("Tensor(%s, dtype=torch.uint8)", Arrays.toString(this.shape));
        }
    }

    static class Tensor_int8
    extends Tensor {
        private final ByteBuffer data;

        private Tensor_int8(ByteBuffer data, long[] shape) {
            super(shape);
            this.data = data;
        }

        @Override
        public DType dtype() {
            return DType.INT8;
        }

        @Override
        Buffer getRawDataBuffer() {
            return this.data;
        }

        @Override
        public byte[] getDataAsByteArray() {
            this.data.rewind();
            byte[] arr = new byte[this.data.remaining()];
            this.data.get(arr);
            return arr;
        }

        public String toString() {
            return String.format("Tensor(%s, dtype=torch.int8)", Arrays.toString(this.shape));
        }
    }

    static class Tensor_int32
    extends Tensor {
        private final IntBuffer data;

        private Tensor_int32(IntBuffer data, long[] shape) {
            super(shape);
            this.data = data;
        }

        @Override
        public DType dtype() {
            return DType.INT32;
        }

        @Override
        Buffer getRawDataBuffer() {
            return this.data;
        }

        @Override
        public int[] getDataAsIntArray() {
            this.data.rewind();
            int[] arr = new int[this.data.remaining()];
            this.data.get(arr);
            return arr;
        }

        public String toString() {
            return String.format("Tensor(%s, dtype=torch.int32)", Arrays.toString(this.shape));
        }
    }

    static class Tensor_float32
    extends Tensor {
        private final FloatBuffer data;

        Tensor_float32(FloatBuffer data, long[] shape) {
            super(shape);
            this.data = data;
        }

        @Override
        public float[] getDataAsFloatArray() {
            this.data.rewind();
            float[] arr = new float[this.data.remaining()];
            this.data.get(arr);
            return arr;
        }

        @Override
        public DType dtype() {
            return DType.FLOAT;
        }

        @Override
        Buffer getRawDataBuffer() {
            return this.data;
        }

        public String toString() {
            return String.format("Tensor(%s, dtype=torch.float32)", Arrays.toString(this.shape));
        }
    }

    static class Tensor_int64
    extends Tensor {
        private final LongBuffer data;

        private Tensor_int64(LongBuffer data, long[] shape) {
            super(shape);
            this.data = data;
        }

        @Override
        public DType dtype() {
            return DType.INT64;
        }

        @Override
        Buffer getRawDataBuffer() {
            return this.data;
        }

        @Override
        public long[] getDataAsLongArray() {
            this.data.rewind();
            long[] arr = new long[this.data.remaining()];
            this.data.get(arr);
            return arr;
        }

        public String toString() {
            return String.format("Tensor(%s, dtype=torch.int64)", Arrays.toString(this.shape));
        }
    }

    static class Tensor_float64
    extends Tensor {
        private final DoubleBuffer data;

        private Tensor_float64(DoubleBuffer data, long[] shape) {
            super(shape);
            this.data = data;
        }

        @Override
        public DType dtype() {
            return DType.DOUBLE;
        }

        @Override
        Buffer getRawDataBuffer() {
            return this.data;
        }

        @Override
        public double[] getDataAsDoubleArray() {
            this.data.rewind();
            double[] arr = new double[this.data.remaining()];
            this.data.get(arr);
            return arr;
        }

        public String toString() {
            return String.format("Tensor(%s, dtype=torch.float64)", Arrays.toString(this.shape));
        }
    }

    static class Tensor_unsupported
    extends Tensor {
        private final ByteBuffer data;
        private final DType mDtype;

        private Tensor_unsupported(ByteBuffer data, long[] shape, DType dtype) {
            super(shape);
            this.data = data;
            this.mDtype = dtype;
            Log.e((String)"ExecuTorch", (String)(this.toString() + " in Java. Please consider re-export the model with proper return type"));
        }

        @Override
        public DType dtype() {
            return this.mDtype;
        }

        public String toString() {
            return String.format("Unsupported tensor(%s, dtype=%d)", new Object[]{Arrays.toString(this.shape), this.mDtype});
        }
    }
}
