package org.smartcolors;

import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.CoinSelection;
import org.bitcoinj.wallet.KeyChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartcolors.core.ColorDefinition;
import org.smartcolors.core.SmartColors;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Created by devrandom on 2014-Oct-21.
 */
public class AssetCoinSelector extends DefaultCoinSelector {
    private static final Logger log = LoggerFactory.getLogger(AssetCoinSelector.class);
    protected final ColorKeyChain colorKeyChain;
    protected final ColorTrack track;

    public AssetCoinSelector(Context context, ColorKeyChain colorKeyChain, ColorTrack track) {
        super(context);
        this.colorKeyChain = colorKeyChain;
        this.track = track;
    }

    public static class AssetCoinSelection extends CoinSelection {
        private final long assetGathered;

        public AssetCoinSelection(Coin valueGathered, long assetGathered, Collection<TransactionOutput> gathered) {
            super(valueGathered, gathered);
            this.assetGathered = assetGathered;
        }
    }

    @Override
    public CoinSelection select(Coin biTarget, List<TransactionOutput> candidates) {
        throw new UnsupportedOperationException("cannot do a bitcoin select on an asset selector");
    }

    public AssetCoinSelection select(List<TransactionOutput> candidates, long target) {
        HashSet<TransactionOutput> selected = new HashSet<TransactionOutput>();
        // Sort the inputs by age*value so we get the highest "coindays" spent.
        // TODO: Consider changing the wallets internal format to track just outputs and keep them ordered.
        ArrayList<TransactionOutput> sortedOutputs = new ArrayList<TransactionOutput>(candidates);
        // When calculating the wallet balance, we may be asked to select all possible coins, if so, avoid sorting
        // them in order to improve performance.
        sortOutputs(sortedOutputs);

        // Now iterate over the sorted outputs until we have got as close to the target as possible or a little
        // bit over (excessive value will be change).
        long assetTotal = 0;
        long total = 0;
        for (TransactionOutput output : sortedOutputs) {
            if (assetTotal >= target) break;
            // Only pick chain-included transactions, or transactions that are ours and pending.
            if (!shouldSelect(output)) continue;
            selected.add(output);
            assetTotal += track.getColor(output.getOutPointFor());
            total += output.getValue().value;
        }
        // Total may be lower than target here, if the given candidates were insufficient to create to requested
        // transaction.
        return new AssetCoinSelection(Coin.valueOf(total), assetTotal, selected);
    }

    protected boolean shouldSelect(TransactionOutput output) {
        if (!super.shouldSelect(output) || !colorKeyChain.isOutputToMe(output))
            return false;
        // FIXME locking
        // FIXME what about multiple color outputs?
        return track.isColored(output.getOutPointFor());
    }

