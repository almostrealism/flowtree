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

import org.almostrealism.flow.NodeGroup;
import org.bitcoinj.core.Address;
import org.libdohj.params.DogecoinMainNetParams;

/**
 */
public class DogecoinMiningNode extends MiningNode {
	public DogecoinMiningNode(NodeGroup parent, int id, String address) {
		super(parent, id, DogecoinMainNetParams.get(), new Address(DogecoinMainNetParams.get(), address));
	}
}
