package org.smartcolors;

import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.CoinSelection;
import org.bitcoinj.wallet.CoinSelector;
import org.bitcoinj.wallet.SendRequest;

import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Created by devrandom on 2014-Oct-21.
 */
public class FeeCalculator {
    /* Copied from Wallet class */
    public static class FeeCalculation {
        public CoinSelection bestCoinSelection;
        public TransactionOutput bestChangeOutput;
    }

    public static FeeCalculation calculateFee(MultiWallet wallet, SendRequest req, Coin value, List<TransactionInput> originalInputs,
                                              boolean needAtLeastReferenceFee, List<TransactionOutput> candidates) throws InsufficientMoneyException {
        FeeCalculation result = new FeeCalculation();
        // There are 3 possibilities for what adding change might do:
        // 1) No effect
        // 2) Causes increase in fee (change < 0.01 COINS)
        // 3) Causes the transaction to have a dust output or change < fee increase (ie change will be thrown away)
        // If we get either of the last 2, we keep note of what the inputs looked like at the time and try to
        // add inputs as we go up the list (keeping track of minimum inputs for each category).  At the end, we pick
        // the best input set as the one which generates the lowest total fee.
        Coin additionalValueForNextCategory = null;
        CoinSelection selection3 = null;
        CoinSelection selection2 = null;
        TransactionOutput selection2Change = null;
        CoinSelection selection1 = null;
        TransactionOutput selection1Change = null;
        // We keep track of the last size of the transaction we calculated but only if the act of adding inputs and
        // change resulted in the size crossing a 1000 byte boundary. Otherwise it stays at zero.
        int lastCalculatedSize = 0;
        Coin valueNeeded, valueMissing = null;
        while (true) {
            resetTxInputs(req, originalInputs);

            Coin fees = Coin.ZERO;
            
            if (lastCalculatedSize > 0) {
                // If the size is exactly 1000 bytes then we'll over-pay, but this should be rare.
                fees = fees.add(req.feePerKb.multiply((lastCalculatedSize / 1000) + 1));
            } else {
                fees = fees.add(req.feePerKb);  // First time around the loop.
            }
            
            if (needAtLeastReferenceFee && fees.compareTo(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE) < 0)
                fees = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;

            valueNeeded = value.add(fees);
            if (additionalValueForNextCategory != null)
                valueNeeded = valueNeeded.add(additionalValueForNextCategory);
            Coin additionalValueSelected = additionalValueForNextCategory;

            // Of the coins we could spend, pick some that we actually will spend.
            CoinSelector selector = req.coinSelector;
            // selector is allowed to modify candidates list.
            CoinSelection selection = selector.select(valueNeeded, new LinkedList<TransactionOutput>(candidates));
            // Can we afford this?
            if (selection.valueGathered.compareTo(valueNeeded) < 0) {
                valueMissing = valueNeeded.subtract(selection.valueGathered);
                break;
            }
            checkState(selection.gathered.size() > 0 || originalInputs.size() > 0);

            // We keep track of an upper bound on transaction size to calculate fees that need to be added.
            // Note that the difference between the upper bound and lower bound is usually small enough that it
            // will be very rare that we pay a fee we do not need to.
            //
            // We can't be sure a selection is valid until we check fee per kb at the end, so we just store
            // them here temporarily.
            boolean eitherCategory2Or3 = false;
            boolean isCategory3 = false;

            Coin change = selection.valueGathered.subtract(valueNeeded);
            if (additionalValueSelected != null)
                change = change.add(additionalValueSelected);

            // If change is < 0.01 BTC, we will need to have at least minfee to be accepted by the network
            if (req.ensureMinRequiredFee && !change.equals(Coin.ZERO) &&
                    change.compareTo(Coin.CENT) < 0 && fees.compareTo(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE) < 0) {
                // This solution may fit into category 2, but it may also be category 3, we'll check that later
                eitherCategory2Or3 = true;
                additionalValueForNextCategory = Coin.CENT;
                // If the change is smaller than the fee we want to add, this will be negative
                change = change.subtract(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE.subtract(fees));
            }

            int size = 0;
            TransactionOutput changeOutput = null;
            if (change.signum() > 0) {
                // The value of the inputs is greater than what we want to send. Just like in real life then,
                // we need to take back some coins ... this is called "change". Add another output that sends the change
                // back to us. The address comes either from the request or getChangeAddress() as a default.
                Address changeAddress = req.changeAddress;
                if (changeAddress == null)
                    changeAddress = wallet.getChangeAddress();
                changeOutput = new TransactionOutput(wallet.getParams(), req.tx, change, changeAddress);
                // If the change output would result in this transaction being rejected as dust, just drop the change and make it a fee
                if (req.ensureMinRequiredFee && Transaction.MIN_NONDUST_OUTPUT.compareTo(change) >= 0) {
                    // This solution definitely fits in category 3
                    isCategory3 = true;
                    additionalValueForNextCategory = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE.add(
                            Transaction.MIN_NONDUST_OUTPUT.add(Coin.SATOSHI));
                } else {
                    size += changeOutput.bitcoinSerialize().length + VarInt.sizeOf(req.tx.getOutputs().size()) - VarInt.sizeOf(req.tx.getOutputs().size() - 1);
                    // This solution is either category 1 or 2
                    if (!eitherCategory2Or3) // must be category 1
                        additionalValueForNextCategory = null;
                }
            } else {
                if (eitherCategory2Or3) {
                    // This solution definitely fits in category 3 (we threw away change because it was smaller than MIN_TX_FEE)
                    isCategory3 = true;
                    additionalValueForNextCategory = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE.add(Coin.SATOSHI);
                }
            }

            // Now add unsigned inputs for the selected coins.
            for (TransactionOutput output : selection.gathered) {
                TransactionInput input = req.tx.addInput(output);
                // If the scriptBytes don't default to none, our size calculations will be thrown off.
                checkState(input.getScriptBytes().length == 0);
            }

            // Estimate transaction size and loop again if we need more fee per kb. The serialized tx doesn't
            // include things we haven't added yet like input signatures/scripts or the change output.
            size += req.tx.bitcoinSerialize().length;
            size += estimateBytesForSigning(wallet, selection, originalInputs);

            if (size / 1000 > lastCalculatedSize / 1000 && req.feePerKb.signum() > 0) {
                lastCalculatedSize = size;
                // We need more fees anyway, just try again with the same additional value
                additionalValueForNextCategory = additionalValueSelected;
                continue;
            }

            if (isCategory3) {
                if (selection3 == null)
                    selection3 = selection;
            } else if (eitherCategory2Or3) {
                // If we are in selection2, we will require at least CENT additional. If we do that, there is no way
                // we can end up back here because CENT additional will always get us to 1
                checkState(selection2 == null);
                checkState(additionalValueForNextCategory.equals(Coin.CENT));
                selection2 = selection;
                selection2Change = checkNotNull(changeOutput); // If we get no change in category 2, we are actually in category 3
            } else {
                // Once we get a category 1 (change kept), we should break out of the loop because we can't do better
                checkState(selection1 == null);
                checkState(additionalValueForNextCategory == null);
                selection1 = selection;
                selection1Change = changeOutput;
            }

            if (additionalValueForNextCategory != null) {
                if (additionalValueSelected != null)
                    checkState(additionalValueForNextCategory.compareTo(additionalValueSelected) > 0);
                continue;
            }
            break;
        }

        resetTxInputs(req, originalInputs);

        if (selection3 == null && selection2 == null && selection1 == null) {
            checkNotNull(valueMissing);
            throw new InsufficientMoneyException(valueMissing);
        }

        Coin lowestFee = null;
        result.bestCoinSelection = null;
        result.bestChangeOutput = null;
        if (selection1 != null) {
            if (selection1Change != null)
                lowestFee = selection1.valueGathered.subtract(selection1Change.getValue());
            else
                lowestFee = selection1.valueGathered;
            result.bestCoinSelection = selection1;
            result.bestChangeOutput = selection1Change;
        }

        if (selection2 != null) {
            Coin fee = selection2.valueGathered.subtract(checkNotNull(selection2Change).getValue());
            if (lowestFee == null || fee.compareTo(lowestFee) < 0) {
                lowestFee = fee;
                result.bestCoinSelection = selection2;
                result.bestChangeOutput = selection2Change;
            }
        }

        if (selection3 != null) {
            if (lowestFee == null || selection3.valueGathered.compareTo(lowestFee) < 0) {
                result.bestCoinSelection = selection3;
                result.bestChangeOutput = null;
            }
        }
        return result;
    }

