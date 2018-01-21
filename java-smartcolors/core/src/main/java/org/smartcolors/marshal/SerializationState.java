package org.smartcolors.marshal;

import org.bitcoinj.core.TransactionOutPoint;

import java.util.Collection;

/**
 * Created by devrandom on 2015-07-19.
 */
public class SerializationState {
    public SerializationState(IterativeSerializable serializable, Collection<TransactionOutPoint> keys, int depth) {
        this.serializable = serializable;
        this.keys = keys;
        this.depth = depth;
    }

    public IterativeSerializable serializable;
    public Collection<TransactionOutPoint> keys;
    public int depth;
    public boolean isDone = false;
}
