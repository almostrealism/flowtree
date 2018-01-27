/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.flowtree.coin;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;

import org.almostrealism.flow.Node;
import org.almostrealism.flow.NodeGroup;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * TODO  Store file on distributed file system.
 * TODO  Responding to coins should be handled by a mining job.
 *       Mining jobs initialized on other machines should trigger
 *       the startup of a mining node.
 * 
 */
public class MiningNode extends Node {
	private static Address forwardingAddress;
	private static WalletAppKit kit;
	
	public MiningNode(NodeGroup parent, int id, NetworkParameters params, Address address) {
		super(parent, id, 0, 0);
		
		forwardingAddress = address;
		kit = new WalletAppKit(params, new File("."), getClass().getSimpleName());
		
		// Download the block chain and wait until it's done.
		kit.startAsync();
		kit.awaitRunning();
		
		kit.wallet().addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
			@Override
			public void onCoinsReceived(Wallet w, Transaction tx, Coin prevBalance, Coin newBalance) {
				// The transaction can either be pending, or included into a block (we
				// didn't see the broadcast).
				Coin value = tx.getValueSentToMe(w);
				System.out.println("Received tx for " + value.toFriendlyString() + ": " + tx);
				System.out.println("Transaction will be forwarded after it confirms.");
				
				// Wait until it's made it into the block chain
				Futures.addCallback(tx.getConfidence().getDepthFuture(1), new FutureCallback<TransactionConfidence>() {
					@Override
					public void onSuccess(TransactionConfidence result) {
						forwardCoins(tx);
					}

					@Override
					public void onFailure(Throwable t) {
						throw new RuntimeException(t);
					}
				});
			}
		});

		Address sendToAddress = kit.wallet().currentReceiveKey().toAddress(params);
		System.out.println("\nSend coins to: " + sendToAddress);
		System.out.println("Waiting for coins to arrive.");
	}
	
	private static void forwardCoins(Transaction tx) {
		try {
			Coin value = tx.getValueSentToMe(kit.wallet());
			System.out.println("Forwarding " + value.toFriendlyString());
			// Now send the coins back! Send with a small fee attached to ensure rapid
			// confirmation.
			final Coin amountToSend = value.subtract(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE);
			final Wallet.SendResult sendResult = kit.wallet().sendCoins(kit.peerGroup(), forwardingAddress,
					amountToSend);
			checkNotNull(sendResult); // We should never try to send more coins than we have!
			System.out.println("Sending ...");
			// Register a callback that is invoked when the transaction has propagated
			// across the network.
			// This shows a second style of registering ListenableFuture callbacks, it works
			// when you don't
			// need access to the object the future returns.
			sendResult.broadcastComplete.addListener(new Runnable() {
				@Override
				public void run() {
					// The wallet has changed now, it'll get auto saved shortly or when the app
					// shuts down.
					System.out.println("Sent coins onwards! Transaction hash is " + sendResult.tx.getHashAsString());
				}
			}, MoreExecutors.directExecutor());
		} catch (KeyCrypterException | InsufficientMoneyException e) {
			// We don't use encrypted wallets in this example - can never happen.
			throw new RuntimeException(e);
		}
	}
}
