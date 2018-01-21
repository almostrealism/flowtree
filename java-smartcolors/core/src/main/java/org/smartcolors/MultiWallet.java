package org.smartcolors;

import com.google.common.util.concurrent.ListenableFuture;
import org.bitcoinj.core.*;
import org.bitcoinj.wallet.RedeemData;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet.BalanceType;
import org.bitcoinj.wallet.WalletTransaction;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Wrap some functions of BitcoinJ's {@link Wallet} that can be implemented in a different way in non-SPV
 * situations.
 * <p/>
 * Created by devrandom on 2015-09-08.
 */
public interface MultiWallet extends TransactionBag {
    Coin getBalance(BalanceType balanceType);

    void signTransaction(SendRequest req);

    ECKey findKeyFromPubHash(byte[] pubKeyHash);

    RedeemData findRedeemDataFromScriptHash(byte[] pubKeyHash);

    Address getChangeAddress();

    interface MultiWalletEventListener {
        void onTransaction(MultiWallet wallet, Transaction tx, boolean isNewCoin);
        void onSyncState(MultiWallet wallet, boolean isSynced, long height);
    }

    void addEventListener(MultiWalletEventListener listener, Executor executor);

    boolean removeEventListener(MultiWalletEventListener listener);

    Set<Transaction> getTransactions();

    Map<Sha256Hash, Transaction> getTransactionPool(WalletTransaction.Pool pool);

    void markKeysAsUsed(Transaction tx);

    void completeTx(SendRequest req) throws InsufficientMoneyException;

    void commitTx(Transaction tx);

    List<TransactionOutput> calculateAllSpendCandidates(boolean excludeImmatureCoinbases, boolean excludeUnsignable);

    ListenableFuture<Transaction> broadcastTransaction(final Transaction tx);

    void lock();

    void unlock();

    /** Start the network layer and wait for it to come up */
    void start();

    /** Start the network layer */
    void startAsync();

    void stop();

    void stopAsync();

    /** must be called after start or startAsync */
    void awaitDownload() throws InterruptedException;

    List<TransactionOutput> getWalletOutputs(Transaction tx);

    Transaction getTransaction(Sha256Hash hash);

    void saveLater();

    Context getContext();
    
    boolean isMine(TransactionOutput output);

    int currentHeight();

    boolean isSynced();

    List<VersionMessage> getPeers();

    List<StoredBlock> getRecentBlocks(int maxBlocks);

    void resetBlockchain();

    NetworkParameters getParams();
}
