package org.smartcolors;

import com.google.common.primitives.Ints;
import org.bitcoinj.core.Transaction;

/**
 * Helper for storing transactions in toplogical order.  Two SortedTransactions are equal
 * if their transactions are equal, regardless of index.
 */
public class SortedTransaction implements Comparable<SortedTransaction> {
    public final Transaction tx;
    public final int index;

    public SortedTransaction(Transaction tx, int index) {
        this.tx = tx;
        this.index = index;
    }

    @Override
    public int compareTo(SortedTransaction o) {
        return Ints.compare(index, o.index);
    }

    @Override
    public int hashCode() {
        return tx.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this.tx.equals(obj);
    }
}
