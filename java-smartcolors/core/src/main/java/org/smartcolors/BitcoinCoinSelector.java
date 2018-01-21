package org.smartcolors;

import org.bitcoinj.core.Context;
import org.bitcoinj.core.TransactionOutput;

/**
 * Created by devrandom on 2014-Oct-21.
 */
public class BitcoinCoinSelector extends DefaultCoinSelector {
    protected final ColorKeyChain colorKeyChain;

    public BitcoinCoinSelector(Context context, ColorKeyChain colorKeyChain) {
        super(context);
        this.colorKeyChain = colorKeyChain;
    }

    protected boolean shouldSelect(TransactionOutput output) {
        return super.shouldSelect(output) && !colorKeyChain.isOutputToMe(output);
    }
}
