package org.smartcolors;

import org.bitcoinj.core.Sha256Hash;
import org.smartcolors.core.ColorDefinition;
import org.smartcolors.core.ColorProof;

/**
 * Created by devrandom on 2014-Nov-26.
 */
public class ClientColorTrack extends ColorTrack {
    public ClientColorTrack(ColorDefinition definition) {
        super(definition);
    }

    @Override
    public Sha256Hash getStateHash() {
        return null;
    }

    public void add(ColorProof proof) throws ColorProof.ValidationException {
        proof.validate();
        if (!proof.getDefinition().equals(definition))
            throw new ColorProof.ValidationException("proof is not for our definition - got " + proof.getDefinition() + ", expected " + definition);
        outputs.put(proof.getOutPoint(), proof.getQuantity());
    }
}
