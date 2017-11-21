/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.part;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ACLPart extends BNAPart {
	public ACLPart() {
		super("permissions.acl");
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		try (BufferedReader defaultACL = new BufferedReader(new InputStreamReader(
				ACLPart.class.getClassLoader().getResourceAsStream("default.acl")))) {
			for (String line; (line = defaultACL.readLine()) != null; ) {
				builder.append(line).append('\n');
			}
		} catch (IOException ignored) {
		}
		return builder.toString();
	}
}
