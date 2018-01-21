package org.smartcolors.marshal;

/**
 * Created by devrandom on 2014-Nov-17.
 */
public interface Deserializer {
    long readVarulong() throws SerializationException;

    int readVaruint() throws SerializationException;

    byte[] readBytes() throws SerializationException;

    byte[] readBytes(int expectedLength) throws SerializationException;

    /** Read the object header (if any) and return any object-by-reference */
    <T> T readObjectHeader() throws SerializationException;

    interface ObjectReader<T> {
        T readObject(Deserializer des) throws SerializationException;
    }

    <T> T readObject(ObjectReader<T> reader) throws SerializationException;

    <T> void afterReadObject(T obj) throws SerializationException;
}
