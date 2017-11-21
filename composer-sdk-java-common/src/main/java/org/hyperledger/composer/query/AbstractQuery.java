/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hyperledger.composer.ComposerAPI;
import org.hyperledger.composer.ComposerException;

import java.text.MessageFormat;
import java.util.List;

public abstract class AbstractQuery<T> {

	private ComposerAPI api;
	private Class<T> clazz;
	private ObjectNode params;

	public AbstractQuery(ComposerAPI api, Class<T> clazz) {
		this.api = api;
		this.clazz = clazz;
		this.params = new ObjectMapper().createObjectNode();
	}
	
	public void bind(int index, int value) throws ComposerException {
		params.put(MessageFormat.format("v{0}", index - 1), value);
	}
	
	public void bind(int index, long value) throws ComposerException{
		params.put(MessageFormat.format("v{0}", index - 1), value);
	}
	
	public void bind(int index, float value) throws ComposerException{
		params.put(MessageFormat.format("v{0}", index - 1), value);
	}
	
	public void bind(int index, double value) throws ComposerException{
		params.put(MessageFormat.format("v{0}", index - 1), value);
	}
	
	public void bind(int index, String value) throws ComposerException{
		params.put(MessageFormat.format("v{0}", index - 1), value);
	}
	
	abstract protected void validate(ObjectNode params) throws ComposerException ;

	abstract public List<T> execute() throws ComposerException ;

	List<T> execute(String type, String query) throws ComposerException {
		this.validate(params);
		return api.executeQuery(clazz, type, query, params.toString());
	}
	
}
