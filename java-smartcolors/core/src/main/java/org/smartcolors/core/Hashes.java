package org.smartcolors.core;

import com.google.common.base.Throwables;
import com.google.common.hash.HashCode;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.Utils;
import org.bitcoinj.script.Script;
import org.smartcolors.marshal.BytesSerializer;
import org.smartcolors.marshal.HashSerializer;
import org.smartcolors.marshal.SerializationException;

/**
 * Created by devrandom on 2014-Nov-19.
 */
public class Hashes {

    public static final byte[] SCRIPT_HMAC_KEY = Utils.HEX.decode("3b808252881682adf56f7cc5abc0cb3c");
    public static final byte[] OUT_POINT_HMAC_KEY = Utils.HEX.decode("eac9aef052700336a94accea6a883e59");
    public static final byte[] TRANSACTION_HMAC_KEY = Utils.HEX.decode("4668df91fe332d65378cc758958d701d");

    public static HashCode calcHash(Script script) {
        BytesSerializer ser = new BytesSerializer();
        try {
            ser.writeWithLength(script.getProgram());
        } catch (SerializationException e) {
            Throwables.propagate(e);
        }
        return HashSerializer.calcHash(ser.getBytes(), SCRIPT_HMAC_KEY);
    }

    public static HashCode calcHash(TransactionOutPoint key) {
        return HashSerializer.calcHash(key.bitcoinSerialize(), OUT_POINT_HMAC_KEY);
    }

    public static HashCode calcHash(Transaction tx) {
        BytesSerializer ser = new BytesSerializer();
        try {
            ser.writeWithLength(tx.bitcoinSerialize());
        } catch (SerializationException e) {
            Throwables.propagate(e);
        }
        return HashSerializer.calcHash(ser.getBytes(), TRANSACTION_HMAC_KEY);
    }
}
