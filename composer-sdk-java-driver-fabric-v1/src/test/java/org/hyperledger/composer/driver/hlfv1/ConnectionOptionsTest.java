/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.driver.hlfv1;

import org.hyperledger.composer.ComposerException;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.Properties;

import static org.testng.Assert.*;

public class ConnectionOptionsTest {

	private ConnectionOptions connectionOptions = new ConnectionOptions()
			.addOrderer("orderer", "grpc://localhost:7050")
			.addPeer("peer1", "grpc://localhost:7051")
			.addEventHub("peer1", "grpc://localhost:7053")
			.ca("http://localhost:7054")
			.addCAProperties("allowAllHostNames", "true")
			.channel("yzhchannel")
			.chaincodeId("myfoo")
			.mspId("YzhMSP")
			.invokeWaitMillis(6000 * 1000);

	@Test
	public void testInvokeWaitMillis() throws Exception {
		assertEquals(connectionOptions.invokeWaitMillis(), 6000 * 1000);
	}

	@Test
	public void testChaincodeId() throws Exception {
		assertEquals(connectionOptions.chaincodeId(), "myfoo");
	}

	@Test
	public void testChannel() throws Exception {
		assertEquals(connectionOptions.channel(), "yzhchannel");
	}

	@Test
	public void testOrderers() throws Exception {
		Iterator<Host> orderers = connectionOptions.orderers().iterator();
		assertTrue(orderers.hasNext());
		assertEquals(orderers.next().toString(), "{orderer:grpc://localhost:7050}");
		assertFalse(orderers.hasNext());
	}

	@Test
	public void testPeers() throws Exception {
		Iterator<Host> peers = connectionOptions.peers().iterator();
		assertTrue(peers.hasNext());
		assertEquals(peers.next().toString(), "{peer1:grpc://localhost:7051}");
		assertFalse(peers.hasNext());
	}

	@Test
	public void testEvents() throws Exception {
		Iterator<Host> events = connectionOptions.events().iterator();
		assertTrue(events.hasNext());
		assertEquals(events.next().toString(), "{peer1:grpc://localhost:7053}");
		assertFalse(events.hasNext());
	}

	@Test
	public void testCaProperties() throws Exception {
		Properties properties = connectionOptions.caProperties();
		assertNotNull(properties);
		assertEquals(properties.getProperty("allowAllHostNames"), "true");
	}

	@Test
	public void testCa() throws Exception {
		assertEquals(connectionOptions.ca(), "http://localhost:7054");
	}

	@Test
	public void testMspId() throws Exception {
		assertEquals(connectionOptions.mspId(), "YzhMSP");
	}

	@Test
	public void testCheck() throws Exception {
		connectionOptions.check();
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "there should be an identical number of event hub urls to peers")
	public void testCheckException() throws Exception {
		new ConnectionOptions()
				.addOrderer("orderer", "grpc://localhost:7050")
				.addPeer("peer1", "grpc://localhost:7051")
				.addEventHub("peer1", "grpc://localhost:7053")
				.addEventHub("peer2", "grpc://localhost:7054")
				.check();
	}
}
