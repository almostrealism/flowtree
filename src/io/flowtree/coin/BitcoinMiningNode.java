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

import io.flowtree.node.NodeGroup;
import org.bitcoinj.core.Address;
import org.bitcoinj.params.MainNetParams;

/**
 * @author Michael Murray
 */
public class BitcoinMiningNode extends MiningNode {
	public BitcoinMiningNode(NodeGroup parent, int id, String address) {
		super(parent, id, MainNetParams.get(), Address.fromBase58(MainNetParams.get(), address));
	}
}