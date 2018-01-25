package org.smartcolors.tools;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.*;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDUtils;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.store.*;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartcolors.*;
import org.smartcolors.core.*;
import org.smartcolors.marshal.SerializationException;
import org.smartwallet.multi.ElectrumMultiWallet;

import javax.annotation.Nullable;
import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class ColorTool {
    private static final Logger log = LoggerFactory.getLogger(ColorTool.class);

    private static OptionSet options;
    private static OptionParser parser;
    private static NetworkParameters params;

    private static BlockStore store;
    private static MyBlockChain chain;
    private static PeerGroup peers;
    private static SmartWallet wallet;
    private static MultiWallet multiWallet;
    private static File chainFile;
    private static File walletFile;
    private static ColorScanner scanner;
    private static File checkpointFile;
    private static ColorKeyChain colorChain;
    private static OptionSpec<String> mnemonicSpec;
    private static boolean useElectrum = false;

    public static void main(String[] args) throws IOException {
//		for (int i = 0; i < args.length; i++) {
//			System.out.println(args[i]);
//		}
        parser = new OptionParser();
        parser.accepts("prod", "use prodnet (default is testnet)");
        parser.accepts("regtest", "use regtest mode (default is testnet)");
        parser.accepts("force", "force creation of wallet from mnemonic");
        parser.accepts("electrum", "use Electrum SPV instead of native SPV");
        parser.accepts("linger", "do not exit after done");
        parser.accepts("nofetch", "do not fetch colors");
        OptionSpec<String> assetsPathSpec = parser.accepts("assets", "asset directory, containing *.smartcolors").withRequiredArg();
        parser.accepts("debug");
        parser.accepts("verbose");
        mnemonicSpec = parser.accepts("mnemonic", "mnemonic phrase").withRequiredArg();
        OptionSpec<String> walletFileNameSpec = parser.accepts("wallet").withRequiredArg();
        parser.nonOptions("COMMAND: one of:" +
                "\n help" +
                "\n scan" +
                "\n send COLOR_NAME DEST AMOUNT" +
                "\n issue COLOR_NAME DEST BASE_AMOUNT" +
                "\n getaddress" +
                "\n getassetaddress" +
                "\n");

        options = parser.parse(args);

        if (options.has("debug")) {
            BriefLogFormatter.init();
            LogManager.getLogManager().getLogger("").setLevel(Level.FINEST);
            log.info("Starting up ...");
        } else {
            // Disable logspam unless there is a flag.
            LogManager.getLogManager().getLogger("").setLevel(Level.WARNING);
        }

        List<?> cmds = options.nonOptionArguments();
        if (cmds.isEmpty())
            usage();
        String cmd = (String) cmds.get(0);
        List<?> cmdArgs = cmds.subList(1, cmds.size());

        String net;
        if (options.has("prod")) {
            net = "prodnet";
            params = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);
        } else if (options.has("regtest")) {
            net = "regtest";
            params = NetworkParameters.fromID(NetworkParameters.ID_REGTEST);
        } else {
            net = "testnet";
            params = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
        }

        if (options.has("nofetch")) {
            ClientColorScanner.setDisableFetch(true);
            log.warn("FETCH DISABLED");
        }

        useElectrum = options.has("electrum");

        checkpointFile = new File("checkpoints-" + net + ".txt");

        String walletName = walletFileNameSpec.value(options);
        if (walletName == null)
            walletName = net + ".wallet";
        chainFile = new File(walletName + ".chain");
        walletFile = new File(walletName);
        if (!walletFile.exists() || options.has(mnemonicSpec)) {
            createWallet(options, params, walletFile, assetsPathSpec);
        }

        if (readWallet()) return;

        wallet.addEventListener(new AbstractWalletEventListener() {
            @Override
            public void onCoinsReceived(final Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                ListenableFuture<Transaction> res = scanner.getTransactionWithKnownAssets(tx, multiWallet, colorChain);
                Futures.addCallback(res, new FutureCallback<Transaction>() {
                    @Override
                    public void onSuccess(@Nullable Transaction result) {
                        Map<ColorDefinition, Long> change = scanner.getNetAssetChange(result, multiWallet, colorChain);
                        System.out.println(change);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("*** failed");
                    }
                });
            }
        }, Threading.SAME_THREAD);

        if (cmd.equals("help")) {
            usage();
        } else if (cmd.equals("scan")) {
            scan(cmdArgs);
        } else if (cmd.equals("send")) {
            send(cmdArgs);
        } else if (cmd.equals("issue")) {
            issue(cmdArgs);
        } else if (cmd.equals("dump")) {
            dump(cmdArgs, false);
        } else if (cmd.equals("quickdump")) {
            dump(cmdArgs, true);
        } else if (cmd.equals("getaddress")) {
            getAddress(cmdArgs);
        } else if (cmd.equals("getassetaddress")) {
            getAssetAddress(cmdArgs);
        } else {
            usage();
        }
    }

    private static boolean readWallet() throws IOException {
        BufferedInputStream walletInputStream = null;
        try {
            makeScanner();
            colorChain = null;
            WalletProtobufSerializer loader = new WalletProtobufSerializer(new WalletProtobufSerializer.WalletFactory() {
                @Override
                public Wallet create(NetworkParameters params, KeyChainGroup keyChainGroup) {
                    Wallet wallet = new SmartWallet(params, keyChainGroup);
                    SmartwalletExtension extension = (SmartwalletExtension) wallet.addOrGetExistingExtension(new SmartwalletExtension(params));
                    extension.setScanner(scanner);
                    if (colorChain != null) {
                        extension.setColorKeyChain(colorChain);
                    }
                    return wallet;
                }
            });
            loader.setKeyChainFactory(new ColorKeyChainFactory(new ColorKeyChainFactory.Callback() {
                @Override
                public void onRestore(ColorKeyChain chain) {
                    colorChain = chain;
                }
            }));
            if (options.has("ignore-mandatory-extensions"))
                loader.setRequireMandatoryExtensions(false);
            walletInputStream = new BufferedInputStream(new FileInputStream(walletFile));
            wallet = (SmartWallet) loader.readWallet(walletInputStream);
            checkNotNull(colorChain);
            if (!wallet.getParams().equals(params)) {
                System.err.println("Wallet does not match requested network parameters: " +
                        wallet.getParams().getId() + " vs " + params.getId());
                return true;
            }
            if (wallet.getExtensions().get(SmartwalletExtension.IDENTIFIER) == null)
                throw new UnreadableWalletException("missing smartcolors extension");
            if (colorChain == null)
                throw new UnreadableWalletException("missing color keychain");
        } catch (Exception e) {
            System.err.println("Failed to load wallet '" + walletFile + "': " + e.getMessage());
            e.printStackTrace();
            return true;
        } finally {
            if (walletInputStream != null) {
                walletInputStream.close();
            }
        }
        return false;
    }

    // Sets up all objects needed for network communication but does not bring up the peers.
    private static void setup() throws BlockStoreException, IOException {
        if (store != null) return;  // Already done.

        ClientColorScanner clientScanner = (ClientColorScanner) scanner;
        clientScanner.setColorKeyChain(colorChain);

        if (useElectrum) {
            multiWallet = new ElectrumMultiWallet(wallet);
            clientScanner.start(multiWallet);
        } else {
            boolean chainExisted = chainFile.exists();
            store = new SPVBlockStore(params, chainFile);
            if (checkpointFile.exists() && !chainExisted) {
                long creationTime = Long.MAX_VALUE;
                creationTime = Math.min(creationTime, wallet.getEarliestKeyCreationTime());
                CheckpointManager.checkpoint(params, new FileInputStream(checkpointFile), store, creationTime);
            }
            chain = new MyBlockChain(params, wallet, store);

            if (peers == null) {
                peers = new PeerGroup(params, chain);
            }
            peers.setUserAgent("ColorTool", "1.0");

            multiWallet = new SPVMultiWallet(wallet, peers);
            clientScanner.start(multiWallet);

            peers.addWallet(wallet);

            if (options.has("peers")) {
                String peersFlag = (String) options.valueOf("peers");
                String[] peerAddrs = peersFlag.split(",");
                for (String peer : peerAddrs) {
                    try {
                        peers.addAddress(new PeerAddress(InetAddress.getByName(peer), params.getPort()));
                    } catch (UnknownHostException e) {
                        System.err.println("Could not understand peer domain name/IP address: " + peer + ": " + e.getMessage());
                        System.exit(1);
                    }
                }
            } else if (isRegTest()) {
                peers.addAddress(new PeerAddress(InetAddress.getLoopbackAddress(), 28883));
            } else {
                //peers.addAddress(PeerAddress.localhost(params));
                peers.addPeerDiscovery(new DnsDiscovery(params));
            }
        }

        wallet.autosaveToFile(walletFile, 200, TimeUnit.MILLISECONDS, null);
    }

    private static void reset() {
        wallet.clearTransactions(0);
        saveWallet(walletFile);
    }

    private static void saveWallet(File walletFile) {
        try {
            // This will save the new state of the wallet to a temp file then rename, in case anything goes wrong.
            wallet.saveToFile(walletFile);
        } catch (IOException e) {
            System.err.println("Failed to save wallet! Old wallet should be left untouched.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void syncChain() {
        try {
            setup();
            int startTransactions = wallet.getTransactions(true).size();
            multiWallet.start();
            try {
                multiWallet.awaitDownload();
                scanner.waitForCurrentUnknownTransactions(multiWallet, colorChain);
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Chain download interrupted, quitting ...");
                System.exit(1);
            }
            int endTransactions = wallet.getTransactions(true).size();
            if (endTransactions > startTransactions) {
                System.out.println("Synced " + (endTransactions - startTransactions) + " transactions.");
            }
            wallet.saveToFile(walletFile);
        } catch (BlockStoreException e) {
            System.err.println("Error reading block chain file " + chainFile + ": " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void createWallet(OptionSet options, NetworkParameters params, File walletFile, OptionSpec<String> assetsDirSpec) throws IOException {
        if (walletFile.exists() && !options.has("force")) {
            System.err.println("Wallet creation requested but " + walletFile + " already exists, use --force");
            System.exit(1);
        }
        if (chainFile.exists())
            chainFile.delete();

        try {
            String mnemonicCode = "correct battery horse staple bogum";
            if (options.has(mnemonicSpec))
                mnemonicCode = mnemonicSpec.value(options);
            System.out.println(mnemonicCode);
            DeterministicSeed seed = new DeterministicSeed(mnemonicCode, null, "", SmartColors.getSmartwalletEpoch(params));
            System.out.println(Utils.HEX.encode(seed.getSeedBytes()));
            colorChain =
                    ColorKeyChain.builder()
                            .seed(seed)
                            .build();
            DeterministicKeyChain chain =
                    StandardKeyChain.builder()
                            .seed(seed)
                            .build();
            KeyChainGroup group = new KeyChainGroup(params);
            group.addAndActivateHDChain(colorChain);
            group.addAndActivateHDChain(chain);
            group.setLookaheadSize(40);
            group.setLookaheadThreshold(20);
            wallet = new SmartWallet(params, group);
            SmartwalletExtension extension = new SmartwalletExtension(params);
            //extension.setColorKeyChain(colorChain);
            wallet.addOrGetExistingExtension(extension);
            makeScanner();
            if (options.has("assets")) {
                addAssetsFromDirectory(assetsDirSpec.value(options));
            } else {
                addBuiltins();
            }
            extension.setScanner(scanner);
        } catch (UnreadableWalletException e) {
            throw new RuntimeException(e);
        }
        wallet.saveToFile(walletFile);
    }

    private static void makeScanner() {
        URI baseUri = null;
        try {
            if (isRegTest())
                baseUri = new URI("http://localhost:8888/");
            else
                baseUri = new URI("http://tracker0.smartcolors.org:8888/");
        } catch (URISyntaxException e) {
            Throwables.propagate(e);
        }
        scanner = new ClientColorScanner(params, baseUri);
    }

    private static void addBuiltins() {
        try {
            scanner.addDefinition(loadDefinitionFromResource("assets/eur.smartcolor"));
            scanner.addDefinition(loadDefinitionFromResource("assets/usd.smartcolor"));
            scanner.addDefinition(loadDefinitionFromResource("assets/oil.smartcolor"));
            scanner.addDefinition(loadDefinitionFromResource("assets/gold.smartcolor"));
        } catch (SPVColorScanner.ColorDefinitionException e) {
            Throwables.propagate(e);
        }
    }

    private static void addAssetsFromDirectory(String path) {
        try {
            File dir = new File(path);
            for (File file : dir.listFiles()) {
                if (file.getPath().endsWith(".smartcolor")) {
                    scanner.addDefinition(loadDefinitionFromFile(file));
                }
            }
        } catch (SPVColorScanner.ColorDefinitionException e) {
            Throwables.propagate(e);
        }
    }

    private static ColorDefinition loadDefinitionFromResource(String path) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream(path);
        return loadDefinition(is);
    }

    private static ColorDefinition loadDefinitionFromFile(File file) {
        try {
            InputStream is = new FileInputStream(file);
            return loadDefinition(is);
        } catch (FileNotFoundException e) {
            throw Throwables.propagate(e);
        }
    }

    private static ColorDefinition loadDefinition(InputStream is) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> values = Maps.newHashMap();
        values.put(ColorDefinition.NETWORK_ID_INJECTABLE, NetworkParameters.ID_TESTNET);
        mapper.setInjectableValues(new InjectableValues.Std(values));
        try {
            return mapper.readValue(is, ColorDefinition.TYPE_REFERENCE);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private static void dump(List<?> cmdArgs, boolean quick) {
        if (!quick) syncChain();
        dumpState();
        System.out.println(wallet.currentReceiveAddress());
        done();
    }

    private static void getAddress(List<?> cmdArgs) {
        syncChain();
        System.out.println(wallet.currentReceiveAddress());
        done();
    }

    private static void getAssetAddress(List<?> cmdArgs) {
        syncChain();
        Address assetBitcoinAddress = colorChain.currentOutputScript(KeyChain.KeyPurpose.RECEIVE_FUNDS).getToAddress(params);
        System.out.println(SmartColors.toAssetAddress(assetBitcoinAddress, !isTestNet()));
        done();
    }

    private static void done() {
        if (!options.has("linger"))
            System.exit(0);
    }

    private static void send(List<?> cmdArgs) {
        syncChain();
        String name = (String) cmdArgs.get(0);
        String dest = (String) cmdArgs.get(1);
        String amountString = (String) cmdArgs.get(2);
        ColorDefinition def = null;
        for (ColorDefinition definition : scanner.getDefinitions()) {
            if (definition.getName().equalsIgnoreCase(name)) {
                def = definition;
                break;
            }
        }
        if (def == null) {
            System.err.println("unknown color");
            System.exit(1);
        }
        AssetCoinSelector assetSelector = new AssetCoinSelector(wallet.getContext(), colorChain, scanner.getColorTrackByDefinition(def));
        BigDecimal divisibilityDivider = getDivider(def);

        long amount = new BigDecimal(amountString).multiply(divisibilityDivider).intValue();
        Wallet.SendRequest req = null;
        try {
            req = makeAssetSendRequest(dest, amount);
            assetSelector.completeTx(multiWallet, req, amount);
        } catch (AddressFormatException e) {
            Throwables.propagate(e);
        } catch (InsufficientMoneyException e) {
            Throwables.propagate(e);
        }
        wallet.commitTx(req.tx);
        try {
            multiWallet.broadcastTransaction(req.tx).get();
        } catch (InterruptedException e) {
            Throwables.propagate(e);
        } catch (ExecutionException e) {
            Throwables.propagate(e);
        }
        System.out.println(req.tx);
        Utils.sleep(2000);
        done();

    }

    private static void issue(List<?> cmdArgs) {
        syncChain();
        int ind = 0;
        String name = (String) cmdArgs.get(ind++);
        String dest = cmdArgs.size() >= 3 ? (String) cmdArgs.get(ind++) : SmartColors.toAssetAddress(colorChain.freshOutputScript(KeyChain.KeyPurpose.RECEIVE_FUNDS).getToAddress(params), !isTestNet()).toString();
        String amountString = (String) cmdArgs.get(ind++);

        long amount = Long.parseLong(amountString);

        Wallet.SendRequest req = null;
        try {
            req = makeAssetSendRequest(dest, amount);
            req.shuffleOutputs = false;
            wallet.completeTx(req);
        } catch (AddressFormatException e) {
            Throwables.propagate(e);
        } catch (InsufficientMoneyException e) {
            Throwables.propagate(e);
        }
        Map<TransactionOutPoint, Long> nodes = Maps.newHashMap();
        nodes.put(req.tx.getOutput(0).getOutPointFor(), amount);
        GenesisOutPointsMerbinnerTree outPoints = new GenesisOutPointsMerbinnerTree(params, nodes);
        Map<String, String> metadata = Maps.newHashMap();
        metadata.put("name", name);
        ColorDefinition def = new ColorDefinition(params, outPoints, new GenesisScriptMerbinnerTree(), metadata, chain.getBestChainHeight() - 6, new byte[16]);
        System.out.println(req.tx);
        File jsonFile = new File(name + ".smartcolor");
        File scdefFile = new File(name + ".scdef");
        if (!options.has("force") && (jsonFile.exists() || scdefFile.exists())) {
            log.error("file exists");
            done();
            return;
        }

        try {
            OutputStream jsonOut = new FileOutputStream(jsonFile);
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(jsonOut, def);
            jsonOut.close();
            OutputStream scdefOut = new FileOutputStream(scdefFile);
            def.serializeToFile(scdefOut);
            scdefOut.close();
        } catch (IOException e) {
            Throwables.propagate(e);
        } catch (SerializationException e) {
            Throwables.propagate(e);
        }
        wallet.commitTx(req.tx);
        try {
            multiWallet.broadcastTransaction(req.tx).get();
        } catch (InterruptedException e) {
            Throwables.propagate(e);
        } catch (ExecutionException e) {
            Throwables.propagate(e);
        }
        Utils.sleep(2000);
        done();
    }

    private static BigDecimal getDivider(ColorDefinition def) {
        String div = def.getMetadata().get("divisibility");
        int divisibilityDivider = 1;
        if (div != null) {
            int divisibility = Integer.parseInt(div);
            divisibilityDivider = BigInteger.TEN.pow(divisibility).intValue();
        }
        return new BigDecimal(divisibilityDivider);
    }

    private static Wallet.SendRequest makeAssetSendRequest(String dest, long amount) throws AddressFormatException {
        Transaction tx = new Transaction(wallet.getParams());
        Address to = new Address(SmartColors.getAssetParameters(!isTestNet()), dest);
        Script outputScript = ScriptBuilder.createOutputScript(to);
        AssetCoinSelector.addAssetOutput(tx, outputScript, amount);
        Wallet.SendRequest request = Wallet.SendRequest.forTx(tx);
        request.shuffleOutputs = false;
        request.coinSelector = new BitcoinCoinSelector(wallet.getContext(), colorChain);
        return request;
    }


    private static void scan(List<?> cmdArgs) {
        syncChain();
        checkState(wallet.isConsistent());
        Utils.sleep(1 * 1000);
        if (options.has("verbose")) {
            dumpState();
        }
        Utils.sleep(1 * 1000);
        done();
    }

    private static void dumpState() {
        System.out.println(scanner);
        System.out.println(wallet);
        if (false) {
            System.out.println("Sample BIP44 addresses");
            ImmutableList<ChildNumber> epath = HDUtils.concat(DeterministicKeyChain.BIP44_ACCOUNT_ZERO_PATH, DeterministicKeyChain.EXTERNAL_SUBPATH);
            for (int i = 0; i < 10; i++) {
                DeterministicKey key = wallet.getActiveKeychain().getKeyByPath(HDUtils.concat(epath, Lists.newArrayList(new ChildNumber(i))), true);
                System.out.println("" + i + " " + key.toAddress(params));
            }
            System.out.println("----");
            System.out.println("Sample BIP32 addresses");
            ImmutableList<ChildNumber> epath0 = HDUtils.concat(DeterministicKeyChain.ACCOUNT_ZERO_PATH, DeterministicKeyChain.EXTERNAL_SUBPATH);
            for (int i = 0; i < 10; i++) {
                DeterministicKey key = wallet.getActiveKeychain().getKeyByPath(HDUtils.concat(epath0, Lists.newArrayList(new ChildNumber(i))), true);
                System.out.println("" + i + " " + key.toAddress(params));
            }
        }
        System.out.println("************** Unspent Transactions:");
        for (Transaction tx : multiWallet.getTransactionPool(WalletTransaction.Pool.UNSPENT).values()) {
            Map<ColorDefinition, Long> values = scanner.getOutputValues(tx, wallet, colorChain);
            System.out.print(tx.getHash());
            for (Map.Entry<ColorDefinition, Long> entry : values.entrySet()) {
                BigDecimal divisibilityDivider = getDivider(entry.getKey());
                BigDecimal amount = BigDecimal.valueOf(entry.getValue()).divide(divisibilityDivider);
                System.out.print("  " + entry.getKey().getName() + ": " + amount);
            }
            System.out.print(" @" + tx.getUpdateTime());
            System.out.println();
        }
        System.out.println("\n************** Balances:");
        Map<ColorDefinition, Long> balances = scanner.getBalances(multiWallet, colorChain);
        for (Map.Entry<ColorDefinition, Long> entry : balances.entrySet()) {
            BigDecimal divisibilityDivider = getDivider(entry.getKey());
            BigDecimal amount = BigDecimal.valueOf(entry.getValue()).divide(divisibilityDivider);
            System.out.println(entry.getKey().getName() + " : " + amount);
        }
        System.out.println("\n************** Key Usage:");
        List<KeyAndAddress> sorted = getSortedKeys(wallet);
        for (KeyAndAddress kna : sorted) {
            System.out.println("  " + kna.address + " " + kna.key.getPathAsString());
        }
        System.out.println("\n************** Current Key:");
        Address assetBitcoinAddress = colorChain.currentOutputScript(KeyChain.KeyPurpose.RECEIVE_FUNDS).getToAddress(params);
        System.out.println(assetBitcoinAddress);
        System.out.println(SmartColors.toAssetAddress(assetBitcoinAddress, !isTestNet()));
    }

    static class KeyAndAddress {
        DeterministicKey key;
        Address address;

        KeyAndAddress(DeterministicKey key, Address address) {
            this.key = key;
            this.address = address;
        }
    }

    private static List<KeyAndAddress> getSortedKeys(Wallet wallet) {
        Set<KeyAndAddress> knas = Sets.newHashSet();
        for (Transaction tx : wallet.getTransactions(true)) {
            for (TransactionOutput o : tx.getOutputs()) {
                if (o.isMine(wallet)) {
                    try {
                        Script script = o.getScriptPubKey();
                        Address address = script.getToAddress(params);
                        ECKey key;
                        if (script.isSentToRawPubKey()) {
                            byte[] pubkey = script.getPubKey();
                            key = wallet.findKeyFromPubKey(pubkey);
                        } else if (script.isSentToAddress()) {
                            byte[] pubkeyHash = script.getPubKeyHash();
                            key = wallet.findKeyFromPubHash(pubkeyHash);
                        } else if (script.isPayToScriptHash()) {
                            byte[] a = script.getPubKeyHash();
                            key = wallet.findRedeemDataFromScriptHash(a).getFullKey();
                        } else {
                            log.warn("unknown script format " + script);
                            continue;
                        }
                        DeterministicKey key1 = (DeterministicKey) key;
                        knas.add(new KeyAndAddress(key1, address));
                    } catch (ScriptException e) {
                        // Just means we didn't understand the output of this transaction: ignore it.
                        log.warn("Could not parse tx output script: {}", e.toString());
                    }
                }
            }
        }
        return new Ordering<KeyAndAddress>() {
            @Override
            public int compare(@Nullable KeyAndAddress left, @Nullable KeyAndAddress right) {
                Iterator<ChildNumber> li = left.key.getPath().iterator();
                Iterator<ChildNumber> ri = right.key.getPath().iterator();
                ComparisonChain chain = ComparisonChain.start();
                while (ri.hasNext() && li.hasNext()) {
                    ChildNumber rc = ri.next();
                    ChildNumber lc = li.next();
                    chain = chain.compare(lc.i(), rc.i());
                }
                chain = chain.compare(li.hasNext(), ri.hasNext());
                return chain.result();
            }
        }.sortedCopy(knas);
    }

    private static boolean isTestNet() {
        return params.getId().equals(NetworkParameters.ID_TESTNET);
    }

    private static boolean isRegTest() {
        return params.getId().equals(NetworkParameters.ID_REGTEST);
    }

    private static ColorDefinition makeColorDefinition() {
        String ser;
        if (params.getId().equals(NetworkParameters.ID_REGTEST)) {
            ser = "000000005b0000000000000000000000000000000000000000000000000000000000000000000000010174b16bf3ce53c26c3bc7a42f06328b4776a616182478b7011fba181db0539fc500000000";
        } else if (isTestNet()) {
            ser = "000000002e970400000000000000000000000000000000000000000000000000000000000000000001019fe1cdae009a55d0877550aabdc7a1dc187f1dabcea8cf167827d6401f912db100000000";
        } else {
            throw new IllegalArgumentException();
        }
        HashMap<String, String> metadata = Maps.newHashMap();
        metadata.put("name", "widgets");
        ColorDefinition def = null; //ColorDefinition.fromPayload(params, Utils.HEX.decode(ser), metadata);
        System.out.println(def);
        return def;
    }

    private static ColorDefinition makeColorDefinition1() {
        List<String> genesisStrings = Lists.newArrayList("a18ed2595af17c30f5968a1c93de2364ae8d5af9d547f2336aafda8ed529fb2e:0:10000");
        Map<TransactionOutPoint, Long> points = Maps.newHashMap();
        for (String str : genesisStrings) {
            String[] sp = str.split(":", 3);
            points.put(new TransactionOutPoint(params, Long.parseLong(sp[1]), Sha256Hash.wrap(sp[0])), Long.parseLong(sp[2]));
        }
        GenesisOutPointsMerbinnerTree outPoints = new GenesisOutPointsMerbinnerTree(params, points);
        GenesisScriptMerbinnerTree scripts = new GenesisScriptMerbinnerTree();
        HashMap<String, String> metadata = Maps.newHashMap();
        metadata.put("name", "widgets");
        return new ColorDefinition(params, outPoints, scripts, metadata);
    }

    private static void usage() throws IOException {
        System.err.println("Version: " + SmartColors.getVersion());
        System.err.println("Usage: OPTIONS COMMAND ARGS*\n" +
                "scan\n" +
                "send COLOR DEST AMOUNT\n");
        parser.printHelpOn(System.err);
        System.exit(1);
    }

    private static class MyBlockChain extends BlockChain {
        public MyBlockChain(NetworkParameters params, Wallet wallet, BlockStore store) throws BlockStoreException {
            super(params, wallet, store);
        }

        public void roll(int height) throws BlockStoreException {
            rollbackBlockStore(height);
        }
    }

    // seconds
    private static long getEpoch() {
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse("2014-09-24T00:00:00+0000").getTime() / 1000;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
