package org.smartcolors;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;

/**
 * Created by android on 1/15/15.
 */
public class InsufficientAssetException extends InsufficientMoneyException {
    InsufficientAssetException(Coin missing, String message) {
        super(missing, message);
    }
}
