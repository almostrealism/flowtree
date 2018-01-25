package org.smartcolors;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.WalletTransaction;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Created by devrandom on 2015-Oct-19.
 */
public class TestMultiWallet extends SmartMultiWallet {
    private final Map<MultiWalletEventListener, WalletEventListener> listenerMap = Maps.newConcurrentMap();

    public TestMultiWallet(SmartWallet wallet) {
        super(wallet);
    }

    @Override
    public void addEventListener(final MultiWalletEventListener listener, Executor executor) {
        AbstractWalletEventListener walletListener = new AbstractWalletEventListener() {
            @Override
            public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                super.onCoinsReceived(wallet, tx, prevBalance, newBalance);
                listener.onTransaction(TestMultiWallet.this, tx, true);
            }

            @Override
            public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                // FIXME change bitcoinj so that we get this also when diff = 0
                super.onCoinsSent(wallet, tx, prevBalance, newBalance);
                listener.onTransaction(TestMultiWallet.this, tx, false);
            }

            @Override
            public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
                super.onTransactionConfidenceChanged(wallet, tx);
            }
        };
        listenerMap.put(listener, walletListener);
        wallet.addEventListener(walletListener, Threading.SAME_THREAD);
    }

    @Override
    public boolean removeEventListener(MultiWalletEventListener listener) {
        return wallet.removeEventListener(listenerMap.remove(listener));
    }


    @Override
    public Set<Transaction> getTransactions() {
        return wallet.getTransactions(false);
    }

    @Override
    public boolean isPubKeyHashMine(byte[] pubkeyHash) {
        return wallet.isPubKeyHashMine(pubkeyHash);
    }

    @Override
    public boolean isWatchedScript(Script script) {
        return wallet.isWatchedScript(script);
    }

    @Override
    public boolean isPubKeyMine(byte[] pubkey) {
        return wallet.isPubKeyMine(pubkey);
    }

    @Override
    public boolean isPayToScriptHashMine(byte[] payToScriptHash) {
        return wallet.isPayToScriptHashMine(payToScriptHash);
    }

    @Override
    public Map<Sha256Hash, Transaction> getTransactionPool(WalletTransaction.Pool pool) {
        return wallet.getTransactionPool(pool);
    }

    @Override
    public void markKeysAsUsed(Transaction tx) {
    }

    @Override
    public void completeTx(Wallet.SendRequest req) throws InsufficientMoneyException {
        wallet.completeTx(req);
    }

    @Override
    public void commitTx(Transaction tx) {
        wallet.commitTx(tx);
    }

    @Override
    public List<TransactionOutput> calculateAllSpendCandidates(boolean excludeImmatureCoinbases, boolean excludeUnsignable) {
        return wallet.calculateAllSpendCandidates(excludeImmatureCoinbases, excludeUnsignable);
    }

    @Override
    public ListenableFuture<Transaction> broadcastTransaction(Transaction tx) {
        return null;
    }

    @Override
    public void lock() {
        wallet.lock();
    }

    @Override
    public void unlock() {
        wallet.unlock();
    }

    @Override
    public void start() {
    }

    @Override
    public void startAsync() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void stopAsync() {
    }

    @Override
    public void awaitDownload() throws InterruptedException {

    }

    @Override
    public List<TransactionOutput> getWalletOutputs(Transaction tx) {
        return tx.getWalletOutputs(wallet);
    }

    @Override
    public Transaction getTransaction(Sha256Hash hash) {
        return wallet.getTransaction(hash);
    }

    @Override
    public void saveLater() {
        wallet.saveLater();
    }

    @Override
    public Context getContext() {
        return wallet.getContext();
    }

    @Override
    public int currentHeight() {
        return 0;
    }

    @Override
    public boolean isSynced() {
        return false;
    }

    @Override
    public List<VersionMessage> getPeers() {
        return null;
    }

    @Override
    public List<StoredBlock> getRecentBlocks(int maxBlocks) {
        return null;
    }

    @Override
    public void resetBlockchain() {

    }
}