    private static void resetTxInputs(SendRequest req, List<TransactionInput> originalInputs) {
        req.tx.clearInputs();
        for (TransactionInput input : originalInputs)
            req.tx.addInput(input);
    }

    private static int estimateBytesForSigning(MultiWallet wallet, CoinSelection selection, List<TransactionInput> originalInputs) {
        int size = 0;
        for (TransactionOutput output : selection.gathered) {
            size += estimateBytesForSigningOutput(wallet, output);
        }
        for (TransactionInput input : originalInputs) {
            TransactionOutput output = input.getConnectedOutput();
            if (output == null) continue;
            size += estimateBytesForSigningOutput(wallet, output);
        }
        return size;
    }

    private static int estimateBytesForSigningOutput(MultiWallet wallet, TransactionOutput output) {
        try {
            Script script = output.getScriptPubKey();
            ECKey key = null;
            Script redeemScript = null;
            if (script.isSentToAddress()) {
                key = wallet.findKeyFromPubHash(script.getPubKeyHash());
                checkNotNull(key, "Coin selection includes unspendable outputs");
            } else if (script.isPayToScriptHash()) {
                redeemScript = wallet.findRedeemDataFromScriptHash(script.getPubKeyHash()).redeemScript;
                checkNotNull(redeemScript, "Coin selection includes unspendable outputs");
            }
            return script.getNumberOfBytesRequiredToSpend(key, redeemScript);
        } catch (ScriptException e) {
            // If this happens it means an output script in a wallet tx could not be understood. That should never
            // happen, if it does it means the wallet has got into an inconsistent state.
            throw new IllegalStateException(e);
        }
    }
}
