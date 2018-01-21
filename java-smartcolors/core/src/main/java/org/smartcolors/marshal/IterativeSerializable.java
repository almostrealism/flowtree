package org.smartcolors.marshal;

import java.util.Deque;

/**
 * Created by devrandom on 2015-07-19.
 */
public interface IterativeSerializable {
    void serialize(Serializer ser, Deque<SerializationState> states) throws SerializationException;

    void deserialize(Deserializer des, Deque<DeserializationState> stack) throws SerializationException;
}
