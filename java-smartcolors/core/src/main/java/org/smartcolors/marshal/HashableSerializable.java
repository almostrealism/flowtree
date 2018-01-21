package org.smartcolors.marshal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Throwables;
import com.google.common.hash.HashCode;

/**
 * Created by devrandom on 2014-Nov-17.
 */
public abstract class HashableSerializable implements Serializable {
    private HashCode cachedHash;

    @JsonIgnore
    public HashCode getHash() {
        if (cachedHash != null)
            return cachedHash;
        HashSerializer serializer = new HashSerializer();
        try {
            serialize(serializer);
        } catch (SerializationException e) {
            Throwables.propagate(e);
        }
        cachedHash = HashSerializer.calcHash(serializer, getHmacKey());
        return cachedHash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HashableSerializable))
            return false;
        return getHash().equals(((HashableSerializable) obj).getHash());
    }

    @Override
    public int hashCode() {
        return getHash().hashCode();
    }
}
