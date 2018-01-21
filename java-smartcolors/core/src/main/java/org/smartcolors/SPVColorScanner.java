package org.smartcolors;

import com.google.common.base.Function;
import com.google.common.collect.*;
import com.google.common.util.concurrent.SettableFuture;
import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptChunk;
import org.bitcoinj.script.ScriptOpCodes;
import org.bitcoinj.utils.Threading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartcolors.core.ColorDefinition;
import org.smartcolors.core.SmartColors;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A blockchain and peer listener that keeps a set of color trackers updated with blockchain events.
 * <p/>
 * <p>You must call {@link org.smartcolors.AbstractColorScanner#addAllPending(org.bitcoinj.core.Wallet, java.util.Collection)} after deserializing the wallet
 * so that we pick up any pending transactions that were saved with the wallet but we didn't get to save.
 * The peer will not let us know about it again, and we will be unable to find it when its hash
 * gets into a block.
 * </p>
 */
public class SPVColorScanner extends AbstractColorScanner<SPVColorTrack> implements PeerFilterProvider, BlockChainListener {
    private static final Logger log = LoggerFactory.getLogger(SPVColorScanner.class);

    private final AbstractPeerEventListener peerEventListener;

    // Lock for bloom filter recalc.  General lock is obtained internally after FilterMerger obtains
    // this lock and the wallet lock (in any order).
    protected final ReentrantLock filterLock = Threading.lock("colorScannerFilter");
    @GuardedBy("lock")
    SetMultimap<Sha256Hash, SortedTransaction> mapBlockTx = TreeMultimap.create();

    public SPVColorScanner(NetworkParameters params) {
        super(params);
        peerEventListener = new AbstractPeerEventListener() {
            @Override
            public void onTransaction(Peer peer, Transaction t) {
                addPending(t);
            }

            @Override
            public void onPeerConnected(Peer peer, int peerCount) {
                log.info("Peer connected {}", peer);
                peer.addEventListener(this);
            }
        };
    }

    public AbstractPeerEventListener getPeerEventListener() {
        return peerEventListener;
    }

    @Override
    protected SPVColorTrack makeTrack(ColorDefinition definition) {
        return new SPVColorTrack(definition);
    }

    @Override
    public void notifyNewBestBlock(StoredBlock block) throws VerificationException {
        lock.lock();
        ArrayList<SettableFuture<Transaction>> futures;
        try {
            futures = Lists.newArrayList(unknownTransactionFutures.values());
            unknownTransactionFutures.clear();
        } finally {
            lock.unlock();
        }
        // Assume that any pending unknowns will not become known and therefore should fail
        for (SettableFuture<Transaction> future : futures) {
            future.setException(new ScanningException("could not find asset type"));
        }
    }

    @Override
    public void reorganize(StoredBlock splitPoint, List<StoredBlock> oldBlocks, List<StoredBlock> newBlocks) throws VerificationException {
        lock.lock();
        try {
            doReorganize(oldBlocks, newBlocks);
        } finally {
            lock.unlock();
        }
    }

    private void doReorganize(List<StoredBlock> oldBlocks, List<StoredBlock> newBlocks) {
        log.info("reorganize {} -> {}", newBlocks.size(), oldBlocks.size());
        // Remove transactions from old blocks
        for (SPVColorTrack track : tracks) {
            blocks:
            for (StoredBlock block : oldBlocks) {
                for (SortedTransaction tx : mapBlockTx.get(block.getHeader().getHash())) {
                    if (track.contains(tx.tx)) {
                        track.undo(tx.tx);
                        // Transactions that are topologically later are automatically removed by
                        // ColorTrack.undo, so we can break here.
                        break blocks;
                    }
                }
            }
        }

        // Add transactions from new blocks
        for (SPVColorTrack track : tracks) {
            for (StoredBlock block : newBlocks) {
                for (SortedTransaction tx : mapBlockTx.get(block.getHeader().getHash())) {
                    if (track.isTransactionRelevant(tx.tx)) {
                        track.add(tx.tx);
                    }
                }
            }
        }
    }

    @Override
    public boolean isTransactionRelevant(Transaction tx) throws ScriptException {
        log.info("isRelevant {}", tx.getHash());
        return isRelevant(tx);
    }

    @Override
    public void receiveFromBlock(Transaction tx, StoredBlock block, AbstractBlockChain.NewBlockType blockType, int relativityOffset) throws VerificationException {
        receive(tx, block, blockType, relativityOffset);
    }

    private boolean receive(Transaction tx, StoredBlock block, AbstractBlockChain.NewBlockType blockType, int relativityOffset) {
        lock.lock();
        Collection<SettableFuture<Transaction>> futures = null;
        try {
            log.info("receive {} {}", tx, relativityOffset);
            mapBlockTx.put(block.getHeader().getHash(), new SortedTransaction(tx, relativityOffset));
            if (blockType == AbstractBlockChain.NewBlockType.BEST_CHAIN) {
                for (SPVColorTrack track : tracks) {
                    if (track.isTransactionRelevant(tx)) {
                        track.add(tx);
                    }
                }
                futures = unknownTransactionFutures.removeAll(tx);
            }
        } finally {
            lock.unlock();
        }

        if (futures != null) {
            for (SettableFuture<Transaction> future : futures) {
                future.set(tx);
            }
        }
        return true;
    }

    private boolean isRelevant(Transaction tx) {
        for (TransactionOutput output : tx.getOutputs()) {
            Script script = output.getScriptPubKey();
            List<ScriptChunk> chunks = script.getChunks();
            if (chunks.size() == 2 && chunks.get(0).opcode == ScriptOpCodes.OP_RETURN && Arrays.equals(chunks.get(1).data, SPVColorTrack.SMART_ASSET_MARKER.getBytes())) {
                return true;
            }
        }

        // Try some more while our genesis points don't have OP_RETURN
        for (SPVColorTrack track : tracks) {
            if (track.isTransactionRelevant(tx)) {
                return true;
            }
        }

        log.info("not relevant");
        return false;
    }

    @Override
    public boolean notifyTransactionIsInBlock(Sha256Hash txHash, StoredBlock block, AbstractBlockChain.NewBlockType blockType, int relativityOffset) throws VerificationException {
        Transaction tx = pending.get(txHash);
        if (tx == null) {
            log.error("in block with no pending tx {} {}", txHash, tx, relativityOffset);
            return false;
        } else {
            log.info("in block {} {} {}", txHash, tx, relativityOffset);
            return receive(tx, block, blockType, relativityOffset);
        }
    }

    @Override
    public long getEarliestKeyCreationTime() {
        lock.lock();
        try {
            long creationTime = Long.MAX_VALUE;
            for (SPVColorTrack track : tracks) {
                creationTime = Math.min(creationTime, track.getCreationTime() + SmartColors.EARLIEST_FUDGE);
            }
            return creationTime;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void beginBloomFilterCalculation() {
        filterLock.lock();
    }

    @Override
    public void endBloomFilterCalculation() {
        filterLock.unlock();
    }

    @Override
    public int getBloomFilterElementCount() {
        int count = 0;
        lock.lock();
        try {
            for (SPVColorTrack track : tracks) {
                count += track.getBloomFilterElementCount();
            }
        } finally {
            lock.unlock();
        }
        return count;
    }

    @Override
    public BloomFilter getBloomFilter(int size, double falsePositiveRate, long nTweak) {
        BloomFilter filter = new BloomFilter(size, falsePositiveRate, nTweak);
        lock.lock();
        try {
            for (SPVColorTrack track : tracks) {
                track.updateBloomFilter(filter);
            }
        } finally {
            lock.unlock();
        }
        return filter;
    }

    @Override
    public boolean isRequiringUpdateAllBloomFilter() {
        return true;
    }

    void setMapBlockTx(SetMultimap<Sha256Hash, SortedTransaction> mapBlockTx) {
        this.mapBlockTx = mapBlockTx;
    }

    SetMultimap<Sha256Hash, SortedTransaction> getMapBlockTx() {
        return mapBlockTx;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[ColorScanner\n");
        Ordering<ColorTrack> ordering = Ordering.natural().onResultOf(new Function<ColorTrack, Comparable>() {
            @Nullable
            @Override
            public Comparable apply(@Nullable ColorTrack input) {
                return input.getDefinition().getHash().toString();
            }
        });
        for (ColorTrack track : ordering.immutableSortedCopy(tracks)) {
            builder.append(track.toString());
        }
        builder.append("\n]");
        return builder.toString();
    }

    /** Reset all state.  Used for blockchain rescan. */
    @Override
    public void doReset() {
        mapBlockTx.clear();
    }
}
