package org.smartcolors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.google.common.hash.HashCode;

import java.io.IOException;

/**
 * Created by devrandom on 2014-Nov-26.
 */
public class HexKeyDeserializer extends KeyDeserializer {
    public HexKeyDeserializer() {
    }

    @Override
    public Object deserializeKey(String value, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return HashCode.fromString(value);
    }
}
