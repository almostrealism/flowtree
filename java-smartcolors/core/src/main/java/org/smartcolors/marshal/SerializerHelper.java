package org.smartcolors.marshal;

import com.google.common.hash.HashCode;

/**
 * Created by devrandom on 2014-Nov-19.
 */
public interface SerializerHelper<T> {
    void serialize(Serializer ser, T obj) throws SerializationException;

    HashCode getHash(T obj);
}
