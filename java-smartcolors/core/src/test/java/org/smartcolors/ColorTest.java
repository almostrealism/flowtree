package org.smartcolors;

import com.google.common.collect.Maps;
import org.bitcoinj.core.*;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.store.MemoryBlockStore;
import org.bitcoinj.testing.FakeTxBuilder;
import org.junit.Before;
import org.smartcolors.core.*;

import java.math.BigInteger;
import java.util.Map;

import static org.bitcoinj.core.Utils.HEX;

/**
 * Created by devrandom on 2014-Oct-21.
 */
public class ColorTest {
    public static final Script EMPTY_SCRIPT = new Script(new byte[0]);

    protected NetworkParameters params;
    protected Transaction genesisTx;
    protected TransactionOutPoint genesisOutPoint;
    protected MemoryBlockStore blockStore;
    protected StoredBlock genesisBlock;
    protected ColorDefinition def;
    protected Script opReturnScript;
    protected ColorKeyChain colorChain;
    protected SmartWallet wallet;
    protected BigInteger privkey;
    protected ECKey privkey1;
    protected MultiWallet multiWallet;

    @Before
    public void setUp() throws Exception {
        params = NetworkParameters.fromID(NetworkParameters.ID_REGTEST);
        new Context(params);
        blockStore = new MemoryBlockStore(params);
        genesisTx = new Transaction(params);
        genesisTx.addInput(Sha256Hash.ZERO_HASH, 0, EMPTY_SCRIPT);
        opReturnScript = SmartColors.makeOpReturnScript();
        genesisTx.addOutput(Utils.makeAssetCoin(10), new Script(new byte[0]));
        if (SmartColors.ENABLE_OP_RETURN_MARKER)
            genesisTx.addOutput(Coin.ZERO, opReturnScript);
        genesisBlock = FakeTxBuilder.createFakeBlock(blockStore, genesisTx).storedBlock;
        genesisOutPoint = new TransactionOutPoint(params, 0, genesisTx);
        Map<TransactionOutPoint, Long> nodes = Maps.newHashMap();
        nodes.put(genesisOutPoint, 5L);
        GenesisOutPointsMerbinnerTree outPoints = new GenesisOutPointsMerbinnerTree(params, nodes);
        Map<String, String> metadata = Maps.newHashMap();
        metadata.put("name", "widgets");
        def = new ColorDefinition(params, outPoints, new GenesisScriptMerbinnerTree(), metadata);
        colorChain = null;
        wallet = null;
        privkey = new BigInteger(1, HEX.decode("180cb41c7c600be951b5d3d0a7334acc7506173875834f7a6c4c786a28fcbb19"));
        privkey1 = new DumpedPrivateKey(TestNet3Params.get(), "92shANodC6Y4evT5kFzjNFQAdjqTtHAnDTLzqBBq4BbKUPyx6CD").getKey();
    }

    protected Transaction makeTx2(ECKey myKey) {
        Transaction tx2 = new Transaction(params);
        tx2.addInput(SmartColors.makeAssetInput(tx2, genesisTx, 0));
        Script outputScript = makeP2SHOutputScript(myKey);
        tx2.addOutput(Utils.makeAssetCoin(5), outputScript);
        tx2.addOutput(Coin.ZERO, opReturnScript);
        return tx2;
    }

    protected Script makeP2SHOutputScript(ECKey key) {
        return ScriptBuilder.createP2SHOutputScript(ScriptBuilder.createOutputScript(key));
    }
}
