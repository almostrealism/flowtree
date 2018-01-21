package org.smartcolors;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;
import org.smartcolors.core.ColorDefinition;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Created by devrandom on 2014-Nov-23.
 */
public abstract class ColorTrack {
    protected final ColorDefinition definition;
    protected Map<TransactionOutPoint, Long> outputs;
    protected Ordering<TransactionOutPoint> outputOrdering =
            Ordering.natural().onResultOf(new Function<TransactionOutPoint, Comparable>() {
                @Nullable
                @Override
                public Comparable apply(@Nullable TransactionOutPoint input) {
                    return Sha256Hash.of(input.bitcoinSerialize());
                }
            });

    public ColorTrack(ColorDefinition definition) {
        this.definition = definition;
        outputs = Maps.newHashMap();
    }

    public abstract Sha256Hash getStateHash();

    public Long[] applyKernel(Transaction tx) {
        // Set up the input color
        Long colorIn[] = new Long[tx.getInputs().size()];
        for (int i = 0; i < colorIn.length; i++) {
            TransactionOutPoint prev = tx.getInput(i).getOutpoint();
            if (outputs.containsKey(prev))
                colorIn[i] = outputs.get(prev);
        }

        // Apply kernel and add output colors to output maps
        return definition.applyKernel(tx, colorIn);
    }

    public void add(Transaction tx) {
        Long[] colorOuts = applyKernel(tx);
        for (int i = 0; i < tx.getOutputs().size(); i++) {
            if (colorOuts[i] != null) {
                outputs.put(tx.getOutput(i).getOutPointFor(), colorOuts[i]);
            }
        }
    }

    public Map<TransactionOutPoint, Long> getOutputs() {
        return outputs;
    }

    /** Get the color value of an outpoint, regardless whether it was spent */
    public Long getColor(TransactionOutPoint point) {
        return outputs.get(point);
    }

    public ColorDefinition getDefinition() {
        return definition;
    }

    public void reset() {
        outputs.clear();
    }

    void setOutputs(Map<TransactionOutPoint, Long> outputs) {
        this.outputs = outputs;
    }

    public boolean isColored(TransactionOutPoint point) {
        return outputs.containsKey(point);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", definition.getName())
                .add("outputsSize", outputs.size())
                .toString();
    }
}
