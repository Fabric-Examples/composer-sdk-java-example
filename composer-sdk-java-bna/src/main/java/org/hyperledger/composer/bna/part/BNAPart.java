/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.part;

import java.util.LinkedList;
import java.util.List;

public abstract class BNAPart {
	protected List entries;
	private String name;

	public BNAPart(String name) {
		this.name = name;
		this.entries = new LinkedList();
	}

	public String entryName() {
		return name;
	}

	public void addEntry(Object entry) {
		entries.add(entry);
	}

	public abstract String toString();
}
