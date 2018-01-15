package org.smartwallet.stratum;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.bitcoinj.core.*;
import org.bitcoinj.utils.BriefLogFormatter;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.converter.Converter;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.*;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.converter.ConverterInvocation;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.helper.InterruptHook;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.actions.Action;
import org.jboss.aesh.terminal.Color;
import org.jboss.aesh.terminal.TerminalColor;
import org.jboss.aesh.terminal.TerminalString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.zip.GZIPOutputStream;

import static com.google.common.base.Throwables.propagate;

/**
 * Created by devrandom on 2015-Aug-30.
 */
public class StratumCli {
    public static final int CALL_TIMEOUT = 2000;
    public static NetworkParameters params;
    protected static Logger log = LoggerFactory.getLogger("StratumCli");
    private static OptionParser parser;
    private static OptionSet options;
    private static String net;

    private StratumClient client;
    private AeshConsole console;
    private ObjectMapper mapper;
    private BlockingQueue<StratumMessage> addressChangeQueue;
    private ExecutorService addressChangeService;
    private BlockingQueue<StratumMessage> headersChangeQueue;
    private ExecutorService headersChangeService;
    private StratumChain chain;
    private HeadersStore store;

    public static void main(String[] args) throws IOException {
        BriefLogFormatter.init();
        LogManager.getLogManager().getLogger("").setLevel(Level.FINEST);
        log.info("Starting up ...");

        parser = new OptionParser();
        parser.accepts("prod", "use prodnet (default is testnet)");
        parser.accepts("regtest", "use regtest mode (default is testnet)");

        if (args.length > 1) {
            System.err.println("Usage: StratumCli HOST:PORT");
            System.exit(1);
        }

        options = parser.parse(args);
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

        List<String> cmds = (List<String>) options.nonOptionArguments();

        if (cmds.size() > 0) {
            String[] hostPort = cmds.remove(0).split(":");
            if (hostPort.length != 2) {
                System.err.println("Usage: StratumCli HOST:PORT");
                System.exit(1);
            }
            String host = hostPort[0];
            int port = Integer.parseInt(hostPort[1]);
            new StratumCli().run(Lists.newArrayList(new InetSocketAddress(host, port)));
        } else {
            new StratumCli().run(null);
        }
    }

