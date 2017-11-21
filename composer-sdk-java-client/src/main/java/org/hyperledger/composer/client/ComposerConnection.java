/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.client;

import org.hyperledger.composer.ComposerAPI;
import org.hyperledger.composer.ComposerException;
import org.hyperledger.composer.ResourceSerializer;
import org.hyperledger.composer.system.Identity;
import org.hyperledger.composer.system.IssueIdentity;
import org.hyperledger.composer.system.RevokeIdentity;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


public class ComposerConnection<C extends ComposerConnector> extends ComposerAPI implements ComposerCA {

	private final C connector;
	private static final int INSERTION_BASH_SIZE = 500;

	ComposerConnection(C connector) {
		super(ComposerChaincodeAPI.asEngine(connector));
		this.connector = connector;
	}

	public void disconnect() {
		if (connector == null) return;
		connector.disconnect();
	}

	public void on(ComposerEventListener listener) throws ComposerException {
		if (connector == null) return;
		connector.on(listener);
	}

	@Override
	public void issueIdentity(Object participant, String userId) throws ComposerException {
		if (participant == null) {
			throw new ComposerException(ComposerException.INVALID_INPUT_ERROR, "participantType not specified");
		}
		IssueIdentity issueIdentity = new IssueIdentity();
		issueIdentity.identityName = userId;
		issueIdentity.participant = participant;
		submitTransaction(issueIdentity);
	}

	@Override
	public void revokeIdentity(String userId) throws ComposerException {
		if (userId == null || userId.isEmpty()) {
			throw new ComposerException(ComposerException.INVALID_INPUT_ERROR, "userId not specified");
		}
		RevokeIdentity revokeIdentity = new RevokeIdentity();
		revokeIdentity.identity = ResourceSerializer.fromID(userId, Identity.class);
		submitTransaction(revokeIdentity);
	}

	@Override
	public <T> void createAssets(Collection<T> assets) throws ComposerException {
		if (assets == null || assets.isEmpty()) return;
		Iterator<T> iterator = assets.iterator();
		List<T> list = new LinkedList<>();
		int i = 0;
		while (iterator.hasNext()) {
			list.add(iterator.next());
			if (i++ % INSERTION_BASH_SIZE == 0) {
				super.createAssets(list);
				list.clear();
			}
		}
		if (!list.isEmpty()) {
			super.createAssets(list);
		}
	}
}
