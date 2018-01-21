package org.smartcolors.core;

import com.google.common.base.Throwables;
import com.google.common.hash.HashCode;
import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.smartcolors.marshal.*;

import java.util.Queue;

/**
 * Created by devrandom on 2014-Nov-19.
 */
public class GenesisScriptColorProof extends ColorProof {
    public static final int PROOF_TYPE = 2;

    private Transaction tx;
    private long index;

    public GenesisScriptColorProof() {
    }

    public GenesisScriptColorProof(ColorDefinition def, Transaction tx, long index) {
        this.def = def;
        this.tx = tx;
        this.index = index;
        quantity = calcQuantity();
        try {
            validate();
        } catch (ValidationException e) {
            Throwables.propagate(e);
        }
    }

    @Override
    protected void deserialize(Deserializer des) throws SerializationException {
        index = des.readVaruint();
        tx = des.readObject(new Deserializer.ObjectReader<Transaction>() {
            @Override
            public Transaction readObject(Deserializer des) throws SerializationException {
                try {
                    return new Transaction(params, des.readBytes(), 0);
                } catch (ProtocolException e) {
                    throw new SerializationException(e);
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new SerializationException(e);
                }
            }
        });
        try {
            quantity = calcQuantity();
        } catch (IllegalStateException e) {
            throw new SerializationException(e);
        } catch (IndexOutOfBoundsException e) {
            throw new SerializationException(e);
        }
        validate();
    }

    @Override
    public void serialize(Serializer ser) throws SerializationException {
        super.serialize(ser);
        ser.write(index);
        ser.write(tx, new SerializerHelper<Transaction>() {
            @Override
            public void serialize(Serializer ser, Transaction obj) throws SerializationException {
                ser.writeWithLength(obj.bitcoinSerialize());
            }

            @Override
            public HashCode getHash(Transaction obj) {
                return Hashes.calcHash(obj);
            }
        });
    }

    private long calcQuantity() {
        return SmartColors.removeMsbdropValuePadding(tx.getOutput((int) index).getValue().getValue());
    }

    @Override
    protected int getType() {
        return PROOF_TYPE;
    }

    @Override
    protected void doValidate(Queue<ColorProof> queue) throws ValidationException {
        if (index < 0 || index >= tx.getOutputs().size())
            throw new ValidationException("invalid index " + index);
        Script scriptPubKey = null;
        try {
            scriptPubKey = tx.getOutputs().get((int) index).getScriptPubKey();
        } catch (ScriptException e) {
            throw new ValidationException(e);
        }
        if (!def.getScriptGenesisPoints().containsKey(scriptPubKey))
            throw new ValidationException("non-genesis script " + scriptPubKey);
    }

    @Override
    public TransactionOutPoint getOutPoint() {
        return new TransactionOutPoint(params, index, tx);
    }

    public long getIndex() {
        return index;
    }

    public Transaction getTransaction() {
        return tx;
    }
}
