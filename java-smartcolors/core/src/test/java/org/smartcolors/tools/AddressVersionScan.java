package org.smartcolors.tools;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;

/**
 * Created by devrandom on 2014-Oct-24.
 */
public class AddressVersionScan {
    public static void main(String[] args) {
        for (int i = 0; i < 256; i++) {
            final int ver = i;
            NetworkParameters param = new MainNetParams() {
                @Override
                public int getAddressHeader() {
                    return ver;
                }
            };

            System.out.println(i);

            for (int j = 0; j < 20; j++) {
                Address address = new Address(param, new ECKey().getPubKeyHash());
                System.out.println("  " + address);
            }
        }
    }
}
