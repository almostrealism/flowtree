package org.smartcolors;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import org.bitcoinj.core.*;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.testing.FakeTxBuilder;
import org.bitcoinj.wallet.KeyChainGroup;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.smartcolors.core.ColorDefinition;
import org.smartcolors.core.SmartColors;
import org.smartcolors.protos.Protos;

import javax.annotation.Nullable;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public class SPVColorScannerTest extends ColorTest {
    private SmartwalletExtension ext;
    protected SPVColorScanner scanner;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        scanner = new SPVColorScanner(params);
        scanner.addDefinition(def);
        ext = new SmartwalletExtension(params);
        colorChain = new ColorKeyChain(new SecureRandom(), 128, "", 0) {
            // Hack - delegate to the current wallet
            @Override
            public boolean isOutputToMe(TransactionOutput output) {
                if (output.getScriptPubKey().isSentToAddress())
                    return wallet.isPubKeyHashMine(output.getScriptPubKey().getPubKeyHash());
                else if (output.getScriptPubKey().isSentToRawPubKey())
                    return wallet.isPubKeyMine(output.getScriptPubKey().getPubKey());
                return false;
            }
        };
    }

    @Test
    public void testGetColors() {
        Set<ColorDefinition> colors = scanner.getDefinitions();
        assertEquals(3, colors.size());
        assertTrue(colors.contains(scanner.getBitcoinDefinition()));
        assertTrue(colors.contains(scanner.getUnknownDefinition()));
        assertTrue(colors.contains(def));
    }

    @Test
    public void testBloomFilter() throws Exception {
        // Genesis
        assertEquals(1, scanner.getBloomFilterElementCount());
        assertTrue(getBloomFilter().contains(org.bitcoinj.core.Utils.HEX.decode("534d415254415353")));
    }

    @Ignore
    @Test
    public void testGetNetAssetChangeUnknown() {
        KeyChainGroup group = new KeyChainGroup(params);
        group.setLookaheadSize(20);
        group.setLookaheadThreshold(7);
        group.addAndActivateHDChain(colorChain);
        group.createAndActivateNewHDChain();
        wallet = new SmartWallet(params, group) {
            @Override
            public boolean isPubKeyMine(byte[] pubkey) {
                return true;
            }
        };
        multiWallet = new TestMultiWallet(wallet);
        Transaction tx2 = makeTx2(new ECKey());
        Map<ColorDefinition, Long> res = scanner.getNetAssetChange(tx2, multiWallet, colorChain);
        Map<ColorDefinition, Long> expected = Maps.newHashMap();
        expected.put(scanner.getUnknownDefinition(), 5L);
        assertEquals(expected, res);
    }

    @Ignore
    @Test
    public void testGetNetAssetChange() throws SPVColorScanner.ColorDefinitionException {
        final ECKey myKey = ECKey.fromPrivate(privkey);
        final Map<Sha256Hash, Transaction> txs = Maps.newHashMap();
        scanner.receiveFromBlock(genesisTx, FakeTxBuilder.createFakeBlock(blockStore, genesisTx).storedBlock, AbstractBlockChain.NewBlockType.BEST_CHAIN, 0);
        wallet = new SmartWallet(params) {
            @Override
            public boolean isPubKeyMine(byte[] pubkey) {
                return Arrays.equals(pubkey, myKey.getPubKey());
            }

            @Nullable
            @Override
            public Transaction getTransaction(Sha256Hash hash) {
                return txs.get(hash);
            }
        };
        multiWallet = new TestMultiWallet(wallet);

        Transaction tx2 = makeTx2(myKey);
        scanner.receiveFromBlock(tx2, FakeTxBuilder.createFakeBlock(blockStore, tx2).storedBlock, AbstractBlockChain.NewBlockType.BEST_CHAIN, 0);
        wallet.receiveFromBlock(tx2, FakeTxBuilder.createFakeBlock(blockStore, tx2).storedBlock, AbstractBlockChain.NewBlockType.BEST_CHAIN, 0);
        Map<ColorDefinition, Long> expected = Maps.newHashMap();
        Map<ColorDefinition, Long> res = scanner.getNetAssetChange(tx2, multiWallet, colorChain);
        expected.put(def, 5L);
        assertEquals(expected, res);


        Transaction tx3 = new Transaction(params);
        tx3.addInput(SmartColors.makeAssetInput(tx3, tx2, 0));
        tx3.addOutput(Utils.makeAssetCoin(2), ScriptBuilder.createOutputScript(myKey));
        tx3.addOutput(Utils.makeAssetCoin(3), ScriptBuilder.createOutputScript(privkey1));
        tx3.addOutput(Coin.ZERO, opReturnScript);
        scanner.receiveFromBlock(tx3, FakeTxBuilder.createFakeBlock(blockStore, tx3).storedBlock, AbstractBlockChain.NewBlockType.BEST_CHAIN, 0);
        wallet.receiveFromBlock(tx3, FakeTxBuilder.createFakeBlock(blockStore, tx3).storedBlock, AbstractBlockChain.NewBlockType.BEST_CHAIN, 0);

        expected.clear();
        res = scanner.getNetAssetChange(tx3, multiWallet, colorChain);
        expected.put(def, -3L);
        assertEquals(expected, res);

        txs.put(genesisTx.getHash(), genesisTx);
        txs.put(tx2.getHash(), tx2);
        txs.put(tx3.getHash(), tx3);

        SPVColorScanner scanner1 = new SPVColorScanner(params);
        scanner1.addDefinition(def);
        Protos.ColorScanner proto = ext.serializeScanner(scanner);
        ext.deserializeScannerSPV(params, proto, scanner1);
        assertEquals(scanner.getMapBlockTx(), scanner1.getMapBlockTx());
        assertEquals("9ba0c8df6d37c0dba260ee0510e68cb41d2d0b19396621757522e5cc270dddb8",
                scanner.getColorTrackByDefinition(def).getStateHash().toString());
    }

    @Test
    public void testGetTransactionWithUnknownAsset() throws ExecutionException, InterruptedException {
        final ECKey myKey = new ECKey();
        scanner.receiveFromBlock(genesisTx, FakeTxBuilder.createFakeBlock(blockStore, genesisTx).storedBlock, AbstractBlockChain.NewBlockType.BEST_CHAIN, 0);
        wallet = new SmartWallet(params) {
            @Override
            public boolean isPubKeyMine(byte[] pubkey) {
                return Arrays.equals(pubkey, myKey.getPubKey());
            }
        };
        multiWallet = new TestMultiWallet(wallet);

        Transaction tx2 = makeTx2(myKey);
        wallet.receiveFromBlock(tx2, FakeTxBuilder.createFakeBlock(blockStore, tx2).storedBlock, AbstractBlockChain.NewBlockType.BEST_CHAIN, 0);
        ListenableFuture<Transaction> future = scanner.getTransactionWithKnownAssets(tx2, multiWallet, colorChain);
        //assertFalse(future.isDone());
        //scanner.receiveFromBlock(tx2, FakeTxBuilder.createFakeBlock(blockStore, tx2).storedBlock, AbstractBlockChain.NewBlockType.BEST_CHAIN, 0);
        // FIXME need a better test now that we apply the color kernel to unconfirmed
        assertTrue(future.isDone());
        assertEquals(tx2, future.get());
    }

    @Test
    public void testGetTransactionWithUnknownAssetFail() throws ExecutionException, InterruptedException {
        final ECKey myKey = new ECKey();
        scanner.receiveFromBlock(genesisTx, FakeTxBuilder.createFakeBlock(blockStore, genesisTx).storedBlock, AbstractBlockChain.NewBlockType.BEST_CHAIN, 0);
        wallet = new SmartWallet(params) {
            @Override
            public boolean isPubKeyMine(byte[] pubkey) {
                return Arrays.equals(pubkey, myKey.getPubKey());
            }
        };
        multiWallet = new TestMultiWallet(wallet);

        Transaction tx2a = new Transaction(params);
        tx2a.addOutput(Coin.ZERO, opReturnScript);
        Transaction tx2 = new Transaction(params);
        tx2.addInput(tx2a.getOutput(0));
        tx2.addOutput(Utils.makeAssetCoin(5), ScriptBuilder.createOutputScript(myKey));
        tx2.addOutput(Coin.ZERO, opReturnScript);
        StoredBlock storedBlock = FakeTxBuilder.createFakeBlock(blockStore, tx2).storedBlock;
        wallet.receiveFromBlock(tx2, storedBlock, AbstractBlockChain.NewBlockType.BEST_CHAIN, 0);
        ListenableFuture<Transaction> future = scanner.getTransactionWithKnownAssets(tx2, multiWallet, colorChain);
        assertFalse(future.isDone());
        scanner.notifyNewBestBlock(storedBlock);
        assertTrue(future.isDone());
        try {
            future.get();
            fail();
        } catch (ExecutionException ex) {
            assertEquals(SPVColorScanner.ScanningException.class, ex.getCause().getClass());
        }
        // FIXME need better test
    }

    private BloomFilter getBloomFilter() {
        return scanner.getBloomFilter(10, 1e-12, (long) (Math.random() * Long.MAX_VALUE));
    }

    @Test
    public void testSerializePending() {
        Transaction tx2 = makeTx2(privkey1);
        scanner.addPending(tx2);
        Protos.ColorScanner scannerProto = ext.serializeScanner(scanner);
        SPVColorScanner scanner1 = new SPVColorScanner(params);
        ext.deserializeScannerSPV(params, scannerProto, scanner1);
        scanner1.lock();
        assertEquals(tx2.getHash(), scanner1.getPending().keySet().iterator().next());
        scanner1.unlock();
    }
}