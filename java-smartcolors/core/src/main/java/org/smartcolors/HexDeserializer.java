package org.smartcolors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.google.common.hash.HashCode;

import java.io.IOException;

/**
 * Created by devrandom on 2014-Nov-26.
 */
public class HexDeserializer extends FromStringDeserializer<HashCode> {
    public HexDeserializer() {
        super(HashCode.class);
    }

    @Override
    protected HashCode _deserialize(String value, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return HashCode.fromString(value);
    }
}
