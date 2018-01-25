package org.smartcolors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.junit.Before;
import org.junit.Test;
import org.smartcolors.core.*;
import org.smartcolors.marshal.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.smartcolors.Utils.parseHex;

public class ColorDefinitionTest {
    public static final String NEW_GOLD_HASH = "a4882cfa917048625e78d46846b0e50f6502e2c674eb125ad0d8b5cdf70efa11";
    private ColorDefinition def;
    private ObjectMapper mapper;
    private NetworkParameters params;

    static class KernelTestItem {
        public String comment;
        public List<String> nseqs;
        public List<Long> outputs;
        public Long inputs[];
        public Long expected[];
    }

    @Before
    public void setUp() {
        params = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
        def = new ColorDefinition(params, new GenesisOutPointsMerbinnerTree(params), new GenesisScriptMerbinnerTree());
        mapper = new ObjectMapper();
    }

    @Test
    public void kernel() throws IOException {
        List<KernelTestItem> items =
                mapper.readValue(FixtureHelpers.fixture("kernel.json"),
                        new TypeReference<List<KernelTestItem>>() {
                        });
        for (KernelTestItem item : items) {
            checkKernel(item);
        }
    }

    @Test
    public void deserialize() throws IOException, SerializationException {
        byte[] defBytes = Utils.HEX.decode("0100ec746756751d8ac6e9345f9050e1565f013ba3edfd7a7b12b27ac72c3e67768f617fc81bc3888a51323a9fb8aa4b1e5e4a0000000080e497d01200");
        BytesDeserializer des = new BytesDeserializer(defBytes);
        ColorDefinition def = ColorDefinition.deserialize(params, des);
        System.out.println(def.toStringFull());
        assertEquals(0, def.getBlockheight());
        long value = def.getOutPointGenesisPoints().get(new TransactionOutPoint(params, 0, Sha256Hash.wrap("4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b")));
        assertEquals(5000000000L, value);

        assertEquals("989d170a0f0c3dfb8d5266d4e9d355583a6a3e100c0d08ff6dee81f43c33c150", Utils.HEX.encode(def.getHash().asBytes()));
    }

    @Test
    public void scdef() throws IOException, SerializationException {
        InputStream is = Resources.getResource("gold.scdef").openStream();
        ColorDefinition def = ColorDefinition.deserializeFromFile(params, is);
        assertEquals("812d469f6aa2c320767c7444610df2b7bcb048d1a5f11630e4a9c0d1051c0bd3", def.getHash().toString());
        BytesSerializer ser = new BytesSerializer();
        def.serialize(ser);
        System.out.println(Utils.HEX.encode(ser.getBytes()));
    }

    @Test
    public void json() throws IOException {
        Map<String, Object> values = Maps.newHashMap();
        values.put(ColorDefinition.NETWORK_ID_INJECTABLE, NetworkParameters.ID_TESTNET);
        mapper.setInjectableValues(new InjectableValues.Std(values));
        ColorDefinition gold = mapper.readValue(FixtureHelpers.fixture("newgold.json"), ColorDefinition.TYPE_REFERENCE);
        assertEquals("Gold", gold.getName());
        assertEquals(NEW_GOLD_HASH, gold.getHash().toString());
        String oilJson = mapper.writeValueAsString(gold);
        Map goldMap = mapper.readValue(FixtureHelpers.fixture("newgold.json"), Map.class);
        Map reconstructedMap = mapper.readValue(oilJson, Map.class);
        assertEquals(goldMap, reconstructedMap);
    }

    private void checkKernel(KernelTestItem item) {
        Transaction tx = new Transaction(params);
        for (String nseq : item.nseqs) {
            TransactionInput input = new TransactionInput(params, tx, new byte[0]);
            input.setSequenceNumber(parseHex(nseq));
            tx.addInput(input);
        }

        for (long outputAmount : item.outputs) {
            tx.addOutput(Coin.valueOf(SmartColors.addMsbdropValuePadding(outputAmount, 0)), new Script(new byte[0]));
        }

        Long[] colorOut = def.applyKernel(tx, item.inputs);
        assertArrayEquals(item.comment, item.expected, colorOut);
    }

    @Test
    public void steg() throws SerializationException {
        Deserializer des = new BytesDeserializer(Utils.HEX.decode("0100586747ecf6e6cecea82f3e1840e411a401aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa000000002a00"));
        ColorDefinition def = ColorDefinition.deserialize(params, des);
        TransactionOutPoint outpoint = new TransactionOutPoint(params, 0, Sha256Hash.wrap("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
        long pad = def.nsequencePad(outpoint);
        assertEquals(1331075725, pad);
    }
}
