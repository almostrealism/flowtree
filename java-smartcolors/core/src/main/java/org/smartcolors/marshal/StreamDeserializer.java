package org.smartcolors.marshal;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by devrandom on 2014-Nov-17.
 */
public class StreamDeserializer implements Deserializer {
    public static final int MAX_BYTES = 1024 * 1024;
    protected final InputStream is;

    public StreamDeserializer(InputStream is) {
        this.is = is;
    }

    @Override
    public long readVarulong() throws SerializationException {
        long value = 0;
        int shift = 0;
        while (true) {
            long b = 0;
            try {
                b = is.read();
                if (b < 0)
                    break;
            } catch (IOException e) {
                throw new SerializationException(e);
            }
            value |= (b & 0x7f) << shift;
            if ((b & 0x80) == 0)
                break;
            shift += 7;
        }
        return value;
    }

    @Override
    public int readVaruint() throws SerializationException {
        long value = readVarulong();
        if (value > Integer.MAX_VALUE || value < 0)
            throw new SerializationException("invalid int " + value);
        return (int) value;
    }

    @Override
    public byte[] readBytes(int expectedLength) throws SerializationException {
        byte[] buf = new byte[expectedLength];
        try {
            is.read(buf);
        } catch (IOException e) {
            throw new SerializationException(e);
        }
        return buf;
    }

    @Override
    public <T> T readObjectHeader() throws SerializationException {
        return null;
    }

    @Override
    public byte[] readBytes() throws SerializationException {
        long length = readVarulong();
        if (length > MAX_BYTES || length < 0)
            throw new RuntimeException("bytes longer than max");
        byte[] buf = new byte[(int) length];
        try {
            if (length > 0) {
                int len = is.read(buf);
                if (len < buf.length)
                    throw new SerializationException("short read");
            }
        } catch (IOException e) {
            throw new SerializationException(e);
        }
        return buf;
    }

    public <T> T readObject(ObjectReader<T> reader) throws SerializationException {
        return reader.readObject(this);
    }

    @Override
    public <T> void afterReadObject(T obj) throws SerializationException {
    }
}