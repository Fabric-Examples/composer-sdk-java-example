/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.part;

import org.hyperledger.composer.bna.model.Model;

import java.util.HashSet;
import java.util.Set;

public class CTOPart extends BNAPart {
	private String namespace;
	private final Set<String> dependencies;

	public CTOPart(String namespace) {
		super("models/" + namespace + ".cto");
		this.namespace = namespace;
		this.dependencies = new HashSet<>();
	}

	@Override
	public void addEntry(Object entry) {
		if (entry instanceof String) {
			dependencies.add((String) entry);
		} else if (entry instanceof Model) {
			super.addEntry(entry);
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("namespace ").append(namespace).append("\n");
		for (String dependency : dependencies) {
			builder.append("import ").append(dependency).append('\n');
		}
		for (Object model : entries) {
			builder.append('\n').append((model.toString()));
		}
		return builder.toString();
	}
}
