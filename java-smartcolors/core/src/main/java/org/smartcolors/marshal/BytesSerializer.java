package org.smartcolors.marshal;

import java.io.ByteArrayOutputStream;

/**
 * Created by devrandom on 2014-Nov-17.
 */
public class BytesSerializer extends StreamSerializer {
    public BytesSerializer() {
        super(new ByteArrayOutputStream());
    }

    public byte[] getBytes() {
        return ((ByteArrayOutputStream) os).toByteArray();
    }
}
