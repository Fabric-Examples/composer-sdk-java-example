/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.client;

import org.hyperledger.composer.ComposerException;
import org.hyperledger.composer.ResourceSerializer;
import org.hyperledger.composer.system.ActivateCurrentIdentity;

import java.util.Map;

public interface ComposerConnector extends ComposerChaincodeAPI {

	void disconnect();

	void enroll(ComposerIdentity composerIdentity, boolean ignoreExisting) throws ComposerException;

	default void login(ComposerIdentity composerIdentity) throws ComposerException {
		_login(composerIdentity);
		try {
			ping();
		} catch (ComposerException e) {
			if (e.getMessage().contains("ACTIVATION_REQUIRED")) {
				invokeChaincode("submitTransaction", ResourceSerializer.toJSONString(new ActivateCurrentIdentity()));
			} else {
				throw e;
			}
		}
	}

	void _login(ComposerIdentity composerIdentity) throws ComposerException;

	default void ping() throws ComposerException {
		queryChaincode("ping");
	}

	String register(ComposerIdentity request) throws ComposerException;

	default String queryChaincode(String functionName, String... args) throws ComposerException {
		if (functionName == null || functionName.isEmpty()) {
			throw new ComposerException(ComposerException.INVALID_INPUT_ERROR, "functionName not specified");
		}
		return _queryChaincode(functionName, args);
	}

	String _queryChaincode(String functionName, String... args) throws ComposerException;

	default String invokeChaincode(String functionName, String... args) throws ComposerException {
		if (functionName == null || functionName.isEmpty()) {
			throw new ComposerException(ComposerException.INVALID_INPUT_ERROR, "functionName not specified");
		}
		return _invokeChaincode(functionName, null, args);
	}

	String _invokeChaincode(String functionName, Map<String, byte[]> transientMap, String... args) throws ComposerException;

	void on(ComposerEventListener listener) throws ComposerException;
}
