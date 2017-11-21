/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.driver.hlfv1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.hyperledger.composer.ComposerException;
import org.hyperledger.composer.client.ComposerConnector;
import org.hyperledger.composer.client.ComposerUser;
import org.hyperledger.composer.client.Wallet;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class FabricDriverTest {

	@Mock
	ObjectMapper mapper;

	@Mock
	JsonNode connectionOptionNode;

	@Mock
	JsonNode protocolNode;

	@Mock
	JsonProcessingException exception;

	@Mock
	Wallet<ComposerUser> wallet;

	@Spy
	ConnectionOptions connectionOptions = new ConnectionOptions()
			.addOrderer("orderer", "grpc://localhost:7050")
			.addPeer("peer1", "grpc://localhost:7051")
			.addEventHub("peer1", "grpc://localhost:7053")
			.ca("http://localhost:7054")
			.addCAProperties("allowAllHostNames", "true")
			.channel("yzhchannel")
			.chaincodeId("myfoo")
			.mspId("YzhMSP")
			.invokeWaitMillis(6000 * 1000);

	@Spy
	@InjectMocks
	FabricDriver driver;

	@BeforeMethod(alwaysRun = true)
	public void init() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testGetConnector() throws Exception {
		when(mapper.readTree("connString")).thenReturn(connectionOptionNode);
		when(connectionOptionNode.getNodeType()).thenReturn(JsonNodeType.OBJECT);
		when(connectionOptionNode.has("protocol")).thenReturn(true);
		when(connectionOptionNode.get("protocol")).thenReturn(protocolNode);
		when(protocolNode.asText()).thenReturn("fabric.v1");
		when(mapper.treeToValue(connectionOptionNode, ConnectionOptions.class)).thenReturn(connectionOptions);

		ComposerConnector connector = driver.getConnector("connString", wallet);
		checkFieldNotNull(connector, "connectOptions");
		checkFieldNotNull(connector, "client");
		checkFieldNotNull(connector, "caClient");
		checkFieldNotNull(connector, "client");
		checkFieldNotNull(connector, "wallet");
	}

	private void checkFieldNotNull(Object o, String fieldName) throws Exception{
		Field f = o.getClass().getDeclaredField(fieldName);
		f.setAccessible(true);
		assertNotNull(f.get(o));
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "error when parsing connection string errorString")
	public void testGetConnectorErrorJson() throws Exception {
		doThrow(new IOException("error")).when(mapper).readTree("errorString");
		driver.getConnector("errorString", wallet);
	}

	@Test
	public void testGetConnectorReject() throws Exception {when(mapper.readTree("connString")).thenReturn(connectionOptionNode);
		doThrow(exception).when(mapper).treeToValue(connectionOptionNode, ConnectionOptions.class);
		doReturn(false).when(driver).acceptsConnectionNode(connectionOptionNode);
		assertNull(driver.getConnector("connString", wallet));
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "error when parsing connection string connString")
	public void testGetConnectorErrorJsonNode() throws Exception {
		when(mapper.readTree("connString")).thenReturn(connectionOptionNode);
		doThrow(exception).when(mapper).treeToValue(connectionOptionNode, ConnectionOptions.class);
		doReturn(true).when(driver).acceptsConnectionNode(connectionOptionNode);
		driver.getConnector("connString", wallet);
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "Invalid CA URL: errorCA")
	public void testGetConnectorMalformedCA() throws Exception {
		when(mapper.readTree("connString")).thenReturn(connectionOptionNode);
		when(mapper.treeToValue(connectionOptionNode, ConnectionOptions.class)).thenReturn(connectionOptions);
		when(connectionOptions.ca()).thenReturn("errorCA");
		doReturn(true).when(driver).acceptsConnectionNode(connectionOptionNode);
		driver.getConnector("connString", wallet);
	}

	@Test
	public void testAcceptsConnectionString() throws Exception {
		when(mapper.readTree("connString")).thenReturn(connectionOptionNode);
		when(connectionOptionNode.getNodeType()).thenReturn(JsonNodeType.OBJECT);
		when(connectionOptionNode.has("protocol")).thenReturn(true);
		when(connectionOptionNode.get("protocol")).thenReturn(protocolNode);
		when(protocolNode.asText()).thenReturn("fabric.v1");

		assertTrue(driver.acceptsConnectionString("connString"));
	}

	@Test
	public void testAcceptsConnectionStringReject() throws Exception {
		doThrow(new IOException()).when(mapper).readTree("connString");
		assertFalse(driver.acceptsConnectionString("connString"));
	}
}