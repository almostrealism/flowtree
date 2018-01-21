package org.smartcolors.marshal;

import java.io.ByteArrayInputStream;

/**
 * Created by devrandom on 2014-Nov-17.
 */
public class BytesDeserializer extends StreamDeserializer {
    public BytesDeserializer(byte[] bytes) {
        super(new ByteArrayInputStream(bytes));
    }
}
