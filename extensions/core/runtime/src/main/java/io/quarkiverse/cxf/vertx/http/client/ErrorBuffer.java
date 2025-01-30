package io.quarkiverse.cxf.vertx.http.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.function.Supplier;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * A {@link Buffer} implementation throwing a passed in {@link Throwable} on any method invocation.
 * It is not backed by a Netty buffer so that it can be used in a static initializer without any complains from
 * GraalVM.
 */
class ErrorBuffer implements Buffer {

    private final Supplier<Throwable> exceptionSupplier;

    public ErrorBuffer() {
        this.exceptionSupplier = UnsupportedOperationException::new;
    }

    public ErrorBuffer(Throwable exception) {
        super();
        this.exceptionSupplier = () -> exception;
    }

    RuntimeException runtimeException() {
        final Throwable e = exceptionSupplier.get();
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        } else {
            return new RuntimeException(e);
        }
    }

    void throwIOExceptionIfNeeded() throws IOException {
        final Throwable e = exceptionSupplier.get();
        if (e instanceof IOException) {
            throw (IOException) e;
        } else if (e != null) {
            throw new IOException(e);
        }
    }

    @Override
    public void writeToBuffer(Buffer buffer) {
        throw runtimeException();
    }

    @Override
    public int readFromBuffer(int pos, Buffer buffer) {
        throw runtimeException();
    }

    @Override
    public String toString(Charset enc) {
        throw runtimeException();
    }

    @Override
    public String toString(String enc) {
        throw runtimeException();
    }

    @Override
    public JsonObject toJsonObject() {
        throw runtimeException();
    }

    @Override
    public JsonArray toJsonArray() {
        throw runtimeException();
    }

    @Override
    public Buffer slice(int start, int end) {
        throw runtimeException();
    }

    @Override
    public Buffer slice() {
        throw runtimeException();
    }

    @Override
    public Buffer setUnsignedShortLE(int pos, int s) {
        throw runtimeException();
    }

    @Override
    public Buffer setUnsignedShort(int pos, int s) {
        throw runtimeException();
    }

    @Override
    public Buffer setUnsignedIntLE(int pos, long i) {
        throw runtimeException();
    }

    @Override
    public Buffer setUnsignedInt(int pos, long i) {
        throw runtimeException();
    }

    @Override
    public Buffer setUnsignedByte(int pos, short b) {
        throw runtimeException();
    }

    @Override
    public Buffer setString(int pos, String str, String enc) {
        throw runtimeException();
    }

    @Override
    public Buffer setString(int pos, String str) {
        throw runtimeException();
    }

    @Override
    public Buffer setShortLE(int pos, short s) {
        throw runtimeException();
    }

    @Override
    public Buffer setShort(int pos, short s) {
        throw runtimeException();
    }

    @Override
    public Buffer setMediumLE(int pos, int i) {
        throw runtimeException();
    }

    @Override
    public Buffer setMedium(int pos, int i) {
        throw runtimeException();
    }

    @Override
    public Buffer setLongLE(int pos, long l) {
        throw runtimeException();
    }

    @Override
    public Buffer setLong(int pos, long l) {
        throw runtimeException();
    }

    @Override
    public Buffer setIntLE(int pos, int i) {
        throw runtimeException();
    }

    @Override
    public Buffer setInt(int pos, int i) {
        throw runtimeException();
    }

    @Override
    public Buffer setFloat(int pos, float f) {
        throw runtimeException();
    }

    @Override
    public Buffer setDouble(int pos, double d) {
        throw runtimeException();
    }

    @Override
    public Buffer setBytes(int pos, byte[] b, int offset, int len) {
        throw runtimeException();
    }

    @Override
    public Buffer setBytes(int pos, byte[] b) {
        throw runtimeException();
    }

    @Override
    public Buffer setBytes(int pos, ByteBuffer b) {
        throw runtimeException();
    }

    @Override
    public Buffer setByte(int pos, byte b) {
        throw runtimeException();
    }

    @Override
    public Buffer setBuffer(int pos, Buffer b, int offset, int len) {
        throw runtimeException();
    }

    @Override
    public Buffer setBuffer(int pos, Buffer b) {
        throw runtimeException();
    }

    @Override
    public int length() {
        throw runtimeException();
    }

    @Override
    public int getUnsignedShortLE(int pos) {
        throw runtimeException();
    }

    @Override
    public int getUnsignedShort(int pos) {
        throw runtimeException();
    }

    @Override
    public int getUnsignedMediumLE(int pos) {
        throw runtimeException();
    }

    @Override
    public int getUnsignedMedium(int pos) {
        throw runtimeException();
    }

    @Override
    public long getUnsignedIntLE(int pos) {
        throw runtimeException();
    }

    @Override
    public long getUnsignedInt(int pos) {
        throw runtimeException();
    }

    @Override
    public short getUnsignedByte(int pos) {
        throw runtimeException();
    }

    @Override
    public String getString(int start, int end, String enc) {
        throw runtimeException();
    }

    @Override
    public String getString(int start, int end) {
        throw runtimeException();
    }

    @Override
    public short getShortLE(int pos) {
        throw runtimeException();
    }

    @Override
    public short getShort(int pos) {
        throw runtimeException();
    }

    @Override
    public int getMediumLE(int pos) {
        throw runtimeException();
    }

    @Override
    public int getMedium(int pos) {
        throw runtimeException();
    }

    @Override
    public long getLongLE(int pos) {
        throw runtimeException();
    }

    @Override
    public long getLong(int pos) {
        throw runtimeException();
    }

    @Override
    public int getIntLE(int pos) {
        throw runtimeException();
    }

    @Override
    public int getInt(int pos) {
        throw runtimeException();
    }

    @Override
    public float getFloat(int pos) {
        throw runtimeException();
    }

    @Override
    public double getDouble(int pos) {
        throw runtimeException();
    }

    @Override
    public Buffer getBytes(int start, int end, byte[] dst, int dstIndex) {
        throw runtimeException();
    }

    @Override
    public Buffer getBytes(int start, int end, byte[] dst) {
        throw runtimeException();
    }

    @Override
    public Buffer getBytes(byte[] dst, int dstIndex) {
        throw runtimeException();
    }

    @Override
    public byte[] getBytes(int start, int end) {
        throw runtimeException();
    }

    @Override
    public Buffer getBytes(byte[] dst) {
        throw runtimeException();
    }

    @Override
    public byte[] getBytes() {
        throw runtimeException();
    }

    @Override
    public ByteBuf getByteBuf() {
        throw runtimeException();
    }

    @Override
    public byte getByte(int pos) {
        throw runtimeException();
    }

    @Override
    public Buffer getBuffer(int start, int end) {
        throw runtimeException();
    }

    @Override
    public Buffer copy() {
        throw runtimeException();
    }

    @Override
    public Buffer appendUnsignedShortLE(int s) {
        throw runtimeException();
    }

    @Override
    public Buffer appendUnsignedShort(int s) {
        throw runtimeException();
    }

    @Override
    public Buffer appendUnsignedIntLE(long i) {
        throw runtimeException();
    }

    @Override
    public Buffer appendUnsignedInt(long i) {
        throw runtimeException();
    }

    @Override
    public Buffer appendUnsignedByte(short b) {
        throw runtimeException();
    }

    @Override
    public Buffer appendString(String str, String enc) {
        throw runtimeException();
    }

    @Override
    public Buffer appendString(String str) {
        throw runtimeException();
    }

    @Override
    public Buffer appendShortLE(short s) {
        throw runtimeException();
    }

    @Override
    public Buffer appendShort(short s) {
        throw runtimeException();
    }

    @Override
    public Buffer appendMediumLE(int i) {
        throw runtimeException();
    }

    @Override
    public Buffer appendMedium(int i) {
        throw runtimeException();
    }

    @Override
    public Buffer appendLongLE(long l) {
        throw runtimeException();
    }

    @Override
    public Buffer appendLong(long l) {
        throw runtimeException();
    }

    @Override
    public Buffer appendIntLE(int i) {
        throw runtimeException();
    }

    @Override
    public Buffer appendInt(int i) {
        throw runtimeException();
    }

    @Override
    public Buffer appendFloat(float f) {
        throw runtimeException();
    }

    @Override
    public Buffer appendDouble(double d) {
        throw runtimeException();
    }

    @Override
    public Buffer appendBytes(byte[] bytes, int offset, int len) {
        throw runtimeException();
    }

    @Override
    public Buffer appendBytes(byte[] bytes) {
        throw runtimeException();
    }

    @Override
    public Buffer appendByte(byte b) {
        throw runtimeException();
    }

    @Override
    public Buffer appendBuffer(Buffer buff, int offset, int len) {
        throw runtimeException();
    }

    @Override
    public Buffer appendBuffer(Buffer buff) {
        throw runtimeException();
    }
}