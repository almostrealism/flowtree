package org.smartcolors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.testing.FakeTxBuilder;
import org.bitcoinj.wallet.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartcolors.core.ColorDefinition;
import org.smartcolors.core.SmartColors;

import java.security.SecureRandom;
import java.util.Map;

import static org.junit.Assert.*;
import static org.smartcolors.core.SmartColors.makeAssetInput;

public class CoinSelectorTest extends ColorTest {
    private static final Logger log = LoggerFactory.getLogger(CoinSelectorTest.class);

    private Script outputScript;
    private BitcoinCoinSelector bitcoinSelector;
    private AssetCoinSelector assetSelector;
    protected SPVColorScanner scanner;
    private TestMultiWallet multiWallet;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        scanner = new SPVColorScanner(params);
        scanner.addDefinition(def);
        colorChain =
                ColorKeyChain.builder()
                        .random(new SecureRandom())
                        .passphrase("")
                        .build();
        DeterministicKeyChain chain =
                DeterministicKeyChain.builder()
                        .seed(colorChain.getSeed())
                        .build();
        KeyChainGroup group = new KeyChainGroup(params);
        group.setLookaheadSize(20);
        group.setLookaheadThreshold(7);
        group.addAndActivateHDChain(colorChain);
        group.addAndActivateHDChain(chain);
        wallet = new SmartWallet(params, group);
        multiWallet = new TestMultiWallet(wallet);
        outputScript = colorChain.freshOutputScript(KeyChain.KeyPurpose.RECEIVE_FUNDS);
        bitcoinSelector = new BitcoinCoinSelector(wallet.getContext(), colorChain);
        assetSelector = new AssetCoinSelector(wallet.getContext(), colorChain, scanner.getColorTrackByHash(def.getHash()));
        scanner.receiveFromBlock(genesisTx, FakeTxBuilder.createFakeBlock(blockStore, genesisTx).storedBlock, AbstractBlockChain.NewBlockType.BEST_CHAIN, 0);
    }

    @Test
    public void testSelection() throws InsufficientMoneyException {
        // Incoming asset
        Transaction tx2 = new Transaction(params);
        tx2.addInput(makeAssetInput(tx2, genesisTx, 0));
        tx2.addOutput(Utils.makeAssetCoin(8), outputScript);
        tx2.addOutput(Coin.ZERO, opReturnScript);
        receiveTransaction(tx2);

        // Partially spend asset
        Transaction tx3 = new Transaction(params);
        tx3.addInput(makeAssetInput(tx3, tx2, 0));
        tx3.addOutput(Utils.makeAssetCoin(5), outputScript);
        tx3.addOutput(Utils.makeAssetCoin(3), ScriptBuilder.createOutputScript(new ECKey()));
        tx3.addOutput(Coin.ZERO, opReturnScript);
        receiveTransaction(tx3);

        // Incoming bitcoin
        Transaction tx4 = new Transaction(params);
        tx4.addInput(makeAssetInput(tx4, genesisTx, 0));
        tx4.addOutput(Coin.COIN, ScriptBuilder.createOutputScript(wallet.currentReceiveKey()));
        tx4.addOutput(Coin.ZERO, opReturnScript); // Spurious OP_RETURN marker
        receiveTransaction(tx4);

        AssetCoinSelector.AssetCoinSelection result = assetSelector.select(wallet.calculateAllSpendCandidates(true, false), 1);
        assertEquals(Sets.newHashSet(tx3.getOutput(0)), result.gathered);
        CoinSelection result1 = bitcoinSelector.select(Coin.FIFTY_COINS, wallet.calculateAllSpendCandidates(true, false));
        assertEquals(Sets.newHashSet(tx4.getOutput(0)), result1.gathered);

        Transaction tx = new Transaction(wallet.getParams());
        Wallet.SendRequest request = Wallet.SendRequest.forTx(tx);
        request.shuffleOutputs = false;
        request.coinSelector = bitcoinSelector;
        AssetCoinSelector.addAssetOutput(tx, ScriptBuilder.createOutputScript(new ECKey().toAddress(params)), 2L);
        assetSelector.completeTx(multiWallet, request, 2L);
        assertEquals(tx3.getOutput(0).getOutPointFor(), tx.getInput(0).getOutpoint());
        assertEquals(tx4.getOutput(0).getOutPointFor(), tx.getInput(1).getOutpoint());
        assertEquals(Coin.valueOf(1000), tx.getFee());
        assertEquals(2L, SmartColors.removeMsbdropValuePadding(tx.getOutput(0).getValue().getValue()));
        assertEquals(3L, SmartColors.removeMsbdropValuePadding(tx.getOutput(1).getValue().getValue()));

        tx.verify();
        for (TransactionInput input : tx.getInputs()) {
            input.verify();
        }
        assertEquals(Coin.COIN.add(Coin.valueOf(5L * 2)), tx.getValueSentFromMe(wallet));
        receiveTransaction(tx);
        Map<ColorDefinition, Long> change = scanner.getNetAssetChange(tx, multiWallet, colorChain);
        assertEquals(1, change.size());
        assertEquals(-2L, (long) change.get(def));
    }

    @Test
    public void testFee() throws InsufficientMoneyException {
        // Incoming asset
        Transaction tx2 = new Transaction(params);
        tx2.addInput(makeAssetInput(tx2, genesisTx, 0));
        tx2.addOutput(Utils.makeAssetCoin(8), outputScript);
        tx2.addOutput(Coin.ZERO, opReturnScript);
        receiveTransaction(tx2);

        // Incoming bitcoin
        Transaction tx4 = new Transaction(params);
        tx4.addInput(makeAssetInput(tx4, genesisTx, 0));
        tx4.addOutput(Coin.COIN, ScriptBuilder.createOutputScript(wallet.currentReceiveKey()));
        tx4.addOutput(Coin.ZERO, opReturnScript); // Spurious OP_RETURN marker
        receiveTransaction(tx4);

        Transaction tx = new Transaction(wallet.getParams());
        final Script notMyScript = ScriptBuilder.createOutputScript(new ECKey());
        for (int i = 0; i < 14; i++) {
            tx.addOutput(Coin.valueOf(1000), notMyScript);
        }
        Wallet.SendRequest request = Wallet.SendRequest.forTx(tx);
        request.shuffleOutputs = false;
        request.coinSelector = bitcoinSelector;
        AssetCoinSelector.addAssetOutput(tx, notMyScript, 2L);
        assetSelector.completeTx(multiWallet, request, 2L);
        final int length = tx.bitcoinSerialize().length;
        assertTrue(length >= 998);
        assertTrue(length <= 1001);
        assertEquals(Coin.valueOf(2000L), tx.getFee());
    }

    private void receiveTransaction(Transaction tx) {
        scanner.receiveFromBlock(tx, FakeTxBuilder.createFakeBlock(blockStore, tx).storedBlock, AbstractBlockChain.NewBlockType.BEST_CHAIN, 0);
        wallet.receiveFromBlock(tx, FakeTxBuilder.createFakeBlock(blockStore, tx).storedBlock, AbstractBlockChain.NewBlockType.BEST_CHAIN, 0);
    }

    private void receivePendingTransaction(Transaction tx) {
        wallet.receivePending(tx, Lists.<Transaction>newArrayList());
    }

    @Test
    public void testRestoreFromSeed() {
        DeterministicSeed existingSeed = wallet.getKeyChainSeed();
        DeterministicSeed seed = new DeterministicSeed(existingSeed.getMnemonicCode(), null, "", existingSeed.getCreationTimeSeconds());
        assertEquals(seed, existingSeed);
        ColorKeyChain colorChain1 = ColorKeyChain.builder()
                .seed(seed)
                .build();
        DeterministicKeyChain chain =
                DeterministicKeyChain.builder()
                        .seed(colorChain1.getSeed())
                        .build();
        KeyChainGroup group = new KeyChainGroup(params);
        group.addAndActivateHDChain(colorChain1);
        group.addAndActivateHDChain(chain);
        Wallet wallet1 = new SmartWallet(params, group);
        Script outputScript1 = colorChain1.freshOutputScript(KeyChain.KeyPurpose.RECEIVE_FUNDS);
        assertEquals(outputScript, outputScript1);
    }

    @Test
    public void testInsufficient() {
        Transaction tx;
        Wallet.SendRequest request;

        // Incoming asset
        Transaction tx2 = new Transaction(params);
        tx2.addInput(makeAssetInput(tx2, genesisTx, 0));
        tx2.addOutput(Utils.makeAssetCoin(8), outputScript);
        tx2.addOutput(Coin.ZERO, opReturnScript);
        receiveTransaction(tx2);

        // Partially spend asset
        Transaction tx3 = new Transaction(params);
        tx3.addInput(makeAssetInput(tx3, tx2, 0));
        tx3.addOutput(Utils.makeAssetCoin(5), outputScript);
        tx3.addOutput(Utils.makeAssetCoin(3), ScriptBuilder.createOutputScript(new ECKey()));
        receiveTransaction(tx3);

        // Incoming pending bitcoin
        Transaction tx4 = new Transaction(params);
        tx4.addInput(makeAssetInput(tx4, genesisTx, 0));
        tx4.addOutput(Coin.COIN, ScriptBuilder.createOutputScript(wallet.currentReceiveKey()));
        receivePendingTransaction(tx4);

        // Insufficient bitcoin
        tx = new Transaction(wallet.getParams());
        request = makeRequest(tx);
        AssetCoinSelector.addAssetOutput(tx, ScriptBuilder.createOutputScript(new ECKey().toAddress(params)), 2L);
        try {
            assetSelector.completeTx(multiWallet, request, 2L);
            fail();
        } catch (InsufficientMoneyException e) {
            assertEquals(Coin.valueOf(3050), e.missing);
        }

        // Asset is checked before bitcoin - insufficient asset
        tx = new Transaction(wallet.getParams());
        request = makeRequest(tx);
        AssetCoinSelector.addAssetOutput(tx, ScriptBuilder.createOutputScript(new ECKey().toAddress(params)), 7L);
        try {
            assetSelector.completeTx(multiWallet, request, 7L);
            fail();
        } catch (InsufficientAssetException e) {
            assertEquals(Coin.valueOf(2), e.missing);
        } catch (InsufficientMoneyException e) {
            fail();
        }

        // Confirm
        receiveTransaction(tx4);

        // Insufficient asset
        tx = new Transaction(wallet.getParams());
        request = makeRequest(tx);
        AssetCoinSelector.addAssetOutput(tx, ScriptBuilder.createOutputScript(new ECKey().toAddress(params)), 7L);
        try {
            assetSelector.completeTx(multiWallet, request, 7L);
            fail();
        } catch (InsufficientAssetException e) {
            assertEquals(Coin.valueOf(2), e.missing);
        } catch (InsufficientMoneyException e) {
            fail();
        }

        // Sufficient
        tx = new Transaction(wallet.getParams());
        request = makeRequest(tx);
        AssetCoinSelector.addAssetOutput(tx, ScriptBuilder.createOutputScript(new ECKey().toAddress(params)), 2L);
        try {
            assetSelector.completeTx(multiWallet, request, 2L);
        } catch (InsufficientMoneyException e) {
            fail();
        }

        // Incoming pending asset
        Transaction tx5 = new Transaction(params);
        tx5.addInput(makeAssetInput(tx5, tx3, 1));
        tx5.addOutput(Utils.makeAssetCoin(3), outputScript);
        receivePendingTransaction(tx5);

        // Insufficient asset
        tx = new Transaction(wallet.getParams());
        request = makeRequest(tx);
        AssetCoinSelector.addAssetOutput(tx, ScriptBuilder.createOutputScript(new ECKey().toAddress(params)), 7L);
        try {
            assetSelector.completeTx(multiWallet, request, 7L);
            fail();
        } catch (InsufficientAssetException e) {
            assertEquals(Coin.valueOf(2), e.missing);
        } catch (InsufficientMoneyException e) {
            fail();
        }

        // Confirm
        receiveTransaction(tx5);

        // Sufficient
        tx = new Transaction(wallet.getParams());
        request = makeRequest(tx);
        AssetCoinSelector.addAssetOutput(tx, ScriptBuilder.createOutputScript(new ECKey().toAddress(params)), 7L);
        try {
            assetSelector.completeTx(multiWallet, request, 7L);
        } catch (InsufficientMoneyException e) {
            fail();
        }
    }

    private Wallet.SendRequest makeRequest(Transaction tx) {
        Wallet.SendRequest request;
        request = Wallet.SendRequest.forTx(tx);
        request.shuffleOutputs = false;
        request.coinSelector = bitcoinSelector;
        return request;
    }
}