    /**
     * Flow:
     * <ul>
     * <li>Select asset inputs</li>
     * <li>Add asset change output</li>
     * <li>Add OP_RETURN output</li>
     * <li>Calculate fee / select bitcoin inputs</li>
     * </ul>
     *
     * @param wallet  wallet
     * @param req     request with a BitcoinCoinSelector
     * @throws InsufficientMoneyException
     */
    public void completeTx(MultiWallet wallet, Wallet.SendRequest req, long assetAmount) throws InsufficientMoneyException {
        checkArgument(req.coinSelector instanceof BitcoinCoinSelector, "Must provide a BitcoinCoinSelector");
        wallet.lock();
        try {
            // Calculate the amount of value we need to import.
            Coin value = Coin.ZERO;
            for (TransactionOutput output : req.tx.getOutputs()) {
                value = value.add(output.getValue());
            }

            log.info("Completing send tx with {} outputs totalling {} (not including fees)",
                    req.tx.getOutputs().size(), value.toFriendlyString());

            // If any inputs have already been added, we don't need to get their value from wallet
            Coin totalInput = Coin.ZERO;
            for (TransactionInput input : req.tx.getInputs())
                if (input.getConnectedOutput() != null)
                    totalInput = totalInput.add(input.getConnectedOutput().getValue());
                else
                    log.warn("SendRequest transaction already has inputs but we don't know how much they are worth - they will be added to fee.");
            value = value.subtract(totalInput);

            List<TransactionInput> originalInputs = new ArrayList<TransactionInput>(req.tx.getInputs());

            // We need to know if we need to add an additional fee because one of our values are smaller than 0.01 BTC
            boolean needAtLeastReferenceFee = false;
            if (req.ensureMinRequiredFee) { // min fee checking is handled later for emptyWallet
                for (TransactionOutput output : req.tx.getOutputs())
                    if (output.getValue().compareTo(Coin.CENT) < 0) {
                        if (output.getValue().compareTo(output.getMinNonDustValue()) < 0)
                            throw new Wallet.DustySendRequested();
                        needAtLeastReferenceFee = true;
                        break;
                    }
            }

            // Calculate a list of ALL potential candidates for spending and then ask a coin selector to provide us
            // with the actual outputs that'll be used to gather the required amount of value. In this way, users
            // can customize coin selection policies.
            //
            // Note that this code is poorly optimized: the spend candidates only alter when transactions in the wallet
            // change - it could be pre-calculated and held in RAM, and this is probably an optimization worth doing.
            List<TransactionOutput> candidates = wallet.calculateAllSpendCandidates(true, false);

            // Select and add requested asset
            AssetCoinSelection assetSelection = select(candidates, assetAmount);

            if (assetSelection.assetGathered < assetAmount) {
                long missing = assetAmount - assetSelection.assetGathered;
                String message = "Insufficient, missing " + missing + " " + track.getDefinition().getMetadata().get(ColorDefinition.METADATA_UNIT);
                throw new InsufficientAssetException(Coin.valueOf(missing), message);
            }
            for (TransactionOutput output : assetSelection.gathered) {
                TransactionInput input = req.tx.addInput(SmartColors.makeAssetInput(req.tx, output));
                originalInputs.add(input);
            }

            value = value.subtract(assetSelection.valueGathered);

            // Create asset change output
            if (assetSelection.assetGathered > assetAmount) {
                Script assetChangeScript = colorKeyChain.freshOutputScript(KeyChain.KeyPurpose.CHANGE);
                long assetChange = assetSelection.assetGathered - assetAmount;
                log.info("  with {} asset change", assetChange);
                Coin change = Coin.valueOf(SmartColors.addMsbdropValuePadding(assetChange, Transaction.MIN_NONDUST_OUTPUT.getValue()));
                value = value.add(change);
                TransactionOutput changeOutput = new TransactionOutput(wallet.getParams(), req.tx, change, assetChangeScript.getToAddress(wallet.getParams()));
                req.tx.addOutput(changeOutput);
            }

            if (SmartColors.ENABLE_OP_RETURN_MARKER) {
                // Add OP_RETURN output
                req.tx.addOutput(new TransactionOutput(wallet.getParams(), req.tx, Coin.ZERO, SmartColors.makeOpReturnScript().getProgram()));
            }

            CoinSelection bestCoinSelection;
            FeeCalculator.FeeCalculation feeCalculation;
            feeCalculation = FeeCalculator.calculateFee(wallet, req, value, originalInputs, needAtLeastReferenceFee, candidates);
            bestCoinSelection = feeCalculation.bestCoinSelection;
            TransactionOutput bestChangeOutput = feeCalculation.bestChangeOutput;

            for (TransactionOutput output : bestCoinSelection.gathered)
                req.tx.addInput(output);

            if (bestChangeOutput != null) {
                req.tx.addOutput(bestChangeOutput);
                log.info("  with {} change", bestChangeOutput.getValue().toFriendlyString());
            }

            // Now shuffle the outputs to obfuscate which is the change.
            if (req.shuffleOutputs)
                req.tx.shuffleOutputs();

            // Now sign the inputs, thus proving that we are entitled to redeem the connected outputs.
            if (req.signInputs) {
                wallet.signTransaction(req);
            }

            // Check size.
            int size = req.tx.bitcoinSerialize().length;
            if (size > Transaction.MAX_STANDARD_TX_SIZE)
                throw new Wallet.ExceededMaxTransactionSize();

            final Coin calculatedFee = req.tx.getFee();
            if (calculatedFee != null) {
                log.info("  with a fee of {}", calculatedFee.toFriendlyString());
            }

            // Label the transaction as being self created. We can use this later to spend its change output even before
            // the transaction is confirmed. We deliberately won't bother notifying listeners here as there's not much
            // point - the user isn't interested in a confidence transition they made themselves.
            req.tx.getConfidence(wallet.getContext()).setSource(TransactionConfidence.Source.SELF);
            // Label the transaction as being a user requested payment. This can be used to render GUI wallet
            // transaction lists more appropriately, especially when the wallet starts to generate transactions itself
            // for internal purposes.
            req.tx.setPurpose(Transaction.Purpose.USER_PAYMENT);
            // Record the exchange rate that was valid when the transaction was completed.
            req.tx.setExchangeRate(req.exchangeRate);
            req.tx.setMemo(req.memo);
            req.fee = calculatedFee;
            log.info("  completed: {}", req.tx);
        } finally {
            wallet.unlock();
        }
    }

    public static void addAssetOutput(Transaction tx, Script script, long value) {
        tx.addOutput(Coin.valueOf(SmartColors.addMsbdropValuePadding(value, Transaction.MIN_NONDUST_OUTPUT.getValue())), script);
    }
}