    private static URL getResource(String path) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        return classloader.getResource(path);
    }

    private static InputStream openResource(String path) {
        try {
            return getResource(path).openStream();
        } catch (IOException e) {
            throw propagate(e);
        }
    }

    private void run(List<InetSocketAddress> addresses) {
        mapper = new ObjectMapper();
        client = new StratumClient(params, addresses, true);
        InputStream checkpointsInputStream = openResource(String.format("checkpoints-%s.txt", net));
        CheckpointManager checkpoints;
        try {
            checkpoints = new CheckpointManager(params, checkpointsInputStream);
        } catch (IOException e) {
            throw propagate(e);
        }
        store = new HeadersStore(params, new File(net + "-stratum.chain"), checkpoints.getCheckpointBefore(new Date().getTime()), getResource(net + "-headers.gz"));
        chain = new StratumChain(params, store, client);
        client.startAsync();
        chain.startAsync();
        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(new ExitCommand())
                .command(new CloseCommand())
                .command(new ConnectCommand())
                .command(new HeaderCommand())
                .command(new HistoryCommand())
                .command(new BalanceCommand())
                .command(new GetTransactionCommand())
                .command(new UnspentCommand())
                .command(new VersionCommand())
                .command(new BannerCommand())
                .command(new HelpCommand())
                .command(new SubscribeAddressCommand())
                .command(new SubscribeHeadersCommand())
                .command(new SetQueueCommand())
                .command(new InfoCommand())
                .command(new DumpChainCommand())
                .create();
        Settings settings = new SettingsBuilder()
                .logging(false)
                .persistHistory(true)
                .historyFile(new File(System.getProperty("user.home"), ".cache/stratum-cli.hist"))
                .interruptHook(new InterruptHook() {
                    @Override
                    public void handleInterrupt(Console console, Action action) {
                        if (action == Action.EOF) {
                            console.stop();
                            cleanup();
                        }
                    }
                })
                .create();
        console = new AeshConsoleBuilder()
                .commandRegistry(registry)
                .settings(settings)
                .prompt(new Prompt(new TerminalString("[aesh@rules]$ ",
                        new TerminalColor(Color.GREEN, Color.DEFAULT, Color.Intensity.BRIGHT))))
                .create();

        console.start();
    }

    @CommandDefinition(name="exit", description = "exit the program")
    public class ExitCommand implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            stop(commandInvocation);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name="close", description = "close the socket for reconnect testing")
    public class CloseCommand implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            client.closeSocket();
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name="connect", description = "connect to server")
    public class ConnectCommand implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            client.startAsync();
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name="info", description = "info")
    public class InfoCommand implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            println("height: " + store.getHeight());
            println("top: " + store.top().getHash());
            println(store.top());
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name="dumpchain", description = "dump the header store to the resources directory")
    public class DumpChainCommand implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            println("height: " + store.getHeight());
            println("top: " + store.top().getHash());
            println(store.top());
            File outFile = new File("stratum-protocol/src/main/resources/" + net + "-headers.gz");
            GZIPOutputStream out;
            
			try {
				out = new GZIPOutputStream(new FileOutputStream(outFile));
			} catch (IOException e) {
				throw new CommandException(e);
			}
			
			try {
	            long roundHeight = store.getHeight() - (store.getHeight() % NetworkParameters.INTERVAL);
	            println("write to: " + roundHeight);
	            byte[] emptyBlock = new byte[Block.HEADER_SIZE];
	            for (int i = 0 ; i < roundHeight ; i++) {
	                Block block = store.get(i);
	                if (block != null)
	                    out.write(block.bitcoinSerialize());
	                else {
	                    out.write(emptyBlock);
	                }
	            }
	            out.close();
			} catch (IOException e) {
				throw new CommandException(e);
			}
            return CommandResult.SUCCESS;
        }
    }

    private void stop(CommandInvocation commandInvocation) {
        cleanup();
        commandInvocation.stop();
    }

    private void cleanup() {
        client.stopAsync();
        client.awaitTerminated();
        System.out.println("done");
    }

    @CommandDefinition(name="version", description = "get server version")
    public class VersionCommand implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            try {
				simpleCall("server.version");
			} catch (IOException e) {
				throw new CommandException(e);
			}
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name="banner", description = "get server banner")
    public class BannerCommand implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            try {
				simpleCall("server.banner");
			} catch (IOException e) {
				throw new CommandException(e);
			}
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name="setqueue", description = "set queue mode")
    public class SetQueueCommand implements Command {
        @Arguments(description = "values")
        List<Boolean> values;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            if (values == null || values.size() == 0)
                client.setQueue(true);
            else if (values.size() == 1)
                client.setQueue(values.get(0));
            else {
                return CommandResult.FAILURE;
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name="history", description = "get address history")
    public class HistoryCommand implements Command {
        @Arguments(description = "addresses")
        List<String> addresses;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            List<Object> params = Lists.newArrayList();
            params.addAll(addresses);
            callAndWaitForResult("blockchain.address.get_history", params);
            return CommandResult.SUCCESS;
        }
    }

    private void callAndWaitForResult(String method, List<Object> params) {
        ListenableFuture<StratumMessage> future = client.call(method, params);
        final long start = System.currentTimeMillis();
        try {
            StratumMessage result = future.get(CALL_TIMEOUT, TimeUnit.MILLISECONDS);
            print("result: ");
            println(formatResult(result));
        } catch (InterruptedException | ExecutionException e) {
            printerr("failed %s\n", e);
        } catch (TimeoutException e) {
            printerrln("timeout, going async");
            Futures.addCallback(future, new FutureCallback<StratumMessage>() {
                @Override
                public void onSuccess(StratumMessage result) {
                    print("async result in " + (System.currentTimeMillis() - start)/1000 + " seconds:");
                    println(formatResult(result));
                }

                @Override
                public void onFailure(Throwable t) {
                    printerr("failed %s\n", t);
                }
            });
        }
    }

    @CommandDefinition(name="balance", description = "get address balance")
    public class BalanceCommand implements Command {
        @Arguments(description = "addresses")
        List<String> addresses;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            List<Object> params = Lists.newArrayList();
            params.addAll(addresses);
            ListenableFuture<StratumMessage> future = client.call("blockchain.address.get_balance", params);
            try {
                StratumMessage result = future.get(CALL_TIMEOUT, TimeUnit.MILLISECONDS);
                print("result: ");
                println(formatResult(result));
            } catch (InterruptedException | ExecutionException e) {
                printerr("failed %s\n", e);
            } catch (TimeoutException e) {
                printerrln("timeout");
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name="unspent", description = "list address unspent")
    public class UnspentCommand implements Command {
        @Arguments(description = "addresses")
        List<String> addresses;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            List<Object> params = Lists.newArrayList();
            params.addAll(addresses);
            ListenableFuture<StratumMessage> future = client.call("blockchain.address.listunspent", params);
            try {
                StratumMessage result = future.get(CALL_TIMEOUT, TimeUnit.MILLISECONDS);
                print("result: ");
                println(formatResult(result));
            } catch (InterruptedException | ExecutionException e) {
                printerr("failed %s\n", e);
            } catch (TimeoutException e) {
                printerrln("timeout");
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name="header", description = "get block header")
    public class HeaderCommand implements Command {
        @Arguments(description = "hashes")
        List<String> hashes;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            List<Object> params = Lists.newArrayList();
            params.addAll(hashes);
            ListenableFuture<StratumMessage> future = client.call("blockchain.block.get_header", params);
            try {
                StratumMessage result = future.get(CALL_TIMEOUT, TimeUnit.MILLISECONDS);
                print("result: ");
                println(formatResult(result));
            } catch (InterruptedException | ExecutionException e) {
                printerr("failed %s\n", e);
            } catch (TimeoutException e) {
                printerrln("timeout");
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name="transaction", description = "get transaction")
    public class GetTransactionCommand implements Command {
        @Arguments(description = "hashes")
        List<String> hashes;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            List<Object> params = Lists.newArrayList();
            params.addAll(hashes);
            ListenableFuture<StratumMessage> future = client.call("blockchain.transaction.get", params);
            try {
                StratumMessage result = future.get(CALL_TIMEOUT, TimeUnit.MILLISECONDS);
                print("result: ");
                Object result1 = result.result;
                println(result1);
            } catch (InterruptedException | ExecutionException e) {
                printerr("failed %s\n", e);
            } catch (TimeoutException e) {
                printerrln("timeout");
            }
            return CommandResult.SUCCESS;
        }
    }

    private void println(Object item) {
        console.getShell().out().println(item);
    }

    private void printerrln(Object item) {
        console.getShell().err().println(item);
    }

    private void printerr(String format, Throwable ex) {
        console.getShell().err().printf(format, ex.getCause().getMessage());
    }

    private void print(Object item) {
        console.getShell().out().print(item);
    }

    private void simpleCall(String method) throws IOException {
        ListenableFuture<StratumMessage> future = client.call(method, Lists.newArrayList());
        try {
            StratumMessage result = future.get(CALL_TIMEOUT, TimeUnit.MILLISECONDS);
            print("result: ");
            println(result.result);
        } catch (InterruptedException | ExecutionException e) {
            printerr("failed %s\n", e);
        } catch (TimeoutException e) {
            printerrln("timeout");
        }
    }

    private String formatResult(StratumMessage result) {
        try {
            return mapper.writeValueAsString(result.result);
        } catch (JsonProcessingException e) {
            throw propagate(e);
        }
    }

    @CommandDefinition(name="help", description = "show this message")
    public class HelpCommand implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            for (String cmd : console.getCommandRegistry().getAllCommandNames()) {
                println(cmd);
                print(console.getHelpInfo(cmd));
                println("------");
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name="subscribe_headers", description = "subscribe to headers")
    public class SubscribeHeadersCommand implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            StratumSubscription subscription = client.subscribeToHeaders();
            if (headersChangeService == null) {
                headersChangeQueue = client.getHeadersQueue();
                headersChangeService = Executors.newSingleThreadExecutor();
                headersChangeService.submit(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            try {
                                StratumMessage item = headersChangeQueue.take();
                                if (item.isSentinel()) {
                                    headersChangeService.shutdown();
                                    break;
                                }
                                println(mapper.writeValueAsString(item));
                            } catch (InterruptedException | JsonProcessingException e) {
                                throw propagate(e);
                            }
                        }
                    }
                });
            }
            handleSubscriptionResult(subscription);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name="subscribe_address", description = "subscribe to address")
    public class SubscribeAddressCommand implements Command {
        @Arguments(description = "address", converter = AddressConverter.class)
        List<Address> addresses;
        
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            StratumSubscription subscription = null;
            for (Address address : addresses) {
                subscription = client.subscribe(address);
                handleSubscriptionResult(subscription);
            }
            if (subscription == null)
                return CommandResult.SUCCESS;
            if (addressChangeService == null) {
                addressChangeQueue = client.getAddressQueue();
                addressChangeService = Executors.newSingleThreadExecutor();
                addressChangeService.submit(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            try {
                                StratumMessage item = addressChangeQueue.take();
                                if (item.isSentinel()) {
                                    addressChangeService.shutdown();
                                    break;
                                }
                                println(mapper.writeValueAsString(item));
                            } catch (InterruptedException | JsonProcessingException e) {
                                throw propagate(e);
                            }
                        }
                    }
                });
            }
            return CommandResult.SUCCESS;
        }
    }

    private void handleSubscriptionResult(StratumSubscription subscription) {
        Futures.addCallback(subscription.future, new FutureCallback<StratumMessage>() {
            @Override
            public void onSuccess(StratumMessage result) {
            }

            @Override
            public void onFailure(Throwable t) {
                printerr("failed %s\n", t);
            }
        });
        try {
            StratumMessage result = subscription.future.get(CALL_TIMEOUT, TimeUnit.MILLISECONDS);
            print("initial state: ");
            println(formatResult(result));
        } catch (InterruptedException | ExecutionException e) {
            // ignore, handled by callback
        } catch (TimeoutException e) {
            printerrln("timeout");
        }
    }

    private static class AddressConverter implements Converter<Address, ConverterInvocation> {
        @Override
        public Address convert(ConverterInvocation converterInvocation) throws OptionValidatorException {
            try {
                return new Address(params, converterInvocation.getInput());
            } catch (AddressFormatException e) {
                throw new OptionValidatorException("invalid address");
            }
        }
    }
}
