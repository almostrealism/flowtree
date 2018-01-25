package org.smartcolors;

import com.google.common.io.Resources;
import org.bitcoinj.core.NetworkParameters;
import org.junit.Before;
import org.junit.Test;
import org.smartcolors.core.*;
import org.smartcolors.marshal.BytesDeserializer;
import org.smartcolors.marshal.BytesSerializer;
import org.smartcolors.marshal.SerializationException;

import java.io.IOException;
import java.io.InputStream;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by devrandom on 2014-Nov-19.
 */
public class ColorProofTest {
    private static final boolean DISABLE_SLOW_TESTS = true;
    private NetworkParameters params;

    @Before
    public void setUp() {
        params = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
    }

    @Test
    public void scriptTree() throws Exception {
        GenesisScriptMerbinnerTree tree = new GenesisScriptMerbinnerTree();
        tree.deserialize(new BytesDeserializer(Utils.HEX.decode("02020002011976a914d8296c49ea7fe04040e3d954abc80a5629e06f0788ac0117a914b77c14cdc812ddc740899867b1d5ae639a82db26870117a914da1745e9b549bd0bfa1a569971c77eba30cd5a4b87")));
        assertEquals("95a254d62931a7546dae40cef57b11b4d89c3e1721b517a3b983fa8f3aec9cfd", tree.getHash().toString());
    }

    @Test
    public void genesis1() throws Exception {
        InputStream is = Resources.getResource("proofs/genesis/2709f98721fbe6d3f78b45364cc2745e29da71c6c479498d4c91792af5c5fa9e:0.scproof").openStream();
        ColorProof proof = ColorProof.deserializeFromFile(params, is);
        assertEquals("f8bcdc4311624b8a0dcf79a92a46d08c26d1409066bb888f32d25f5f400e138d", proof.getHash().toString());
        testMutate(proof);
    }

    @Test
    public void genesis2() throws Exception {
        InputStream is = Resources.getResource("proofs/genesis/1051584e9e19f740ddf32b7b3d30c274ae23f2ff2b706f1446e4f7815563c3fd:0.scproof").openStream();
        ColorProof proof = ColorProof.deserializeFromFile(params, is);
        assertEquals("bafedb7c365d58eac2cb83513c4a084235826f092913022699d208d4d972b3d6", proof.getHash().toString());
        testMutate(proof);
    }

    @Test
    public void genesisScript() throws Exception {
        InputStream is = Resources.getResource("proofs/scriptPubKey/340c79e8369c539c3100b4f49ec869a0499d81d9f8db9feaf4694272bf5b432e:0.scproof").openStream();
        ColorProof proof = ColorProof.deserializeFromFile(params, is);
        assertEquals("15f7ab204611c7eab35a06744652edb71defdfb5ac13879e7cce20a3d3f8ce1b", proof.getHash().toString());
        testMutate(proof);
    }

    @Test
    public void transfer() throws Exception {
        ColorProof proof = readProof("proofs/transferred/29ac0d5313882c337921dd34fee09efce6bd9fcc2d5c16155e9c22af1b4b9a13:0.scproof");
        assertEquals("3e5d6880c921a62f756df6cc1768528af56408f2e8f91a8ad6f61443af24c76f", proof.getHash().toString());
        testMutate(proof);
    }

    @Test
    public void transferAll() throws Exception {
        readProof("proofs/transferred/ceac36cf041b844c047fbba23fc3cbcac8f6a245c7e687da801a356f96ec2003:1.scproof");
        for (int i = 0; i <= 15; i++) {
            String name = "proofs/transferred/57fc75fb1722a6573e0dccfb405ec04652dfd38963035685d8cf50c3f70bc69b:" + i + ".scproof";
            ColorProof proof = readProof(name);
            testMutate(proof);
        }
    }

    private void testMutate(ColorProof proof) throws SerializationException {
        if (DISABLE_SLOW_TESTS) return;
        System.out.println("testMutate " + proof.getHash());
        BytesSerializer ser = new BytesSerializer();
        proof.serialize(ser);
        byte[] b = ser.getBytes();
        for (int i = 0; i < b.length; i++) {
            byte[] b1 = b.clone();
            b1[i]++;
            checkMutation(proof, b1, i);
            b1[i] -= 2;
            checkMutation(proof, b1, i);
        }
    }

    private void checkMutation(ColorProof proof, byte[] b1, int idx) {
        BytesDeserializer des = new BytesDeserializer(b1);
        try {
            ColorProof proof1 = ColorProof.deserialize(params, des);
            boolean sameDef = proof.getDefinition().getHash().equals(proof1.getDefinition().getHash());
            boolean sameArgs = true;
            if (proof instanceof TransferColorProof && proof1 instanceof TransferColorProof) {
                TransferColorProof tproof = (TransferColorProof) proof;
                TransferColorProof tproof1 = (TransferColorProof) proof1;
                sameArgs = sameArgs && tproof.getIndex() == tproof1.getIndex();
                sameArgs = sameArgs && tproof.getTransaction().getHash().equals(tproof1.getTransaction().getHash());
            }
            if (proof instanceof GenesisScriptColorProof && proof1 instanceof GenesisScriptColorProof) {
                GenesisScriptColorProof gproof = (GenesisScriptColorProof) proof;
                GenesisScriptColorProof gproof1 = (GenesisScriptColorProof) proof1;
                sameArgs = sameArgs && gproof.getIndex() == gproof1.getIndex();
                sameArgs = sameArgs && gproof.getTransaction().getHash().equals(gproof1.getTransaction().getHash());
            }
            if (sameDef && sameArgs && proof1.getQuantity() > 0) {
                System.out.println(proof);
                System.out.println(proof1);
                fail("failed at mutation index " + idx);
            }
            //fail();
        } catch (SerializationException e) {
            // expected
        }
    }

    private ColorProof readProof(String name) throws IOException, SerializationException {
        InputStream is = Resources.getResource(name).openStream();
        return ColorProof.deserializeFromFile(params, is);
    }
}
