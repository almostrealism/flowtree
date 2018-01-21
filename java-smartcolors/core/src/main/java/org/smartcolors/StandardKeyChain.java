package org.smartcolors;

import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;

import com.google.common.collect.ImmutableList;
import org.spongycastle.crypto.params.KeyParameter;

import java.security.SecureRandom;

import static com.google.common.base.Preconditions.checkState;

/**
 * Created by devrandom on 2015-07-13.
 */
public class StandardKeyChain extends DeterministicKeyChain {
    public StandardKeyChain(DeterministicSeed seed, KeyCrypter crypter) {
        super(seed, crypter);
    }

    public StandardKeyChain(DeterministicKey accountKey, boolean isFollowingKey) {
        super(accountKey, isFollowingKey);
    }

    public StandardKeyChain(DeterministicSeed decSeed) {
        super(decSeed);
    }

    public StandardKeyChain(KeyCrypter keyCrypter, KeyParameter aesKey, StandardKeyChain standardKeyChain) {
        super(keyCrypter, aesKey, standardKeyChain);
    }

    public StandardKeyChain(SecureRandom random, int bits, String passphrase, long seconds) {
        super(random, bits, passphrase, seconds);
    }

    public StandardKeyChain(byte[] entropy, String passphrase, long timeSeconds) {
        super(entropy, passphrase, timeSeconds);
    }

    @Override
    public ImmutableList<ChildNumber> getAccountPath() {
        return BIP44_ACCOUNT_ZERO_PATH;
    }

    @Override
    public DeterministicKeyChain toEncrypted(KeyCrypter keyCrypter, KeyParameter aesKey) {
        return new StandardKeyChain(keyCrypter, aesKey, this);
    }

    @Override
    protected DeterministicKeyChain makeKeyChainFromSeed(DeterministicSeed decSeed) {
        return new StandardKeyChain(decSeed);
    }

    public static class Builder<T extends Builder<T>> extends DeterministicKeyChain.Builder<T> {
        protected Builder() {
        }

        public StandardKeyChain build() {
            checkState(random != null || entropy != null || seed != null, "Must provide either entropy or random");
            checkState(passphrase == null || seed == null, "Passphrase must not be specified with seed");
            StandardKeyChain chain;
            if (random != null) {
                chain = new StandardKeyChain(random, bits, passphrase, Utils.currentTimeSeconds());
            } else if (entropy != null) {
                chain = new StandardKeyChain(entropy, passphrase, Utils.currentTimeSeconds());
            } else {
                chain = new StandardKeyChain(seed);
            }
            return chain;
        }
    }

    public static Builder<?> builder() {
        return new Builder();
    }
}
