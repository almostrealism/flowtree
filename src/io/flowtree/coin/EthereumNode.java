/*
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package io.flowtree.coin;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

import org.almostrealism.flow.NodeGroup;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.*;
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.EthereumFactory;
import org.ethereum.listener.EthereumListener;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.mine.MinerListener;
import org.ethereum.net.eth.message.StatusMessage;
import org.ethereum.net.message.Message;
import org.ethereum.net.p2p.HelloMessage;
import org.ethereum.net.rlpx.Node;
import org.ethereum.net.server.Channel;
import org.ethereum.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;
import java.util.*;

public class EthereumNode extends org.almostrealism.flow.Node implements MinerListener {
    public static final Logger sLogger = LoggerFactory.getLogger(EthereumNode.class);
    private static CustomFilter CUSTOM_FILTER;

    private String loggerName;
    protected Logger logger;

    @Autowired
    protected Ethereum ethereum;
    
    @Autowired
    protected SystemProperties config;
    
    private volatile long txCount;
    private volatile long gasSpent;
    
	public EthereumNode(NodeGroup parent, int id) {
		super(parent, id, 0, 0);
        sLogger.info("Starting EthereumJ...");
        EthereumFactory.createEthereum();
    }
	
    protected void setupLogging() {
        addSampleLogger(loggerName);
        logger = LoggerFactory.getLogger(loggerName);
    }

    /**
     * Allow only selected logger to print DEBUG events to STDOUT and FILE.
     * Other loggers are allowed to print ERRORS only.
     */
    private static void addSampleLogger(final String loggerName) {
        if (CUSTOM_FILTER == null) {
            CUSTOM_FILTER = new CustomFilter();
            final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

            Appender ca = loggerContext.getLogger("ROOT").getAppender("STDOUT");
            ca.clearAllFilters();
            ca.addFilter(CUSTOM_FILTER);
        }
        CUSTOM_FILTER.addVisibleLogger(loggerName);
    }

    /**
     * The method is called after all EthereumJ instances are created
     */
    @PostConstruct
    private void springInit() {
        setupLogging();
        // adding the main EthereumJ callback to be notified on different kind of events
        ethereum.addListener(listener);
    }

    /**
     * The method tracks step-by-step the instance lifecycle from node discovery till sync completion.
     * At the end the method onSyncDone() is called which might be overridden by a sample subclass
     * to start making other things with the Ethereum network
     */
    public void run() {
        try {
            logger.info("Sample worker thread started.");

            if (config.peerDiscovery()) {
                waitForDiscovery();
            } else {
                logger.info("Peer discovery disabled. We should actively connect to another peers or wait for incoming connections");
            }

            waitForAvailablePeers();

            waitForSyncPeers();

            waitForFirstBlock();

            waitForSync();

            onSyncDone();

        } catch (Exception e) {
            logger.error("Error occurred in Sample: ", e);
        }
    }
    
    public void onSyncDone() throws Exception {
        logger.debug("Monitoring new blocks in real-time...");
        logger.info("Starting miner...");
        ethereum.getBlockMiner().addListener(this);
        ethereum.getBlockMiner().startMining();
        logger.info("Ethereum mining has begun");
        super.run();
    }

    @Override
    public void miningStarted() {
        logger.info("Miner started");
    }

    @Override
    public void miningStopped() {
        logger.info("Miner stopped");
    }

    @Override
    public void blockMiningStarted(Block block) {
        logger.info("Start mining block: " + block.getShortDescr());
    }

    @Override
    public void blockMined(Block block) {
        logger.info("Block mined! : \n" + block);
    }

    @Override
    public void blockMiningCanceled(Block block) {
        logger.info("Cancel mining block: " + block.getShortDescr());
    }
    
    protected List<Node> nodesDiscovered = new Vector<>();

    /**
     * Waits until any new nodes are discovered by the UDP discovery protocol
     */
    protected void waitForDiscovery() throws Exception {
        logger.info("Waiting for nodes discovery...");

        int bootNodes = config.peerDiscoveryIPList().size();
        int cnt = 0;
        while(true) {
            Thread.sleep(cnt < 30 ? 300 : 5000);

            if (nodesDiscovered.size() > bootNodes) {
                logger.info("[v] Discovery works, new nodes started being discovered.");
                return;
            }

            if (cnt >= 30) logger.warn("Discovery keeps silence. Waiting more...");
            if (cnt > 50) {
                logger.error("Looks like discovery failed, no nodes were found.\n" +
                        "Please check your Firewall/NAT UDP protocol settings.\n" +
                        "Your IP interface was detected as " + config.bindIp() + ", please check " +
                        "if this interface is correct, otherwise set it manually via 'peer.discovery.bind.ip' option.");
                throw new RuntimeException("Discovery failed.");
            }
            cnt++;
        }
    }

    protected Map<Node, StatusMessage> ethNodes = new Hashtable<>();

    /**
     * Discovering nodes is only the first step. No we need to find among discovered nodes
     * those ones which are live, accepting inbound connections, and has compatible subprotocol versions
     */
    protected void waitForAvailablePeers() throws Exception {
        logger.info("Waiting for available Eth capable nodes...");
        int cnt = 0;
        while(true) {
            Thread.sleep(cnt < 30 ? 1000 : 5000);

            if (ethNodes.size() > 0) {
                logger.info("[v] Available Eth nodes found.");
                return;
            }

            if (cnt >= 30) logger.info("No Eth nodes found so far. Keep searching...");
            if (cnt > 60) {
                logger.error("No eth capable nodes found. Logs need to be investigated.");
//                throw new RuntimeException("Eth nodes failed.");
            }
            cnt++;
        }
    }

    protected List<Node> syncPeers = new Vector<>();

    /**
     * When live nodes found SyncManager should select from them the most
     * suitable and add them as peers for syncing the blocks
     */
    protected void waitForSyncPeers() throws Exception {
        logger.info("Searching for peers to sync with...");
        int cnt = 0;
        while(true) {
            Thread.sleep(cnt < 30 ? 1000 : 5000);

            if (syncPeers.size() > 0) {
                logger.info("[v] At least one sync peer found.");
                return;
            }

            if (cnt >= 30) logger.info("No sync peers found so far. Keep searching...");
            if (cnt > 60) {
                logger.error("No sync peers found. Logs need to be investigated.");
//                throw new RuntimeException("Sync peers failed.");
            }
            cnt++;
        }
    }

    protected Block bestBlock = null;

    /**
     * Waits until blocks import started
     */
    protected void waitForFirstBlock() throws Exception {
        Block currentBest = ethereum.getBlockchain().getBestBlock();
        logger.info("Current BEST block: " + currentBest.getShortDescr());
        logger.info("Waiting for blocks start importing (may take a while)...");
        int cnt = 0;
        while(true) {
            Thread.sleep(cnt < 300 ? 1000 : 60000);

            if (bestBlock != null && bestBlock.getNumber() > currentBest.getNumber()) {
                logger.info("[v] Blocks import started.");
                return;
            }

            if (cnt >= 300) logger.info("Still no blocks. Be patient...");
            if (cnt > 330) {
                logger.error("No blocks imported during a long period. Must be a problem, logs need to be investigated.");
//                throw new RuntimeException("Block import failed.");
            }
            cnt++;
        }
    }

    boolean synced = false;
    boolean syncComplete = false;

    /**
     * Waits until the whole blockchain sync is complete
     */
    private void waitForSync() throws Exception {
        logger.info("Waiting for the whole blockchain sync (will take up to several hours for the whole chain)...");
        while(true) {
            Thread.sleep(10000);

            if (synced) {
                logger.info("[v] Sync complete! The best block: " + bestBlock.getShortDescr());
                syncComplete = true;
                return;
            }

            logger.info("Blockchain sync in progress. Last imported block: " + bestBlock.getShortDescr() +
                    " (Total: txs: " + txCount + ", gas: " + (gasSpent / 1000) + "k)");
            txCount = 0;
            gasSpent = 0;
        }
    }

    /**
     * The main EthereumJ callback.
     */
    EthereumListener listener = new EthereumListenerAdapter() {
        @Override
        public void onSyncDone(SyncState state) {
            synced = true;
        }

        @Override
        public void onNodeDiscovered(Node node) {
            if (nodesDiscovered.size() < 1000) {
                nodesDiscovered.add(node);
            }
        }

        @Override
        public void onEthStatusUpdated(Channel channel, StatusMessage statusMessage) {
            ethNodes.put(channel.getNode(), statusMessage);
        }

        @Override
        public void onPeerAddedToSyncPool(Channel peer) {
            syncPeers.add(peer.getNode());
        }

        @Override
        public void onBlock(Block block, List<TransactionReceipt> receipts) {
            bestBlock = block;
            txCount += receipts.size();
            for (TransactionReceipt receipt : receipts) {
                gasSpent += ByteUtil.byteArrayToLong(receipt.getGasUsed());
            }
            if (syncComplete) {
                logger.info("New block: " + block.getShortDescr());
            }
        }
        @Override
        public void onRecvMessage(Channel channel, Message message) {
        }

        @Override
        public void onSendMessage(Channel channel, Message message) {
        }

        @Override
        public void onPeerDisconnect(String host, long port) {
        }

        @Override
        public void onPendingTransactionsReceived(List<Transaction> transactions) {
        }

        @Override
        public void onPendingStateChanged(PendingState pendingState) {
        }
        @Override
        public void onHandShakePeer(Channel channel, HelloMessage helloMessage) {
        }

        @Override
        public void onNoConnections() {
        }

        @Override
        public void onVMTraceCreated(String transactionHash, String trace) {
        }

        @Override
        public void onTransactionExecuted(TransactionExecutionSummary summary) {
        }
    };

    private static class CustomFilter extends Filter<ILoggingEvent> {
        private Set<String> visibleLoggers = new HashSet<>();
        @Override
        public synchronized FilterReply decide(ILoggingEvent event) {
            return visibleLoggers.contains(event.getLoggerName()) && event.getLevel().isGreaterOrEqual(Level.INFO) ||
                    event.getLevel().isGreaterOrEqual(Level.ERROR) ? FilterReply.NEUTRAL : FilterReply.DENY;
        }

        public synchronized void addVisibleLogger(String name) {
            visibleLoggers.add(name);
        }
    }
}
