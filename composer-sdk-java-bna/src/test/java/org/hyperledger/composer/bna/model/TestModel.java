/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.model;

import org.hyperledger.composer.annotation.Asset;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class TestModel {
	AnAsset asset;

	AnAsset[] assetArray;

	Object notPointer;

	Map<String, String> mapField;

	List<AnAsset> assets;

	List<Integer> ints;

	List<int[]> intsList;

	BigDecimal decimal;

	boolean hello() {
		return true;
	}
}

@Asset
class AnAsset {
	private int x;

	AnAsset(int x) {
		this.x = x;
	}

	int hello() {
		return x;
	}
}

class SubModel extends TestModel {
}
