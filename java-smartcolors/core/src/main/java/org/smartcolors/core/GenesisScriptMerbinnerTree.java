package org.smartcolors.core;

import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Utils;
import org.bitcoinj.script.Script;
import org.smartcolors.marshal.*;

import java.util.Map;

/**
 * Created by devrandom on 2014-Nov-18.
 */
public class GenesisScriptMerbinnerTree extends MerbinnerTree<Script, Void> {
    @Override
    public void serializeKey(Serializer ser, Script key) throws SerializationException {
        ser.write(key, new SerializerHelper<Script>() {
            @Override
            public void serialize(Serializer ser, Script obj) throws SerializationException {
                ser.writeWithLength(obj.getProgram());
            }

            @Override
            public HashCode getHash(Script obj) {
                return Hashes.calcHash(obj);
            }
        });
    }

    @Override
    public void serializeValue(Serializer ser, Void value) {
    }

    @Override
    public long getSum(Void value) {
        return 0;
    }

    @Override
    public HashCode getKeyHash(Script key) {
        return Hashes.calcHash(key);
    }

    @Override
    protected void deserializeNode(Deserializer des) throws SerializationException {
        Script key = des.readObject(new Deserializer.ObjectReader<Script>() {
            @Override
            public Script readObject(Deserializer des) throws SerializationException {
                try {
                    return new Script(des.readBytes());
                } catch (ScriptException e) {
                    throw new SerializationException(e);
                }
            }
        });
        entries.put(key, null);
    }

    @Override
    protected void serializeSum(Serializer ser, long sum) throws SerializationException {
    }

    @Override
    public byte[] getHmacKey() {
        return Utils.HEX.decode("d431b155684582c6e0eef8b38d62321e");
    }

    public GenesisScriptMerbinnerTree(Map<Script, Void> nodes) {
        super(nodes);
    }

    public GenesisScriptMerbinnerTree() {
        super(Maps.<Script, Void>newHashMap());
    }
}
