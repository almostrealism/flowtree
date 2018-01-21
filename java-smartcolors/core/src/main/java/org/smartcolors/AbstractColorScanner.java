package org.smartcolors;

import com.google.common.collect.*;
import com.google.common.hash.HashCode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.bitcoinj.core.*;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.WalletTransaction;
import org.smartcolors.core.ColorDefinition;
import org.smartcolors.core.SmartColors;

import javax.annotation.concurrent.GuardedBy;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkState;

/**
 * Created by devrandom on 2014-Nov-23.
 */
public abstract class AbstractColorScanner<TRACK extends ColorTrack> implements ColorScanner {
    protected final NetworkParameters params;
    protected Set<TRACK> tracks = Sets.newHashSet();
    protected final ColorDefinition unknownDefinition;
    protected final ColorDefinition bitcoinDefinition;
    // General lock.  Wallet lock is internally obtained first for any wallet related work.
    protected final ReentrantLock lock = Threading.lock("colorScanner");
    @GuardedBy("lock")
    protected Multimap<Transaction, SettableFuture<Transaction>> unknownTransactionFutures = ArrayListMultimap.create();
    @GuardedBy("lock")
    Map<Sha256Hash, Transaction> pending = Maps.newConcurrentMap();
    protected ColorKeyChain colorKeyChain;

    public AbstractColorScanner(NetworkParameters params) {
        this.bitcoinDefinition = ColorDefinition.makeBitcoin(params);
        this.unknownDefinition = ColorDefinition.makeUnknown(params);
        this.params = params;
    }

    @Override
    public void setColorKeyChain(ColorKeyChain colorKeyChain) {
        this.colorKeyChain = colorKeyChain;
    }

    @Override
    public ColorDefinition getBitcoinDefinition() {
        return bitcoinDefinition;
    }

    @Override
    public ColorDefinition getUnknownDefinition() {
        return unknownDefinition;
    }

