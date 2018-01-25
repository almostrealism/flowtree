package org.smartcolors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import org.junit.Before;
import org.junit.Test;
import org.smartcolors.marshal.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by devrandom on 2014-Nov-17.
 */
public class MarshalTest {
    private ObjectMapper mapper;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
    }

    static class BoxedVaruint extends HashableSerializable {
        public long value;

        public BoxedVaruint(long value) {
            this.value = value;
        }

        @Override
        public void serialize(Serializer ser) throws SerializationException {
            ser.write(value);
        }

        @Override
        public byte[] getHmacKey() {
            return Utils.HEX.decode("dd2617248e435da6db7c119c17cc19cd");
        }

        public static BoxedVaruint deserialize(Deserializer ser) throws SerializationException {
            return new BoxedVaruint(ser.readVarulong());
        }
    }

    static class BoxedBytes extends HashableSerializable {
        public byte[] bytes;
        private byte[] cachedHash;

        public BoxedBytes(byte[] bytes) {
            this.bytes = bytes;
        }

        public void serialize(Serializer serializer) throws SerializationException {
            serializer.writeWithLength(bytes);
        }

        public void serializeFixed(Serializer ser) throws SerializationException {
            ser.write(bytes);
        }

        @Override
        public byte[] getHmacKey() {
            return Utils.HEX.decode("f690a4d282810e868a0d7d59578a6585");
        }

        public static BoxedBytes deserialize(Deserializer ser, Integer expectedLength) throws SerializationException {
            if (expectedLength != null)
                return new BoxedBytes(ser.readBytes(expectedLength));
            else
                return new BoxedBytes(ser.readBytes());
        }
    }

    static class BoxedObj extends HashableSerializable {
        public BoxedBytes buf;
        public BoxedVaruint i;
        private byte[] cachedHash;

        public BoxedObj(byte[] buf, long i) {
            this.buf = new BoxedBytes(buf);
            this.i = new BoxedVaruint(i);
        }

        public BoxedObj() {
        }

        public void serialize(Serializer ser) throws SerializationException {
            ser.write(buf);
            ser.write(i);
        }

        public static BoxedObj deserialize(Deserializer des) throws SerializationException {
            BoxedObj obj = new BoxedObj();
            obj.buf = BoxedBytes.deserialize(des, null);
            obj.i = BoxedVaruint.deserialize(des);
            return obj;
        }

        @Override
        public byte[] getHmacKey() {
            return Utils.HEX.decode("296d566c10ebb4b92e8a7f6e909eb191");
        }
    }

    @Test
    public void testVaruint() throws IOException, SerializationException {
        List<List<String>> items =
                mapper.readValue(FixtureHelpers.fixture("marshal/valid_varuints.json"),
                        new TypeReference<List<List<String>>>() {
                        });
        for (List<String> entry : items) {
            BytesSerializer serializer = new BytesSerializer();
            if (entry.size() == 1) continue; // comment
            byte[] expected = Utils.HEX.decode(entry.get(0));
            long value = Long.parseLong(entry.get(1));
            BoxedVaruint boxed = new BoxedVaruint(value);
            boxed.serialize(serializer);
            byte[] bytes = serializer.getBytes();
            assertArrayEquals(entry.get(0), expected, bytes);
            BytesDeserializer deserializer = new BytesDeserializer(bytes);
            assertEquals(value, BoxedVaruint.deserialize(deserializer).value);
        }
    }

    @Test
    public void testBytes() throws IOException, SerializationException {
        List<List<String>> items =
                mapper.readValue(FixtureHelpers.fixture("marshal/valid_bytes.json"),
                        new TypeReference<List<List<String>>>() {
                        });
        for (List<String> entry : items) {
            BytesSerializer serializer = new BytesSerializer();
            if (entry.size() == 1) continue; // comment
            byte[] expectedBytes = Utils.HEX.decode(entry.get(0));
            byte[] value = Utils.HEX.decode(entry.get(1));
            Integer expectedLength = entry.get(2) == null ? null : Integer.valueOf(entry.get(2));
            BoxedBytes boxed = new BoxedBytes(value);
            if (expectedLength != null)
                boxed.serializeFixed(serializer);
            else
                boxed.serialize(serializer);
            byte[] bytes = serializer.getBytes();
            assertArrayEquals(entry.get(0), expectedBytes, bytes);
            BytesDeserializer deserializer = new BytesDeserializer(bytes);
            assertArrayEquals(value, BoxedBytes.deserialize(deserializer, expectedLength).bytes);
        }
    }

    @Test
    public void testObjs() throws IOException, SerializationException {
        List<List<String>> items =
                mapper.readValue(FixtureHelpers.fixture("marshal/valid_boxed_objs.json"),
                        new TypeReference<List<List<String>>>() {
                        });
        for (List<String> entry : items) {
            BytesSerializer serializer = new BytesSerializer();
            if (entry.size() == 1) continue; // comment
            byte[] expectedBytes = Utils.HEX.decode(entry.get(0));
            byte[] buf = Utils.HEX.decode(entry.get(1));
            long i = Long.parseLong(entry.get(2));
            byte[] expectedHash = Utils.HEX.decode(entry.get(3));
            BoxedObj boxed = new BoxedObj(buf, i);
            serializer.write(boxed);
            byte[] bytes = serializer.getBytes();
            assertArrayEquals(entry.get(0), expectedBytes, bytes);
            BytesDeserializer deserializer = new BytesDeserializer(bytes);
            BoxedObj actual = BoxedObj.deserialize(deserializer);
            assertArrayEquals(actual.buf.bytes, buf);
            assertEquals(actual.i.value, i);
            serializer = new BytesSerializer();
            serializer.write(actual);
            byte[] roundTrip = serializer.getBytes();
            assertArrayEquals(expectedBytes, roundTrip);
            byte[] hash = boxed.getHash().asBytes();
            assertArrayEquals(expectedHash, hash);
        }
    }

    static class TestMerbinnerTree extends MerbinnerTree<byte[], byte[]> {
        @Override
        public void serializeKey(Serializer ser, byte[] key) throws SerializationException {
            ser.write(key);
        }

        @Override
        public void serializeValue(Serializer ser, byte[] value) throws SerializationException {
            ser.write(value);
        }

        @Override
        public long getSum(byte[] value) {
            return 0;
        }

        @Override
        public com.google.common.hash.HashCode getKeyHash(byte[] key) {
            return HashCode.fromBytes(key);
        }

        @Override
        protected void deserializeNode(Deserializer des) throws SerializationException {
            byte[] key = des.readBytes(4);
            byte[] value = des.readBytes(4);
            entries.put(key, value);
        }

        @Override
        public byte[] getHmacKey() {
            return Utils.HEX.decode("92e8898fcfa8b86b60b32236d6990da0");
        }

        TestMerbinnerTree(Map<byte[], byte[]> nodes) {
            super(nodes);
        }
    }

    @Test
    public void testMerbinner() throws IOException, SerializationException {
        List<List<Object>> items =
                mapper.readValue(FixtureHelpers.fixture("marshal/merbinnertree_hashes.json"),
                        new TypeReference<List<List<Object>>>() {
                        });
        for (List<Object> entry : items) {
            if (entry.size() == 1) continue; // comment
            Map<String, String> map = (Map<String, String>) entry.get(0);
            String mode = (String) entry.get(1);
            String expectedHex = (String) entry.get(2);
            byte[] expected = Utils.HEX.decode(expectedHex.replaceAll(" ", ""));
            Map<byte[], byte[]> nodes = Maps.newHashMap();
            for (String keyString : map.keySet()) {
                byte[] value = Utils.HEX.decode(map.get(keyString));
                byte[] key = Utils.HEX.decode(keyString);
                nodes.put(key, value);
            }
            TestMerbinnerTree tree = new TestMerbinnerTree(nodes);

            if (mode.equals("serialize")) {
                BytesSerializer ser = new BytesSerializer();
                ser.write(tree);
                assertArrayEquals(expected, ser.getBytes());
                BytesDeserializer des = new BytesDeserializer(expected);
                TestMerbinnerTree tree2 = new TestMerbinnerTree(Maps.<byte[], byte[]>newHashMap());
                tree2.deserialize(des);
                BytesSerializer ser1 = new BytesSerializer();
                tree2.serialize(ser1);
                assertArrayEquals(expected, ser1.getBytes());
            } else if (mode.equals("hash")) {
                assertArrayEquals(expected, tree.getHash().asBytes());
            } else {
                fail(mode);
            }
        }
    }
}
