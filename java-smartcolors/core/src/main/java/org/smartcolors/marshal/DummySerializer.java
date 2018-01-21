package org.smartcolors.marshal;

/**
 * Created by devrandom on 2015-07-19.
 */
public class DummySerializer implements Serializer {
    @Override
    public void write(long value) throws SerializationException {

    }

    @Override
    public void write(byte[] bytes) throws SerializationException {

    }

    @Override
    public void writeWithLength(byte[] bytes) throws SerializationException {

    }

    @Override
    public void write(Serializable obj) throws SerializationException {

    }

    @Override
    public void write(Object obj, SerializerHelper helper) throws SerializationException {

    }
}
