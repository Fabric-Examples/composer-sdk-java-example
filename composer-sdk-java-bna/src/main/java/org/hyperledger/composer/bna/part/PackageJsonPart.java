/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.part;

public class PackageJsonPart extends BNAPart {

	private String name, version, description;

	public PackageJsonPart(String name, String version, String description) {
		super("package.json");
		this.name = name;
		this.version = version;
		this.description = description;
	}

	@Override
	public String toString() {
		return String.format("{\n\t\"name\": \"%s\",\n\t\"version\": \"%s\",\n\t\"description\": \"%s\"\n}", name, version, description);
	}
}