    /** Add a color to the set of tracked colors */
    @Override
    public void addDefinition(ColorDefinition definition) throws ColorDefinitionExists, ColorDefinitionOutdated {
        boolean exists = false;
        if (exists) {
            throw new ColorDefinitionExists();
        }

        boolean outdated = false;
        if (outdated) {
            throw new ColorDefinitionOutdated();
        }

        lock.lock();
        try {
            tracks.add(makeTrack(definition));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<ListenableFuture<Transaction>> rescanUnknown(MultiWallet wallet, ColorKeyChain colorKeyChain) {
        return Lists.newArrayList();
    }

    /** Add a pending transaction from a peer or outgoing from us */
    @Override
    public void addPending(Transaction t) {
        pending.put(t.getHash(), t);
    }

    @Override
    public void start(MultiWallet wallet) {
        addAllPending(wallet, wallet.getTransactionPool(WalletTransaction.Pool.PENDING).values());
    }

    protected abstract TRACK makeTrack(ColorDefinition definition);

    @Override
    public ColorDefinition getColorDefinitionByHash(HashCode hash) {
        for (ColorDefinition def : getDefinitions()) {
            if (def.getHash().equals(hash))
                return def;
        }
        return null;
    }

    @Override
    public boolean removeDefinition(ColorDefinition def) {
        HashCode hash = def.getHash();
        ColorTrack track = getColorTrackByHash(hash);
        return tracks.remove(track);
    }

    @Override
    public ColorTrack getColorTrackByHash(HashCode hash) {
        for (ColorTrack track : tracks) {
            if (track.getDefinition().getHash().equals(hash))
                return track;
        }
        return null;
    }

    @Override
    public ColorTrack getColorTrackByDefinition(ColorDefinition def) {
        for (ColorTrack track : tracks) {
            if (track.getDefinition().equals(def))
                return track;
        }
        return null;
    }

    /**
     * Get the net movement of assets caused by the transaction.
     * <p/>
     * <p>If we notice an output that is marked as carrying color, but we don't know what asset
     * it is, it will be marked as UNKNOWN</p>
     */
    @Override
    public Map<ColorDefinition, Long> getNetAssetChange(Transaction tx, MultiWallet wallet, ColorKeyChain chain) {
        wallet.lock();
        try {
            Map<ColorDefinition, Long> res = Maps.newHashMap();
            applyNetAssetChange(tx, wallet, chain, res);
            return res;
        } finally {
            wallet.unlock();
        }
    }

    private void applyNetAssetChange(Transaction tx, MultiWallet wallet, ColorKeyChain chain, Map<ColorDefinition, Long> res) {
        lock.lock();
        try {
            for (TransactionOutput out : tx.getOutputs()) {
                if (chain.isOutputToMe(out)) {
                    applyOutputValue(out, res);
                }
            }
            inps:
            for (TransactionInput inp : tx.getInputs()) {
                if (SmartColors.isInputMine(inp, wallet)) {
                    for (ColorTrack track : tracks) {
                        Long value = track.getOutputs().get(inp.getOutpoint());
                        if (value != null) {
                            Long existing = res.get(track.getDefinition());
                            if (existing == null)
                                existing = 0L;
                            res.put(track.getDefinition(), existing - value);
                            continue inps;
                        }
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Map<ColorDefinition, Long> getOutputValues(Transaction tx, Wallet _wallet, ColorKeyChain chain) {
        SmartWallet wallet = (SmartWallet) _wallet;
        wallet.lock();
        lock.lock();
        try {
            Map<ColorDefinition, Long> res = Maps.newHashMap();
            for (TransactionOutput out : tx.getOutputs()) {
                if (out.isAvailableForSpending() && chain.isOutputToMe(out)) {
                    applyOutputValue(out, res);
                }
            }
            return res;
        } finally {
            lock.unlock();
            wallet.unlock();
        }
    }

    public Map<ColorDefinition, Long> getOutputValue(TransactionOutput output, Wallet _wallet) {
        SmartWallet wallet = (SmartWallet) _wallet;
        wallet.lock();
        lock.lock();
        try {
            Map<ColorDefinition, Long> res = Maps.newHashMap();
            applyOutputValue(output, res, false);
            return res;
        } finally {
            lock.unlock();
            wallet.unlock();
        }
    }

    public Map<ColorDefinition, Long> getInputValue(TransactionInput input, Wallet _wallet) {
        SmartWallet wallet = (SmartWallet) _wallet;
        wallet.lock();
        lock.lock();
        try {
            Map<ColorDefinition, Long> res = Maps.newHashMap();
            for (ColorTrack track : tracks) {
                TransactionOutPoint point = input.getOutpoint();
                Long value = track.getOutputs().get(point);
                if (value != null) {
                    res.put(track.getDefinition(), value);
                }
            }
            return res;
        } finally {
            lock.unlock();
            wallet.unlock();
        }
    }

    @Override
    public boolean contains(TransactionOutPoint point) {
        lock.lock();
        try {
            for (ColorTrack track : tracks) {
                Long value = track.getOutputs().get(point);
                if (value != null)
                    return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    protected boolean applyOutputValue(TransactionOutput out, Map<ColorDefinition, Long> res) {
        return applyOutputValue(out, res, true);
    }

    protected boolean applyOutputValue(TransactionOutput out, Map<ColorDefinition, Long> res, boolean useUnknown) {
        TransactionOutPoint point = out.getOutPointFor();
        for (ColorTrack track : tracks) {
            Long value = track.getOutputs().get(point);
            if (value == null) {
                // We don't know about this output yet, try applying the color kernel to figure
                // it out from the inputs.  This is likely an unconfirmed transaction.
                Long[] colorOuts = track.applyKernel(out.getParentTransaction());
                value = colorOuts[out.getIndex()];
            }
            if (value != null) {
                Long existing = res.get(track.getDefinition());
                if (existing != null)
                    value = existing + value;
                res.put(track.getDefinition(), value);
                return true;
            }
        }

        if (useUnknown) {
            // Unknown asset on this output
            Long value = SmartColors.removeMsbdropValuePadding(out.getValue().getValue());
            Long existing = res.get(unknownDefinition);
            if (existing != null)
                value = value + existing;
            res.put(unknownDefinition, value);
        }
        return false;
    }

    @Override
    public Map<ColorDefinition, Long> getBalances(MultiWallet wallet, ColorKeyChain colorKeyChain) {
        wallet.lock();
        lock.lock();
        try {
            List<TransactionOutput> all = wallet.calculateAllSpendCandidates(true, false);
            return getBalances(colorKeyChain, all);
        } finally {
            lock.unlock();
            wallet.unlock();
        }
    }

    private Map<ColorDefinition, Long> getBalances(ColorKeyChain colorKeyChain, List<TransactionOutput> all) {
        Map<ColorDefinition, Long> res = Maps.newHashMap();
        res.put(bitcoinDefinition, 0L);
        for (TransactionOutput output : all) {
            if (colorKeyChain.isOutputToMe(output))
                applyOutputValue(output, res);
            else
                res.put(bitcoinDefinition, res.get(bitcoinDefinition) + output.getValue().getValue());
        }
        return res;
    }

    /**
     * Get a future that will be ready when we make progress finding out the asset types that this
     * transaction outputs, or throw {@link org.smartcolors.SPVColorScanner.ScanningException}
     * if we were unable to ascertain some of the outputs.
     * <p/>
     * <p>The caller may have to run this again if we find one asset, but there are other unknownDefinition outputs</p>
     */
    @Override
    public ListenableFuture<Transaction> getTransactionWithKnownAssets(Transaction tx, MultiWallet wallet, ColorKeyChain chain) {
        wallet.lock();
        lock.lock();
        try {
            SettableFuture<Transaction> future = SettableFuture.create();
            if (getNetAssetChange(tx, wallet, chain).containsKey(unknownDefinition)) {
                // FIXME need to fail here right away if we are past the block where this tx appears and we are bloom filtering
                unknownTransactionFutures.put(tx, future);
            } else {
                future.set(tx);
            }
            return future;
        } finally {
            lock.unlock();
            wallet.unlock();
        }
    }

    /** wait for any unknown transactions in flight, for UI purposes */
    @Override
    public void waitForCurrentUnknownTransactions(MultiWallet wallet, ColorKeyChain chain) throws ExecutionException, InterruptedException {
        List<ListenableFuture<Transaction>> futures = Lists.newArrayList();
        wallet.lock();
        lock.lock();
        try {
            for (Transaction transaction : pending.values()) {
                futures.add(getTransactionWithKnownAssets(transaction, wallet, chain));
            }
        } finally {
            lock.unlock();
            wallet.unlock();
        }
        Futures.allAsList(futures).get();
    }

    @Override
    public Set<ColorDefinition> getDefinitions() {
        lock.lock();
        try {
            Set<ColorDefinition> colors = Sets.newHashSet();
            colors.add(bitcoinDefinition);
            for (ColorTrack track : tracks) {
                colors.add(track.getDefinition());
            }
            colors.add(unknownDefinition);
            return colors;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void reset() {
        lock.lock();
        try {
            unknownTransactionFutures.clear();
            for (ColorTrack track : tracks) {
                track.reset();
            }
            pending.clear();
            doReset();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Map<Sha256Hash, Transaction> getPending() {
        checkState(lock.isHeldByCurrentThread());
        return pending;
    }

    @Override
    public int getPendingCount() {
        lock.lock();
        try {
            return pending.size();
        } finally {
            lock.unlock();
        }
    }

    /** Call this after deserializing the wallet with any wallet pending transactions */
    protected void addAllPending(MultiWallet wallet, Collection<Transaction> txs) {
        for (Transaction tx : txs) {
            pending.put(tx.getHash(), tx);
        }
    }

    void setPending(Map<Sha256Hash, Transaction> pending) {
        this.pending = pending;
    }

    public Set<? extends ColorTrack> getColorTracks() {
        return tracks;
    }

    protected abstract void doReset();

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void unlock() {
        lock.unlock();
    }

    @Override
    public void stop() {
    }
}
