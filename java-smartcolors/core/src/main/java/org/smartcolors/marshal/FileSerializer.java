package org.smartcolors.marshal;

import com.google.common.hash.HashCode;
import org.bitcoinj.core.Utils;

import java.util.Arrays;

import static com.google.common.base.Preconditions.checkState;

/**
 * Created by devrandom on 2014-Nov-19.
 */
public abstract class FileSerializer {

    public static final int VERSION = 0;

    public void write(MemoizedSerializer ser, Serializable obj) throws SerializationException {
        ser.write(getMagic());
        ser.write(new byte[]{VERSION});
        ser.write(obj);
        byte[] hash = obj.getHash().asBytes();
        checkState(hash.length == 32);
        ser.write(hash);
    }

    public void readHeader(MemoizedDeserializer des) throws SerializationException {
        byte[] expectedMagic = getMagic();
        byte[] magic = des.readBytes(expectedMagic.length);
        if (!Arrays.equals(expectedMagic, magic))
            throw new SerializationException("wrong magic " + Utils.HEX.encode(magic));
        long version = des.readVarulong();
        if (version != VERSION)
            throw new SerializationException("wrong version " + version);
    }

    public void verifyHash(MemoizedDeserializer des, Serializable obj) throws SerializationException {
        HashCode hash = obj.getHash();
        byte[] expectedHash = des.readBytes(32);
        if (!hash.equals(HashCode.fromBytes(expectedHash)))
            throw new SerializationException("hash mismatch");
    }

    protected abstract byte[] getMagic();
}
