/*
 * Copyright 2017 Michael Murray
 *
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

import java.io.File;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

import static com.google.common.base.Preconditions.checkNotNull;

import io.flowtree.job.Job;
import io.flowtree.job.JobFactory;

/**
 * @author Michael Murray
 */
public class MiningJobFactory implements JobFactory {
	private static Address forwardingAddress;
	private static WalletAppKit kit;

	public MiningJobFactory(String address) {
		BriefLogFormatter.init();
		
		String filePrefix = "mining-job";
		forwardingAddress = Address.fromBase58(MainNetParams.get(), address);

		// Start up a basic app using a class that automates some boilerplate.
		kit = new WalletAppKit(MainNetParams.get(), new File("."), filePrefix);
		
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

		Address sendToAddress = kit.wallet().currentReceiveKey().toAddress(MainNetParams.get());
		System.out.println("Send coins to: " + sendToAddress);
		System.out.println("Waiting for coins to arrive. Press Ctrl-C to quit.");

		try {
			Thread.sleep(Long.MAX_VALUE);
		} catch (InterruptedException ignored) {
		}
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

	/**
	 * @see io.flowtree.job.JobFactory#getTaskId()
	 */
	@Override
	public long getTaskId() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @see io.flowtree.job.JobFactory#nextJob()
	 */
	@Override
	public Job nextJob() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see io.flowtree.job.JobFactory#createJob(java.lang.String)
	 */
	@Override
	public Job createJob(String data) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see io.flowtree.job.JobFactory#set(java.lang.String, java.lang.String)
	 */
	@Override
	public void set(String key, String value) {
		// TODO Auto-generated method stub

	}

	/**
	 * @see io.flowtree.job.JobFactory#encode()
	 */
	@Override
	public String encode() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see io.flowtree.job.JobFactory#getName()
	 */
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see io.flowtree.job.JobFactory#getCompleteness()
	 */
	@Override
	public double getCompleteness() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @see io.flowtree.job.JobFactory#isComplete()
	 */
	@Override
	public boolean isComplete() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see io.flowtree.job.JobFactory#setPriority(double)
	 */
	@Override
	public void setPriority(double p) {
		// TODO Auto-generated method stub

	}

	/**
	 * @see io.flowtree.job.JobFactory#getPriority()
	 */
	@Override
	public double getPriority() {
		// TODO Auto-generated method stub
		return 0;
	}
}
