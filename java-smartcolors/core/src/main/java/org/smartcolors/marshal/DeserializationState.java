package org.smartcolors.marshal;

import com.google.common.base.MoreObjects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by devrandom on 2015-07-19.
 */
public class DeserializationState {
    public DeserializationState(IterativeSerializable inst, Callback callback) {
        this(inst);
        this.callback = callback;
    }

    public interface Callback {
        void call(IterativeSerializable serializable) throws SerializationException;
    }

    public DeserializationState(IterativeSerializable serializable) {
        checkNotNull(serializable);
        this.serializable = serializable;
    }

    public IterativeSerializable serializable;
    public Callback callback;
    public boolean isDone = false;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("item", serializable.getClass().getName())
                .add("done", isDone)
                .add("callback", callback != null).toString();
    }
}
