/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.driver.hlfv1;

import org.hyperledger.composer.ComposerException;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.hyperledger.composer.ComposerException.INVALID_INPUT_ERROR;

class ConnectionOptions {
    private final List<Host> orderers = new ArrayList<>();
    private final List<Host> peers = new ArrayList<>();
    private final List<Host> events = new ArrayList<>();
    private String ca, channel, mspId;
    private int invokeWaitMillis;
    private final Properties caProperties = new Properties();
	private String ccId;

    int invokeWaitMillis() {
        return invokeWaitMillis == 0 ? 60 * 1000 : invokeWaitMillis;
    }

    String chaincodeId() {
        return ccId;
    }

    String channel() {
        return channel;
    }

    Iterable<Host> orderers() {
        return orderers;
    }

    Iterable<Host> peers() {
        return peers;
    }

    Iterable<Host> events() {
        return events;
    }

    Properties caProperties() {
        return caProperties.isEmpty() ? null : caProperties;
    }

    String ca() {
        return ca;
    }

    String mspId() {
        return mspId;
    }

    ConnectionOptions addOrderer(String name, String url) {
        return addOrderer(name, url, null);
    }

    ConnectionOptions addOrderer(String name, String url, Properties properties) {
        this.orderers.add(new Host(name, url, properties));
        return this;
    }

    ConnectionOptions addPeer(String name, String url) {
        return addPeer(name, url, null);
    }

    ConnectionOptions addPeer(String name, String url, Properties properties) {
        this.peers.add(new Host(name, url, properties));
        return this;
    }

    ConnectionOptions addEventHub(String name, String url) {
        return addEventHub(name, url, null);
    }

    ConnectionOptions addEventHub(String name, String url, Properties properties) {
        this.events.add(new Host(name, url, properties));
        return this;
    }

    ConnectionOptions ca(String ca) {
        this.ca = ca;
        return this;
    }

    ConnectionOptions chaincodeId(String ccId) {
        this.ccId = ccId;
        return this;
    }

    ConnectionOptions channel(String channel) {
        this.channel = channel;
        return this;
    }

    ConnectionOptions mspId(String mspid) {
        this.mspId = mspid;
        return this;
    }

    ConnectionOptions invokeWaitMillis(int invokeWaitMillis) {
        this.invokeWaitMillis = invokeWaitMillis;
        return this;
    }

    ConnectionOptions addCAProperties(String key, String value) {
        this.caProperties.setProperty(key, value);
        return this;
    }

	void check() throws ComposerException {
		checkListField(orderers, "orderer URLs");
		checkListField(peers, "peer URLs");
		checkListField(events, "event hub URLs");
		if (events.size() != peers.size()) {
			throw new ComposerException(INVALID_INPUT_ERROR, "there should be an identical number of event hub urls to peers");
		}
		checkField(ca, "certificate authority URL");
		checkField(channel, "channel");
		checkField(mspId, "msp id");
		checkField(ccId, "chaincode id");
	}

	private static void checkField(Object field, String name) throws ComposerException {
		if (field == null) {
			throw new ComposerException(INVALID_INPUT_ERROR, "The " + name + " has not been specified in the connection profile");
		}
	}

	private static void checkListField(List field, String name) throws ComposerException {
		checkField(field, name);
		if (field.isEmpty()) {
			throw new ComposerException(INVALID_INPUT_ERROR, "No " + name + " have been specified in the connection profile");
		}
	}

}
