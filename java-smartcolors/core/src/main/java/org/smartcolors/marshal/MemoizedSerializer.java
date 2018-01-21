package org.smartcolors.marshal;

import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;

import java.io.OutputStream;
import java.util.Map;

/**
 * Created by devrandom on 2014-Nov-19.
 */
public class MemoizedSerializer extends StreamSerializer {
    Map<HashCode, Long> memos = Maps.newHashMap();

    public MemoizedSerializer(OutputStream os) {
        super(os);
    }

    @Override
    public void write(Serializable obj) throws SerializationException {
        HashCode hash = obj.getHash();
        Long idx = memos.get(hash);
        if (idx != null)
            write(idx);
        else {
            write(0);
            super.write(obj);
            idx = memos.size() + 1L;
            memos.put(hash, idx);
        }
    }

    @Override
    public void write(Object obj, SerializerHelper helper) throws SerializationException {
        HashCode hash = helper.getHash(obj);
        Long idx = memos.get(hash);
        if (idx != null)
            write(idx);
        else {
            write(0);
            super.write(obj, helper);
            idx = memos.size() + 1L;
            memos.put(hash, idx);
        }
    }
}
