/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hyperledger.composer.ComposerAPI;
import org.hyperledger.composer.ComposerException;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public abstract class NamedQuery<T> extends AbstractQuery<T> {

	private String name;

	public NamedQuery(ComposerAPI api, Class<T> clazz, String name) {
		super(api, clazz);
		this.name = name;
	}

	public List<T> execute() throws ComposerException {
		return this.execute("named", name);
	}
	
	@Override
	protected void validate(ObjectNode params) throws ComposerException {
		Iterator<Entry<String, JsonNode>> iterator = params.fields();
		Set<String> keys = new HashSet<>();
		while (iterator.hasNext()) {
			Entry<String, JsonNode> entry = iterator.next();
			String key = entry.getKey();
			keys.add(key);
		}
		if (keys.contains("v-1")) {
			throw new ComposerException(ComposerException.INVALID_INPUT_ERROR, "Finding index 0, must start from 1");
		}
		int numOfParams = keys.size();
		for (int i = 0; i < numOfParams; i++) {
			if (!keys.contains(MessageFormat.format("v{0}", i))) {
				throw new ComposerException(ComposerException.INVALID_INPUT_ERROR, "Missing parameter " + (i + 1));
			}
		}
	}

}
