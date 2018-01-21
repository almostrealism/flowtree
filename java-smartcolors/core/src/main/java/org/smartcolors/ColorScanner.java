package org.smartcolors;

import com.google.common.hash.HashCode;
import com.google.common.util.concurrent.ListenableFuture;
import org.bitcoinj.core.*;
import org.smartcolors.core.ColorDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Created by devrandom on 2014-Nov-23.
 */
public interface ColorScanner {
    /**
     * Add a definition.  Use {@link #rescanUnknown} to scan for any existing outputs that might
     * be colored with this definition.
     */
    void addDefinition(ColorDefinition definition) throws ColorScanner.ColorDefinitionExists, ColorScanner.ColorDefinitionOutdated;

    /**
     * Remove a definition
     *
     * @return true if we had it
     * @throws Exception
     */
    boolean removeDefinition(ColorDefinition def);

    void waitForCurrentUnknownTransactions(MultiWallet wallet, ColorKeyChain chain) throws ExecutionException, InterruptedException;

    /** Get all currently configured definitions. The definitions persist in the wallet using {@link SmartwalletExtension}. */
    Set<ColorDefinition> getDefinitions();

    /** The special definition for unknown assets */
    ColorDefinition getUnknownDefinition();

    /** The special definition for the bitcoin asset */
    ColorDefinition getBitcoinDefinition();

    /** Get the net change for each color this transaction moves */
    Map<ColorDefinition, Long> getNetAssetChange(Transaction tx, MultiWallet wallet, ColorKeyChain chain);

    /** Check if the outpoint is colored, even if the color is unknown.  Colored outpoints belong to the {@link ColorKeyChain} */
    boolean contains(TransactionOutPoint point);

    /** Get the balance for each color */
    Map<ColorDefinition, Long> getBalances(MultiWallet wallet, ColorKeyChain colorKeyChain);

    /** Get a future that triggers when the colors the transaction moves become known - i.e. after the asset tracker responds */
    ListenableFuture<Transaction> getTransactionWithKnownAssets(Transaction tx, MultiWallet wallet, ColorKeyChain chain);

    /**
     * Rescan any unknown assets.  Useful after {@link #addDefinition}.  Caller is responsible for invoking {@link #start(MultiWallet)}
     * if needed, but any pending fetches will be noted.
     */
    List<ListenableFuture<Transaction>> rescanUnknown(MultiWallet wallet, ColorKeyChain colorKeyChain);

    /** Add a transaction to the pending queue.  It will be scanned in due course. */
    void addPending(Transaction t);

    /** Find a color definition by its hash, or null if we don't have it */
    ColorDefinition getColorDefinitionByHash(HashCode hash);

    /** Find a ColorTrack by color definition hash */
    ColorTrack getColorTrackByHash(HashCode hash);

    /** Find a ColorTrack by definition */
    ColorTrack getColorTrackByDefinition(ColorDefinition def);

    /** Start the scanner - meaning that fetches will happen. */
    void start(MultiWallet wallet);

    /** Stop the scanner - meaning that fetches will be postponed. */
    void stop();

    /** Reset the scanner state.  May also stop the scanner. */
    void reset();

    /** Set the color key chain - used during loading */
    void setColorKeyChain(ColorKeyChain colorKeyChain);

    /** Get the pending transaction queue.  Caller must {@link #lock()} the object. */
    Map<Sha256Hash, Transaction> getPending();

    /** Get the number of pending transactions */
    int getPendingCount();

    /** Lock */
    void lock();

    /** Unlock */
    void unlock();

    /** Get the amount of assets in transaction outputs */
    Map<ColorDefinition, Long> getOutputValues(Transaction tx, Wallet wallet, ColorKeyChain colorChain);

    public static class ScanningException extends RuntimeException {
        public ScanningException(String reason) {
            super(reason);
        }
    }

    public class ColorDefinitionException extends Exception {
        public ColorDefinitionException(String s) {
            super(s);
        }
    }

    public class ColorDefinitionOutdated extends ColorDefinitionException {
        public ColorDefinitionOutdated() {
            super("Trying to replace an existing definition with an older one.");
        }
    }

    public class ColorDefinitionExists extends ColorDefinitionException {
        public ColorDefinitionExists() {
            super("Trying to replace an existing definition.");
        }
    }
}
