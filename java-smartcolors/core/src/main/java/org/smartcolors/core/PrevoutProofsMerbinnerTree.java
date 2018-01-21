package org.smartcolors.core;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.Utils;
import org.smartcolors.marshal.*;

import java.util.*;

import static com.google.common.base.Preconditions.checkState;

/**
 * Created by devrandom on 2014-Nov-18.
 */
public class PrevoutProofsMerbinnerTree extends MerbinnerTree<TransactionOutPoint, ColorProof> implements IterativeSerializable {
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
    public void serializeValue(Serializer ser, ColorProof value) throws SerializationException {
        ser.write(value);
    }

    @Override
    public long getSum(ColorProof value) {
        return value.quantity;
    }

    @Override
    public HashCode getKeyHash(TransactionOutPoint key) {
        return Hashes.calcHash(key);
    }

    @Override
    protected void deserializeNode(Deserializer des) throws SerializationException {
        TransactionOutPoint key = deserializeKey(des);
        ColorProof value = des.readObject(new Deserializer.ObjectReader<ColorProof>() {
            @Override
            public ColorProof readObject(Deserializer des) throws SerializationException {
                return ColorProof.deserialize(params, des);
            }
        });
        entries.put(key, value);
    }

    private TransactionOutPoint deserializeKey(Deserializer des) throws SerializationException {
        return des.readObject(new Deserializer.ObjectReader<TransactionOutPoint>() {
            @Override
            public TransactionOutPoint readObject(Deserializer des) throws SerializationException {
                return new TransactionOutPoint(params, des.readBytes(36), 0);
            }
        });
    }

    @Override
    protected void serializeSum(Serializer ser, long sum) throws SerializationException {
        ser.write(sum);
    }

    @Override
    public byte[] getHmacKey() {
        return Utils.HEX.decode("486a3b9f0cc1adc7f0f7f3e388b89dbc");
    }

    public PrevoutProofsMerbinnerTree(NetworkParameters params, Map<TransactionOutPoint, ColorProof> nodes) {
        super(nodes);
        this.params = params;
    }

    public PrevoutProofsMerbinnerTree(NetworkParameters params) {
        super(Maps.<TransactionOutPoint, ColorProof>newHashMap());
        this.params = params;
    }

    @Override
    public void serialize(final Serializer ser, Deque<SerializationState> stack) throws SerializationException {
        SerializationState state = stack.getFirst();

        Collection<TransactionOutPoint> keys = state.keys;
        if (keys == null)
            keys = keySet();

        if (keys.isEmpty()) {
            ser.write(0);
        } else if (keys.size() == 1) {
            ser.write(1);
            TransactionOutPoint key = keys.iterator().next();
            serializeKey(ser, key);
            ColorProof colorProof = entries.get(key);
            if (colorProof instanceof TransferColorProof) {
                stack.push(new SerializationState((TransferColorProof) colorProof, null, 0));
            } else {
                serializeValue(ser, colorProof);
            }
        } else {
            ser.write(2);
            Set<TransactionOutPoint> left = Sets.newHashSet();
            Set<TransactionOutPoint> right = Sets.newHashSet();
            for (TransactionOutPoint key : keys) {
                byte[] keyHash = getKeyHash(key).asBytes();
                boolean side = ((keyHash[state.depth / 8] >> (7 - (state.depth % 8))) & 1) == 1;
                if (side)
                    left.add(key);
                else
                    right.add(key);
            }
            stack.push(new SerializationState(this, right, state.depth + 1));
            stack.push(new SerializationState(this, left, state.depth + 1));
        }
    }

    @Override
    public void deserialize(Deserializer des, Deque<DeserializationState> stack) throws SerializationException {
        int type = (int) des.readVarulong();
        if (type == 0) {
        } else if (type == 1) {
            final TransactionOutPoint key = deserializeKey(des);
            ColorProof proof = ColorProof.deserialize(params, des, stack);
            entries.put(key, proof);
        } else {
            checkState(type == 2);
            stack.push(new DeserializationState(this));
            stack.push(new DeserializationState(this));
        }
    }
}
