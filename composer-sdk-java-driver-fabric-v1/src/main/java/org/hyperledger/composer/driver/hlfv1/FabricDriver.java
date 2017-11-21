/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.driver.hlfv1;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.composer.ComposerException;
import org.hyperledger.composer.client.*;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;

import java.io.IOException;

import static org.hyperledger.composer.ComposerException.PARSE_ERROR_CODE;
import static org.hyperledger.composer.ComposerException.SERVER_NOT_FOUND_CODE;

public class FabricDriver implements ComposerDriver<ComposerUser, ComposerConnector> {
	private ObjectMapper mapper;

	static {
		ComposerDriverManager.registerDriver(new FabricDriver());
	}

	FabricDriver(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	FabricDriver() {
		this(getDefaultMapper());
	}

	private static ObjectMapper getDefaultMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
				.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
		return mapper;
	}


	@Override
	public ComposerConnector getConnector(String connString, Wallet<ComposerUser> userWallet) throws ComposerException {
		JsonNode node;
		try {
			node = mapper.readTree(connString);
		} catch (IOException e) {
			throw new ComposerException(PARSE_ERROR_CODE, "error when parsing connection string " + connString, e);
		}

		if (acceptsConnectionString(connString)) {
			ConnectionOptions connectOptions;
			try {
				connectOptions = mapper.treeToValue(node, ConnectionOptions.class);
			} catch (JsonProcessingException e) {
				throw new ComposerException(PARSE_ERROR_CODE, "error when parsing connection string " + connString, e);
			}

			connectOptions.check();

			HFCAClient caClient;
			try {
				caClient = HFCAClient.createNewInstance(connectOptions.ca(), connectOptions.caProperties());
				caClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
			} catch (Exception e) {
				throw new ComposerException(SERVER_NOT_FOUND_CODE, "Invalid CA URL: " + connectOptions.ca(), e);
			}

			HFClient client = HFClient.createNewInstance();
			try {
				client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
			} catch (Exception e) {
				throw new ComposerException(ComposerException.FABRIC_SDK_ERROR, e.getMessage(), e);
			}
			return new FabricConnector(connectOptions, client, caClient, userWallet);
		}
		return null;
	}

	boolean acceptsConnectionNode(JsonNode node) {
		return node.isObject() && node.has("protocol") && "fabric.v1".equals(node.get("protocol").asText());
	}

	@Override
	public boolean acceptsConnectionString(String connStr) {
		JsonNode node;
		try {
			node = mapper.readTree(connStr);
		} catch (IOException e) {
			// not supported
			return false;
		}
		return acceptsConnectionNode(node);
	}

}
