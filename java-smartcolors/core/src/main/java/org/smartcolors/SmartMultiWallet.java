package org.smartcolors;

import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.CoinSelection;
import org.bitcoinj.wallet.RedeemData;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet.BalanceType;

import java.util.List;

/**
 * Created by devrandom on 2015-Oct-19.
 */
abstract public class SmartMultiWallet implements MultiWallet {
    protected final SmartWallet wallet;

    public SmartMultiWallet(SmartWallet wallet) {
        this.wallet = wallet;
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
        return wallet.isPubKeyHashMine(pubkey);
    }

    @Override
    public boolean isPayToScriptHashMine(byte[] payToScriptHash) {
        return wallet.isPayToScriptHashMine(payToScriptHash);
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
    public List<TransactionOutput> getWalletOutputs(Transaction tx) {
        return tx.getWalletOutputs(wallet);
    }

    @Override
    public Transaction getTransaction(Sha256Hash hash) {
        return wallet.getTransaction(hash);
    }

    @Override
    public void saveLater() {
        wallet.doSaveLater();
    }

    @Override
    public Context getContext() {
        return wallet.getContext();
    }
    
    /**
     * Returns true if this output is to a key, or an address we have the keys for, in the wallet.
     */
    @Override
    public boolean isMine(TransactionOutput output) {
        try {
            Script script = output.getScriptPubKey();
            if (script.isSentToRawPubKey()) {
                byte[] pubkey = script.getPubKey();
                return isPubKeyMine(pubkey);
            } if (script.isPayToScriptHash()) {
                return isPayToScriptHashMine(script.getPubKeyHash());
            } else {
                byte[] pubkeyHash = script.getPubKeyHash();
                return isPubKeyHashMine(pubkeyHash);
            }
        } catch (ScriptException e) {
            // Just means we didn't understand the output of this transaction: ignore it.
            return false;
        }
    }

    /**
     * Returns the balance of this wallet as calculated by the provided balanceType.
     */
    @Override
    public Coin getBalance(BalanceType balanceType) {
        lock();
        try {
            if (balanceType == BalanceType.AVAILABLE || balanceType == BalanceType.AVAILABLE_SPENDABLE) {
                List<TransactionOutput> candidates = calculateAllSpendCandidates(true, balanceType == BalanceType.AVAILABLE_SPENDABLE);
                CoinSelection selection = wallet.getCoinSelector().select(NetworkParameters.MAX_MONEY, candidates);
                return selection.valueGathered;
            } else if (balanceType == BalanceType.ESTIMATED || balanceType == BalanceType.ESTIMATED_SPENDABLE) {
                List<TransactionOutput> all = calculateAllSpendCandidates(false, balanceType == BalanceType.ESTIMATED_SPENDABLE);
                Coin value = Coin.ZERO;
                for (TransactionOutput out : all) value = value.add(out.getValue());
                return value;
            } else {
                throw new AssertionError("Unknown balance type");  // Unreachable.
            }
        } finally {
            unlock();
        }
    }

    @Override
    public ECKey findKeyFromPubHash(byte[] pubKeyHash) {
        return wallet.findKeyFromPubHash(pubKeyHash);
    }

    @Override
    public RedeemData findRedeemDataFromScriptHash(byte[] pubKeyHash) {
        return wallet.findRedeemDataFromScriptHash(pubKeyHash);
    }

    @Override
    public Address getChangeAddress() {
        return wallet.getChangeAddress();
    }

    @Override
    public NetworkParameters getParams() {
        return wallet.getParams();
    }

    @Override
    public void signTransaction(SendRequest req) {
        wallet.signTransaction(req);
    }
}
