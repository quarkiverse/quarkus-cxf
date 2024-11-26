package io.quarkiverse.cxf.vertx.http.client;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * A buffer not backed by a Netty buffer so that it can be used in a static initializer without any complains from
 * GraalVM
 */
class DummyBuffer implements Buffer {

    @Override
    public void writeToBuffer(Buffer buffer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readFromBuffer(int pos, Buffer buffer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString(Charset enc) {
        return "";
    }

    @Override
    public String toString(String enc) {
        return "";
    }

    @Override
    public JsonObject toJsonObject() {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonArray toJsonArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer slice(int start, int end) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer slice() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setUnsignedShortLE(int pos, int s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setUnsignedShort(int pos, int s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setUnsignedIntLE(int pos, long i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setUnsignedInt(int pos, long i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setUnsignedByte(int pos, short b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setString(int pos, String str, String enc) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setString(int pos, String str) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setShortLE(int pos, short s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setShort(int pos, short s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setMediumLE(int pos, int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setMedium(int pos, int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setLongLE(int pos, long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setLong(int pos, long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setIntLE(int pos, int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setInt(int pos, int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setFloat(int pos, float f) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setDouble(int pos, double d) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setBytes(int pos, byte[] b, int offset, int len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setBytes(int pos, byte[] b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setBytes(int pos, ByteBuffer b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setByte(int pos, byte b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setBuffer(int pos, Buffer b, int offset, int len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer setBuffer(int pos, Buffer b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public int getUnsignedShortLE(int pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getUnsignedShort(int pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getUnsignedMediumLE(int pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getUnsignedMedium(int pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getUnsignedIntLE(int pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getUnsignedInt(int pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public short getUnsignedByte(int pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getString(int start, int end, String enc) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getString(int start, int end) {
        throw new UnsupportedOperationException();
    }

    @Override
    public short getShortLE(int pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public short getShort(int pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMediumLE(int pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMedium(int pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLongLE(int pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLong(int pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getIntLE(int pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getInt(int pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getFloat(int pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getDouble(int pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer getBytes(int start, int end, byte[] dst, int dstIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer getBytes(int start, int end, byte[] dst) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer getBytes(byte[] dst, int dstIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getBytes(int start, int end) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer getBytes(byte[] dst) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getBytes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuf getByteBuf() {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte getByte(int pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer getBuffer(int start, int end) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer copy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendUnsignedShortLE(int s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendUnsignedShort(int s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendUnsignedIntLE(long i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendUnsignedInt(long i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendUnsignedByte(short b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendString(String str, String enc) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendString(String str) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendShortLE(short s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendShort(short s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendMediumLE(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendMedium(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendLongLE(long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendLong(long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendIntLE(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendInt(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendFloat(float f) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendDouble(double d) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendBytes(byte[] bytes, int offset, int len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendBytes(byte[] bytes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendByte(byte b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendBuffer(Buffer buff, int offset, int len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Buffer appendBuffer(Buffer buff) {
        throw new UnsupportedOperationException();
    }
}