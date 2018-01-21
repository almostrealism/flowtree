package org.smartcolors.marshal;

import com.google.common.collect.Queues;
import org.smartcolors.core.PrevoutProofsMerbinnerTree;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;

/**
 * Created by devrandom on 2014-Nov-17.
 */
public class StreamSerializer implements Serializer {
    protected final OutputStream os;

    public StreamSerializer(OutputStream os) {
        this.os = os;
    }

    @Override
    public void write(long value) throws SerializationException {
        try {
            if (value == 0)
                os.write(0);
            while (value != 0) {
                int b = (int) (value & 0x7f);
                value = (value >> 7) & Long.MAX_VALUE;
                if (value != 0)
                    b |= 0x80;
                os.write(b);
            }
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void write(byte[] bytes) throws SerializationException {
        try {
            os.write(bytes);
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void writeWithLength(byte[] bytes) throws SerializationException {
        write(bytes.length);
        try {
            os.write(bytes);
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void write(Serializable obj) throws SerializationException {
        if (obj instanceof IterativeSerializable) {
            // Switch to iterative serialization
            ArrayDeque<SerializationState> stack = Queues.newArrayDeque();
            IterativeSerializable tree = (PrevoutProofsMerbinnerTree) obj;
            stack.push(new SerializationState(tree, null, 0));
            while (!stack.isEmpty()) {
                SerializationState state = stack.getFirst();
                if (state.isDone) {
                    stack.pop();
                } else {
                    state.serializable.serialize(this, stack);
                    state.isDone = true;
                }
            }
        } else {
            obj.serialize(this);
        }
    }

    @Override
    public void write(Object obj, SerializerHelper helper) throws SerializationException {
        helper.serialize(this, obj);
    }
}
