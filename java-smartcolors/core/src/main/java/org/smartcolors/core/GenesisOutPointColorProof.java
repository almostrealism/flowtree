package org.smartcolors.core;

import com.google.common.base.Throwables;
import com.google.common.hash.HashCode;
import org.bitcoinj.core.TransactionOutPoint;
import org.smartcolors.marshal.*;

import java.util.Queue;

/**
 * Created by devrandom on 2014-Nov-19.
 */
public class GenesisOutPointColorProof extends ColorProof {
    public static final int PROOF_TYPE = 1;

    private TransactionOutPoint outpoint;

    public GenesisOutPointColorProof() {
    }

    public GenesisOutPointColorProof(ColorDefinition def, TransactionOutPoint outpoint) {
        this.def = def;
        this.outpoint = outpoint;
        quantity = calcQuantity();
        try {
            validate();
        } catch (ValidationException e) {
            Throwables.propagate(e);
        }
    }

    private Long calcQuantity() {
        return def.getOutPointGenesisPoints().get(outpoint);
    }

    @Override
    protected void deserialize(Deserializer des) throws SerializationException {
        outpoint = des.readObject(new Deserializer.ObjectReader<TransactionOutPoint>() {
            @Override
            public TransactionOutPoint readObject(Deserializer des) throws SerializationException {
                return new TransactionOutPoint(params, des.readBytes(36), 0);
            }
        });
        Long quantity = calcQuantity();
        if (quantity == null)
            quantity = 0L; // Checked in validate
        this.quantity = quantity;
        validate();
    }

    @Override
    public void serialize(Serializer ser) throws SerializationException {
        super.serialize(ser);
        ser.write(outpoint, new SerializerHelper<TransactionOutPoint>() {
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
    protected int getType() {
        return PROOF_TYPE;
    }

    @Override
    protected void doValidate(Queue<ColorProof> queue) throws ValidationException {
        if (!def.getOutPointGenesisPoints().containsKey(outpoint))
            throw new ValidationException("outpoint not in def " + outpoint);
    }

    @Override
    public TransactionOutPoint getOutPoint() {
        return outpoint;
    }
}
