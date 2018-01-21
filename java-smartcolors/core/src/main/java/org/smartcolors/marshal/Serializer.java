package org.smartcolors.marshal;

/**
 * Created by devrandom on 2014-Nov-17.
 */
public interface Serializer {
    void write(long value) throws SerializationException;

    void write(byte[] bytes) throws SerializationException;

    void writeWithLength(byte[] bytes) throws SerializationException;

    void write(Serializable obj) throws SerializationException;

    void write(Object obj, SerializerHelper helper) throws SerializationException;
}
