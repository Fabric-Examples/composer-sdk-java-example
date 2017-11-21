/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.client;

import org.hyperledger.composer.ComposerException;
import org.hyperledger.composer.Engine;

import java.lang.reflect.Proxy;
import java.util.Arrays;

public interface ComposerChaincodeAPI {
	String queryChaincode(String functionName, String... args) throws ComposerException;

	String invokeChaincode(String functionName, String... args) throws ComposerException;

	static Engine asEngine(ComposerChaincodeAPI connector) {
		return (Engine) Proxy.newProxyInstance(ComposerChaincodeAPI.class.getClassLoader(), new Class[]{Engine.class}, (proxy, method, args) -> {
			String methodName = method.getName();
			if ("toString".equals(methodName)) {
				return "EngineProxy for connector:" + connector;
			}

			String[] strArgs;
			if (args.length > 1 && args[1] instanceof String[]) {
				int size = ((String[]) args[1]).length;
				strArgs = new String[size + 1];
				strArgs[0] = args[0].toString();
				System.arraycopy(args[1], 0, strArgs, 1, size);
			} else {
				strArgs = Arrays.stream(args).map(Object::toString).toArray(String[]::new);
			}
			return connector.invokeChaincode(methodName, strArgs);
		});
	}
}
