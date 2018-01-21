package org.smartcolors.core;

import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.Utils;
import org.smartcolors.marshal.*;

import java.util.Map;

/**
 * Created by devrandom on 2014-Nov-18.
 */
public class GenesisOutPointsMerbinnerTree extends MerbinnerTree<TransactionOutPoint, Long> {
    private NetworkParameters params;

    @Override
    public void serializeKey(Serializer ser, TransactionOutPoint key) throws SerializationException {
        ser.write(key, new SerializerHelper<TransactionOutPoint>() {
            @Override
            public void serialize(Serializer ser, TransactionOutPoint obj) throws SerializationException {
                ser.write(obj.bitcoinSerialize());
            }

            @Override
            public HashCode getHash(TransactionOutPoint obj) {
                return Hashes.calcHash(obj);
            }
        });
    }

    @Override
    public void serializeValue(Serializer ser, Long value) throws SerializationException {
        ser.write(value);
    }

    @Override
    public long getSum(Long value) {
        return value;
    }

    @Override
    public HashCode getKeyHash(TransactionOutPoint key) {
        return Hashes.calcHash(key);
    }

    @Override
    protected void deserializeNode(Deserializer des) throws SerializationException {
        TransactionOutPoint key = des.readObject(new Deserializer.ObjectReader<TransactionOutPoint>() {
            @Override
            public TransactionOutPoint readObject(Deserializer des) throws SerializationException {
                return new TransactionOutPoint(params, des.readBytes(36), 0);
            }
        });
        long value = des.readVarulong();
        entries.put(key, value);
    }

    @Override
    protected void serializeSum(Serializer ser, long sum) throws SerializationException {
        ser.write(sum);
    }

    @Override
    public byte[] getHmacKey() {
        return Utils.HEX.decode("d8497e1258c3f8e747341cb361676cee");
    }

    public GenesisOutPointsMerbinnerTree(NetworkParameters params, Map<TransactionOutPoint, Long> nodes) {
        super(nodes);
        this.params = params;
    }

    public GenesisOutPointsMerbinnerTree(NetworkParameters params) {
        super(Maps.<TransactionOutPoint, Long>newHashMap());
        this.params = params;
    }
}
