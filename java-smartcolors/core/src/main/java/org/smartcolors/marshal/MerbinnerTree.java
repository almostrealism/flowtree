package org.smartcolors.marshal;

import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by devrandom on 2014-Nov-17.
 */
public abstract class MerbinnerTree<K, V> extends HashableSerializable {
    protected Map<K, V> entries;

    public MerbinnerTree(Map<K, V> entries) {
        this.entries = entries;
    }

    public MerbinnerTree() {
    }

    public V get(K key) {
        return entries.get(key);
    }

    public boolean containsKey(K key) {
        return entries.containsKey(key);
    }

    public abstract void serializeKey(Serializer ser, K key) throws SerializationException;

    public abstract void serializeValue(Serializer ser, V value) throws SerializationException;

    public abstract long getSum(V value);

    public abstract com.google.common.hash.HashCode getKeyHash(K key);

    public Set<K> keySet() {
        return entries.keySet();
    }


    public Collection<V> values() {
        return entries.values();
    }

    @Override
    public void serialize(final Serializer ser) throws SerializationException {
        serialize(ser, entries.keySet(), 0);
    }

    private long serialize(Serializer ser, Collection<K> keys, int depth) throws SerializationException {
        if (keys.isEmpty()) {
            ser.write(0);
            return 0;
        } else if (keys.size() == 1) {
            ser.write(1);
            K key = keys.iterator().next();
            serializeKey(ser, key);
            serializeValue(ser, entries.get(key));
            return getSum(entries.get(key));
        } else {
            ser.write(2);
            Set<K> left = Sets.newHashSet();
            Set<K> right = Sets.newHashSet();
            for (K key : keys) {
                byte[] keyHash = getKeyHash(key).asBytes();
                boolean side = ((keyHash[depth / 8] >> (7 - (depth % 8))) & 1) == 1;
                if (side)
                    left.add(key);
                else
                    right.add(key);
            }
            long leftSum = subSerialize(ser, left, depth + 1);
            long rightSum = subSerialize(ser, right, depth + 1);
            return doSum(leftSum, rightSum);
        }
    }

    private long subSerialize(Serializer ser, Set<K> keys, int depth) throws SerializationException {
        if (ser instanceof HashSerializer) {
            HashSerializer ser1 = new HashSerializer();
            long sum = serialize(ser1, keys, depth);
            HashCode hash = HashSerializer.calcHash(ser1, getHmacKey());
            ser.write(hash.asBytes());
            serializeSum(ser, sum);
            return sum;
        } else {
            return serialize(ser, keys, depth);
        }
    }

    protected void serializeSum(Serializer ser, long sum) throws SerializationException {
    }

    public void deserialize(Deserializer des) throws SerializationException {
        long type = des.readVarulong();
        //noinspection StatementWithEmptyBody
        if (type == 0)
            ; // nothing
        else if (type == 1) {
            deserializeNode(des);
        } else if (type == 2) {
            deserialize(des); // left
            deserialize(des); // right
        } else {
            throw new SerializationException("unknown Merbinner node type " + type);
        }
    }

    protected abstract void deserializeNode(Deserializer des) throws SerializationException;

    private long doSum(long leftSum, long rightSum) {
        return leftSum + rightSum;
    }

    @Override
    public String toString() {
        return getHash().toString();
    }
}
