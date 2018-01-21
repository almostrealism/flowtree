package org.smartcolors.marshal;

/**
 * Created by devrandom on 2014-Nov-18.
 */
public class SerializationException extends Exception {
    public SerializationException() {
        super();
    }

    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(Exception e) {
        super(e);
    }
}
