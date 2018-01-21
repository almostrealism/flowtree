package org.smartcolors.marshal;

import com.google.common.base.Throwables;
import com.google.common.collect.Queues;
import com.google.common.hash.HashCode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;

/**
 * Created by devrandom on 2014-Nov-17.
 */
public class HashSerializer extends BytesSerializer {
    @Override
    public void write(Serializable obj) throws SerializationException {
        if (obj instanceof IterativeSerializable) {
            DummySerializer dummy = new DummySerializer();
            // Switch to iterative serialization
            ArrayDeque<SerializationState> stack = Queues.newArrayDeque();
            IterativeSerializable tree = (IterativeSerializable) obj;
            stack.push(new SerializationState(tree, null, 0));
            while (!stack.isEmpty()) {
                SerializationState state = stack.getFirst();
                if (state.isDone) {
                    if (state.serializable instanceof HashableSerializable) {
                        ((HashableSerializable) state.serializable).getHash();
                    }
                    stack.pop();
                } else {
                    state.serializable.serialize(dummy, stack);
                    state.isDone = true;
                }
            }
        }
        write(obj.getHash().asBytes());
    }

    @Override
    public void write(Object obj, SerializerHelper helper) throws SerializationException {
        write(helper.getHash(obj).asBytes());
    }

    public static HashCode calcHash(BytesSerializer serializer, byte[] hmacKey) {
        Mac hmac = null;
        try {
            hmac = Mac.getInstance("HmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            Throwables.propagate(e);
        }
        SecretKeySpec macKey = new SecretKeySpec(hmacKey, "RAW");
        try {
            hmac.init(macKey);
        } catch (InvalidKeyException e) {
            Throwables.propagate(e);
        }
        return HashCode.fromBytes(hmac.doFinal(serializer.getBytes()));
    }

    public static HashCode calcHash(byte[] content, byte[] hmacKey) {
        Mac hmac = null;
        try {
            hmac = Mac.getInstance("HmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            Throwables.propagate(e);
        }
        SecretKeySpec macKey = new SecretKeySpec(hmacKey, "RAW");
        try {
            hmac.init(macKey);
        } catch (InvalidKeyException e) {
            Throwables.propagate(e);
        }
        return HashCode.fromBytes(hmac.doFinal(content));
    }
}
