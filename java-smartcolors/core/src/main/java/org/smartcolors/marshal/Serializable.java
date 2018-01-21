package org.smartcolors.marshal;

import com.google.common.hash.HashCode;

/**
 * Created by devrandom on 2014-Nov-17.
 */
public interface Serializable {
    void serialize(Serializer serializer) throws SerializationException;

    HashCode getHash();

    byte[] getHmacKey();
}
