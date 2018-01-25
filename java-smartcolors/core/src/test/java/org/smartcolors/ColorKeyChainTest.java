package org.smartcolors;

import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.KeyChain;
import org.junit.Before;
import org.junit.Test;

import java.security.SecureRandom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ColorKeyChainTest {
    private ColorKeyChain colorChain;

    @Before
    public void setUp() throws Exception {
        colorChain = new ColorKeyChain(new SecureRandom(), 128, "", 0);
    }

    @Test
    public void testCurrentOutputScript() throws Exception {
        Script funds = colorChain.currentOutputScript(KeyChain.KeyPurpose.RECEIVE_FUNDS);
        Script funds1 = colorChain.currentOutputScript(KeyChain.KeyPurpose.RECEIVE_FUNDS);
        assertEquals(funds1, funds);
        Script change = colorChain.currentOutputScript(KeyChain.KeyPurpose.CHANGE);
        Script change1 = colorChain.currentOutputScript(KeyChain.KeyPurpose.CHANGE);
        assertEquals(change1, change);
        assertNotEquals(change, funds);
    }
}