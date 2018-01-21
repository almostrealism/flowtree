package org.smartcolors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.*;
import com.google.common.hash.HashCode;
import com.google.protobuf.ByteString;
import org.bitcoinj.core.*;
import org.bitcoinj.store.UnreadableWalletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartcolors.core.ColorDefinition;
import org.smartcolors.core.SmartColors;
import org.smartcolors.protos.Protos;

import java.io.IOException;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A wallet extension to hold items relevant to the SmartColors protocol.
 * <p/>
 * <p>{@link #setScanner(ColorScanner)} and {@link #setColorKeyChain(ColorKeyChain)} must be called before the extension can be functional.</p>
 * <p>For example:
 * <pre>
 *   WalletProtobufSerializer loader = new WalletProtobufSerializer(new WalletProtobufSerializer.WalletFactory() {
 *     public Wallet create(NetworkParameters params, KeyChainGroup keyChainGroup) {
 *       Wallet wallet = new SmartWallet(params, keyChainGroup);
 *       SmartwalletExtension extension = (SmartwalletExtension) wallet.addOrGetExistingExtension(new SmartwalletExtension(params));
 *       extension.setScanner(scanner);
 *       if (colorChain != null) {
 *         extension.setColorKeyChain(colorChain);
 *       }
 *       return wallet;
 *     }
 *   });
 *  loader.setKeyChainFactory(new ColorKeyChainFactory(new ColorKeyChainFactory.Callback() {
 *    public void onRestore(ColorKeyChain chain) {
 *      colorChain = chain;
 *    }
 *  }));
 * </pre>
 * </p>
 * Created by devrandom on 2014-Oct-17.
 */
public class SmartwalletExtension implements WalletExtension {
    private static final Logger log = LoggerFactory.getLogger(SmartwalletExtension.class);
    public static final String IDENTIFIER = "org.smartcolors";
    private final ObjectMapper mapper;

    protected ColorScanner scanner;
    protected ColorKeyChain colorKeyChain;
    private boolean useSeparateThreadForSerialization = true;

    public SmartwalletExtension(NetworkParameters params) {
        mapper = new ObjectMapper();
        Map<String, Object> values = Maps.newHashMap();
        values.put(ColorDefinition.NETWORK_ID_INJECTABLE, params.getId());
        mapper.setInjectableValues(new InjectableValues.Std(values));
    }

    @Override
    public String getWalletExtensionID() {
        return IDENTIFIER;
    }

    @Override
    public boolean isWalletExtensionMandatory() {
        return false;
    }

    public void setUseSeparateThreadForSerialization(boolean useSeparateThreadForSerialization) {
        this.useSeparateThreadForSerialization = useSeparateThreadForSerialization;
    }

    @Override
    public byte[] serializeWalletExtension() {
        final Protos.ColorScanner scannerProto;
        if (useSeparateThreadForSerialization) {
            Callable<Protos.ColorScanner> call = new Callable<Protos.ColorScanner>() {
                @Override
                public Protos.ColorScanner call() throws Exception {
                    return serializeScanner(scanner);
                }
            };
            log.warn("Using separate thread for serialization");
            ExecutorService service = SmartColors.makeSerializationService("Serialize thread");
            try {
                scannerProto = service.submit(call).get();
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            } catch (ExecutionException e) {
                throw Throwables.propagate(e);
            }
            service.shutdown();
        } else {
            scannerProto = serializeScanner(scanner);
        }
        return scannerProto.toByteArray();
    }

    Protos.ColorScanner serializeScanner(ColorScanner scanner) {
        Protos.ColorScanner.Builder scannerBuilder = Protos.ColorScanner.newBuilder();
        scanner.lock();
        try {
            for (Transaction transaction : scanner.getPending().values()) {
                scannerBuilder.addPending(ByteString.copyFrom(transaction.bitcoinSerialize()));
            }
            if (scanner instanceof SPVColorScanner) {
                return serializeSPV(scannerBuilder, (SPVColorScanner) scanner);
            } else {
                return serializeClient(scannerBuilder, (ClientColorScanner) scanner);
            }
        } finally {
            scanner.unlock();
        }
    }

    private Protos.ColorScanner serializeClient(Protos.ColorScanner.Builder scannerBuilder, ClientColorScanner scanner) {
        for (ColorTrack track : scanner.getColorTracks()) {
            scannerBuilder.addTracks(serializeTrack((ClientColorTrack) track));
        }
        return scannerBuilder.build();
    }

    private Protos.ColorScanner serializeSPV(Protos.ColorScanner.Builder scannerBuilder, SPVColorScanner scanner) {
        for (ColorTrack track : scanner.getColorTracks()) {
            scannerBuilder.addTracks(serializeTrack((SPVColorTrack) track));
        }
        for (Map.Entry<Sha256Hash, SortedTransaction> entry : scanner.getMapBlockTx().entries()) {
            scannerBuilder.addBlockToTransaction(Protos.BlockToSortedTransaction.newBuilder()
                    .setBlockHash(getHash(entry.getKey()))
                    .setTransaction(Protos.SortedTransaction.newBuilder()
                            .setTransaction(ByteString.copyFrom(entry.getValue().tx.bitcoinSerialize()))
                            .setIndex(entry.getValue().index)));
        }
        for (Transaction transaction : scanner.getPending().values()) {
            scannerBuilder.addPending(ByteString.copyFrom(transaction.bitcoinSerialize()));
        }
        return scannerBuilder.build();
    }

    Protos.ColorTrack serializeTrack(ClientColorTrack track) {
        Protos.ColorTrack.Builder trackBuilder = Protos.ColorTrack.newBuilder();
        serializeTrack(track, trackBuilder);
        return trackBuilder.build();
    }

    Protos.ColorTrack serializeTrack(SPVColorTrack track) {
        Protos.ColorTrack.Builder trackBuilder = Protos.ColorTrack.newBuilder();
        serializeTrack(track, trackBuilder);
        for (Map.Entry<TransactionOutPoint, Long> entry : track.getUnspentOutputs().entrySet()) {
            trackBuilder.addUnspentOutputs(Protos.OutPointValue.newBuilder()
                    .setHash(getHash(entry.getKey().getHash()))
                    .setIndex(entry.getKey().getIndex())
                    .setValue(entry.getValue()));
        }
        for (SortedTransaction tx : track.getTxs()) {
            trackBuilder.addTxs(Protos.SortedTransaction.newBuilder()
                    .setIndex(tx.index)
                    .setTransaction(ByteString.copyFrom(tx.tx.bitcoinSerialize())));
        }
        return trackBuilder.build();
    }

    private void serializeTrack(ColorTrack track, Protos.ColorTrack.Builder trackBuilder) {
        for (Map.Entry<TransactionOutPoint, Long> entry : track.getOutputs().entrySet()) {
            trackBuilder.addOutputs(Protos.OutPointValue.newBuilder()
                    .setHash(getHash(entry.getKey().getHash()))
                    .setIndex(entry.getKey().getIndex())
                    .setValue(entry.getValue()));
        }
        try {
            trackBuilder.setColorDefinition(Protos.ColorDefinition.newBuilder()
                            .setHash(getHash(track.getDefinition().getHash()))
                            .setJson(mapper.writeValueAsString(track.getDefinition()))
            );
        } catch (JsonProcessingException e) {
            Throwables.propagate(e);
        }
    }

    private static ByteString getHash(HashCode hash) {
        return ByteString.copyFrom(hash.asBytes());
    }

    private static ByteString getHash(Sha256Hash hash) {
        return ByteString.copyFrom(hash.getBytes());
    }

    @Override
    public void deserializeWalletExtension(Wallet wallet, byte[] data) throws Exception {
        Preconditions.checkArgument(wallet instanceof SmartWallet, "must use derived wallet class SmartWallet");
        Protos.ColorScanner proto = Protos.ColorScanner.parseFrom(data);
        deserializeScanner(wallet.getParams(), proto, scanner);
    }

    private void deserializeScanner(NetworkParameters params, Protos.ColorScanner proto, ColorScanner scanner) throws UnreadableWalletException {
        if (scanner instanceof SPVColorScanner) {
            deserializeScannerSPV(params, proto, (SPVColorScanner) scanner);
        } else if (scanner instanceof ClientColorScanner) {
            deserializeScannerClient(params, proto, (ClientColorScanner) scanner);
        } else {
            throw new UnsupportedOperationException("unknown scanner type");
        }
    }

    void deserializeScannerSPV(NetworkParameters params, Protos.ColorScanner proto, SPVColorScanner scanner) {
        SetMultimap<Sha256Hash, SortedTransaction> mapBlockTx = TreeMultimap.create();
        for (Protos.BlockToSortedTransaction bstxp : proto.getBlockToTransactionList()) {
            Transaction transaction = new Transaction(params, bstxp.getTransaction().getTransaction().toByteArray());
            SortedTransaction stx =
                    new SortedTransaction(transaction, bstxp.getTransaction().getIndex());
            mapBlockTx.put(getSha256Hash(bstxp.getBlockHash()), stx);
        }
        scanner.setMapBlockTx(mapBlockTx);

        for (Protos.ColorTrack trackp : proto.getTracksList()) {
            HashCode hash = getHash(trackp.getColorDefinition().getHash());
            ColorTrack track = scanner.getColorTrackByHash(hash);
            if (track == null) {
                String json = trackp.getColorDefinition().getJson();
                if (json != null) {
                    ColorDefinition def;
                    try {
                        def = mapper.readValue(json, ColorDefinition.TYPE_REFERENCE);
                    } catch (IOException e) {
                        throw Throwables.propagate(e);
                    }
                    try {
                        scanner.addDefinition(def);
                    } catch (AbstractColorScanner.ColorDefinitionException e) {
                        Throwables.propagate(e);
                    }
                    track = scanner.getColorTrackByDefinition(def);
                } else {
                    log.warn("Could not find color track {} for deserializing", hash);
                    continue;
                }
            }
            deserializeTrackSPV(params, trackp, (SPVColorTrack) track);
        }

        Map<Sha256Hash, Transaction> pending = Maps.newHashMap();
        for (ByteString bytes : proto.getPendingList()) {
            Transaction tx = new Transaction(params, bytes.toByteArray());
            pending.put(tx.getHash(), tx);
        }
        scanner.setPending(pending);
    }

    void deserializeScannerClient(NetworkParameters params, Protos.ColorScanner proto, ClientColorScanner scanner) throws UnreadableWalletException {
        for (Protos.ColorTrack trackp : proto.getTracksList()) {
            HashCode hash = getHash(trackp.getColorDefinition().getHash());
            ColorTrack track = scanner.getColorTrackByHash(hash);
            if (track == null) {
                String json = trackp.getColorDefinition().getJson();
                if (json != null) {
                    ColorDefinition def;
                    try {
                        def = mapper.readValue(json, ColorDefinition.TYPE_REFERENCE);
                    } catch (IOException e) {
                        throw Throwables.propagate(e);
                    }
                    try {
                        scanner.addDefinition(def);
                    } catch (AbstractColorScanner.ColorDefinitionException e) {
                        Throwables.propagate(e);
                    }
                    track = scanner.getColorTrackByDefinition(def);
                } else {
                    log.warn("Could not find color track {} for deserializing", hash);
                    continue;
                }
            }
            deserializeTrackClient(params, trackp, (ClientColorTrack) track);
        }

        Map<Sha256Hash, Transaction> pending = Maps.newHashMap();
        for (ByteString bytes : proto.getPendingList()) {
            Transaction tx = new Transaction(params, bytes.toByteArray());
            pending.put(tx.getHash(), tx);
        }
        scanner.setPending(pending);
    }

    static void deserializeTrackSPV(NetworkParameters params, Protos.ColorTrack trackp, SPVColorTrack track) {
        deserializeTrack(params, trackp, track);
        Map<TransactionOutPoint, Long> unspentOutputs = Maps.newHashMap();
        for (Protos.OutPointValue outp : trackp.getUnspentOutputsList()) {
            TransactionOutPoint out = new TransactionOutPoint(params, outp.getIndex(), getSha256Hash(outp.getHash()));
            unspentOutputs.put(out, outp.getValue());
        }
        track.setUnspentOutputs(unspentOutputs);
        TreeSet<SortedTransaction> txs = Sets.newTreeSet();
        for (Protos.SortedTransaction stxp : trackp.getTxsList()) {
            Transaction transaction = new Transaction(params, stxp.getTransaction().toByteArray());
            SortedTransaction tx = new SortedTransaction(transaction, stxp.getIndex());
            txs.add(tx);
        }
        track.setTxs(txs);
    }

    private static void deserializeTrack(NetworkParameters params, Protos.ColorTrack trackp, ColorTrack track) {
        Map<TransactionOutPoint, Long> outputs = Maps.newHashMap();
        for (Protos.OutPointValue outp : trackp.getOutputsList()) {
            TransactionOutPoint out = new TransactionOutPoint(params, outp.getIndex(), getSha256Hash(outp.getHash()));
            outputs.put(out, outp.getValue());
        }
        track.setOutputs(outputs);
    }

    static void deserializeTrackClient(NetworkParameters params, Protos.ColorTrack trackp, ClientColorTrack track) throws UnreadableWalletException {
        deserializeTrack(params, trackp, track);
    }

    static private Sha256Hash getSha256Hash(ByteString hash) {
        return Sha256Hash.wrap(hash.toByteArray());
    }

    static private HashCode getHash(ByteString hash) {
        return HashCode.fromBytes(hash.toByteArray());
    }

    public void setScanner(ColorScanner scanner) {
        checkNotNull(scanner);
        this.scanner = scanner;
    }

    public ColorScanner getScanner() {
        checkNotNull(scanner);
        return scanner;
    }

    public void setColorKeyChain(ColorKeyChain colorKeyChain) {
        this.colorKeyChain = colorKeyChain;
    }

    public ColorKeyChain getColorKeyChain() {
        return colorKeyChain;
    }
}
