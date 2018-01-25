package org.smartcolors;

import org.bitcoinj.core.Coin;
import org.smartcolors.core.SmartColors;

import java.math.BigInteger;

public class Utils extends org.bitcoinj.core.Utils {
    public static long parseBinary(String s) {
        BigInteger v = new BigInteger(s, 2);
        if (v.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0)
            v = v.add(BigInteger.valueOf(Long.MIN_VALUE).multiply(BigInteger.valueOf(2)));
        return v.longValue();
    }

    public static long parseHex(String s) {
        BigInteger v = new BigInteger(s, 16);
        if (v.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0)
            v = v.add(BigInteger.valueOf(Long.MIN_VALUE).multiply(BigInteger.valueOf(2)));
        return v.longValue();
    }

    public static Coin makeAssetCoin(long value) {
        return Coin.valueOf(SmartColors.addMsbdropValuePadding(value, 0));
    }
}
