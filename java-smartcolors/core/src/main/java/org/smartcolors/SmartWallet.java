package org.smartcolors;

import org.apache.commons.lang3.NotImplementedException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.KeyChainGroup;

/**
 * Created by devrandom on 2015-07-13.
 */
public class SmartWallet extends Wallet {
    public SmartWallet(NetworkParameters params, KeyChainGroup group) {
        super(params, group);
    }

    public SmartWallet(NetworkParameters params) {
        super(params);
    }

    public void lock() {
        lock.lock();
        keyChainGroupLock.lock();
    }

    public void unlock() {
    		keyChainGroupLock.unlock();
        lock.unlock();
    }

    public void lockKeychain() {
    		keyChainGroupLock.lock();
    }

    public void unlockKeychain() {
    		keyChainGroupLock.unlock();
    }

    public KeyChainGroup getKeychain() {
    		throw new NotImplementedException("Need to implement getKeychain");
//        return keyChainGroup;
    }

    @Override
    public void saveNow() {
        super.saveNow();
    }

    @Override
    protected void saveLater() {
        super.saveLater();
    }

    public void doSaveLater() {
        saveLater();
    }
}
