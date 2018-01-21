package org.smartcolors;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.bitcoinj.core.*;
import org.smartcolors.core.ColorDefinition;
import org.smartcolors.core.SmartColors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * Track colored outputs for one definition
 * <p/>
 * <p>Tracks transactions required to prove all relevant color moves back
 * to the genesis points. Also manages updates to the track as
 * blocks/transactions are added/removed.
 * <p>Also used to update bloom filters for SPV scanning
 */
public class SPVColorTrack extends ColorTrack {
    public static final String SMART_ASSET_MARKER = "SMARTASS";
    private Map<TransactionOutPoint, Long> unspentOutputs;
    private TreeSet<SortedTransaction> txs;

    public SPVColorTrack(ColorDefinition definition) {
        super(definition);
        unspentOutputs = Maps.newHashMap();
        txs = Sets.newTreeSet();
    }

    /**
     * A hash covering all of the color track state, including definition hash, outpoints with values and
     * unspent outpoints with values.
     */
    @Override
    public Sha256Hash getStateHash() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            //bos.write(definition.getHash().getBytes());
            for (TransactionOutPoint point : outputOrdering.immutableSortedCopy(outputs.keySet())) {
                bos.write(point.bitcoinSerialize());
                Utils.uint32ToByteStreamLE(outputs.get(point), bos);
            }
            bos.write(new byte[1]);
            for (TransactionOutPoint point : outputOrdering.immutableSortedCopy(unspentOutputs.keySet())) {
                bos.write(point.bitcoinSerialize());
                Utils.uint32ToByteStreamLE(unspentOutputs.get(point), bos);
            }
            bos.write(new byte[1]);
            for (SortedTransaction tx : txs) {
                bos.write(tx.tx.getHash().getBytes());
                bos.write(new VarInt(tx.index).encode());
            }
        } catch (IOException e) {
            Throwables.propagate(e);
        }
        return Sha256Hash.of(bos.toByteArray());
    }

    /**
     * Add a new transaction to the track.  outputs and unspentOutputs will be updated.
     * <p/>
     * <p>Assumes that double spends have already been checked for</p>
     * <p>Assumes transactions are provided in topological order, i.e. all of the relevant input
     * transactions are added before the transaction itself.</p>
     */
    @Override
    public void add(Transaction tx) {
        int numOutputs = tx.getOutputs().size();
        // Check that the tx is being added in topological order - i.e. that none of its outputs
        // are spent by transactions already added.
        for (int i = 0; i < numOutputs; i++) {
            checkState(!outputs.containsKey(tx.getOutput(i).getOutPointFor()));
        }
        // Add genesis outpoints to the output maps
        for (int i = 0; i < numOutputs; i++) {
            if (definition.contains(tx.getOutput(i).getOutPointFor())) {
                long qty = SmartColors.removeMsbdropValuePadding(tx.getOutput(i).getValue().value);
                outputs.put(tx.getOutput(i).getOutPointFor(), qty);
                unspentOutputs.put(tx.getOutput(i).getOutPointFor(), qty);
            }
        }

        Long[] colorOut = applyKernel(tx);

        for (int i = 0; i < colorOut.length; i++) {
            if (colorOut[i] != null) {
                TransactionOutPoint outPoint = new TransactionOutPoint(tx.getParams(), i, tx);
                outputs.put(outPoint, colorOut[i]);
                unspentOutputs.put(outPoint, colorOut[i]);
            }
        }

        // Remove spent
        for (TransactionInput input : tx.getInputs()) {
            unspentOutputs.remove(input.getOutpoint());
        }
        txs.add(new SortedTransaction(tx, txs.size()));
    }

    public boolean contains(Transaction tx) {
        return txs.contains(new SortedTransaction(tx, 0));
    }

    /** Undoes all adds at and after the transaction */
    public void undo(Transaction tx) {
        checkArgument(txs.contains(new SortedTransaction(tx, 0)));
        while (true) {
            if (tx.equals(undoLast()))
                break;
        }
    }

    /** Undoes the last add transaction */
    public Transaction undoLast() {
        Transaction tx = txs.pollLast().tx;
        int numInputs = tx.getInputs().size();
        int numOutputs = tx.getOutputs().size();
        for (int i = 0; i < numOutputs; i++) {
            TransactionOutPoint point = new TransactionOutPoint(tx.getParams(), i, tx);
            if (outputs.containsKey(point)) {
                outputs.remove(point);
                unspentOutputs.remove(point);
            }
        }
        for (int i = 0; i < numInputs; i++) {
            TransactionOutPoint point = tx.getInput(i).getOutpoint();
            if (outputs.containsKey(point)) {
                unspentOutputs.put(point, outputs.get(point));
            }
        }
        return tx;
    }

    public Map<TransactionOutPoint, Long> getUnspentOutputs() {
        return unspentOutputs;
    }

    /** Whether the transaction spends any of our color, or has a genesis point */
    public boolean isTransactionRelevant(Transaction tx) {
        // Spends any of our color?
        for (TransactionInput input : tx.getInputs()) {
            if (unspentOutputs.containsKey(input.getOutpoint())) {
                return true;
            }
        }
        int numOutputs = tx.getOutputs().size();
        // Contains a genesis point?
        for (int i = 0; i < numOutputs; i++) {
            if (definition.contains(tx.getOutput(i).getOutPointFor())) {
                return true;
            }
        }
        return false;
    }

    /** The creation time of our color definition, so we know where to SPV scan from */
    public long getCreationTime() {
        return definition.getCreationTime();
    }

    /** The number of items in the bloom filter this tracker contributes */
    public int getBloomFilterElementCount() {
        return 1;
    }

    public static byte[] getBloomFilterElement() {
        return SMART_ASSET_MARKER.getBytes();
    }

    /** Insert outpoints and genesis points in the bloom filter */
    public void updateBloomFilter(BloomFilter filter) {
        filter.insert(getBloomFilterElement());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[ColorProof");
        builder.append(" name=" + definition.getName() + " hash=" + definition.getHash());
        builder.append("\n State hash: ");
        builder.append(getStateHash());
        builder.append("\n All:\n");

        for (TransactionOutPoint point : outputOrdering.immutableSortedCopy(outputs.keySet())) {
            builder.append("  ");
            builder.append(point.toString());
            builder.append(" = ");
            builder.append(outputs.get(point));
            builder.append("\n");
        }
        builder.append("\nUnspent:\n");
        for (TransactionOutPoint point : outputOrdering.immutableSortedCopy(unspentOutputs.keySet())) {
            builder.append("  ");
            builder.append(point.toString());
            builder.append(" = ");
            builder.append(unspentOutputs.get(point));
            builder.append("\n");
        }
        builder.append("]\n");
        return builder.toString();
    }

    @Override
    public void reset() {
        super.reset();
        unspentOutputs.clear();
        txs.clear();
    }

    void setUnspentOutputs(Map<TransactionOutPoint, Long> unspentOutputs) {
        this.unspentOutputs = unspentOutputs;
    }

    void setTxs(TreeSet<SortedTransaction> txs) {
        this.txs = txs;
    }

    TreeSet<SortedTransaction> getTxs() {
        return txs;
    }

}
