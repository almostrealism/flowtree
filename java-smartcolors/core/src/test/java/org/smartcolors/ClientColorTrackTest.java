package org.smartcolors;

import com.google.common.collect.Maps;
import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.junit.Before;
import org.junit.Test;
import org.smartcolors.core.*;
import org.smartcolors.protos.Protos;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ClientColorTrackTest {
    public static final Script EMPTY_SCRIPT = new Script(new byte[0]);
    public static final Coin ASSET_COIN_ONE = Coin.valueOf(SmartColors.addMsbdropValuePadding(1, 0));
    private NetworkParameters params;

    @Before
    public void setUp() {
        params = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
    }

    private GenesisOutPointsMerbinnerTree makeTree(TransactionOutPoint genesisOutPoint) {
        Map<TransactionOutPoint, Long> nodes = Maps.newHashMap();
        nodes.put(genesisOutPoint, 0L);
        return new GenesisOutPointsMerbinnerTree(params, nodes);
    }

    @Test
    public void serialize() throws Exception {
        Transaction genesisTx = new Transaction(params);
        genesisTx.addOutput(ASSET_COIN_ONE, new Script(new byte[0]));
        TransactionOutPoint genesisOutPoint = new TransactionOutPoint(params, 0, genesisTx);
        GenesisOutPointsMerbinnerTree outPoints = makeTree(genesisOutPoint);
        ColorDefinition def = new ColorDefinition(params, outPoints, new GenesisScriptMerbinnerTree());
        ClientColorTrack track = new ClientColorTrack(def);
        GenesisOutPointColorProof proof = new GenesisOutPointColorProof(def, genesisTx.getOutput(0).getOutPointFor());
        track.add(proof);
        assertEquals(proof.getQuantity(), (long) track.getOutputs().values().iterator().next());

        SmartwalletExtension ext = new SmartwalletExtension(params);
        Protos.ColorTrack trackp = ext.serializeTrack(track);
        ClientColorTrack track1 = new ClientColorTrack(def);
        SmartwalletExtension.deserializeTrackClient(params, trackp, track1);
        assertEquals(1, track1.getOutputs().size());
        assertEquals(proof.getQuantity(), (long) track1.getOutputs().values().iterator().next());
    }
}
