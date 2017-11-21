/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.client;

import org.hyperledger.composer.ComposerException;

public interface ComposerCA {

	/**
	 * Bind a blockchain userId for a participant
	 * @param participant
	 * @param userId
	 * @throws ComposerException
	 */
	void issueIdentity(Object participant, String userId) throws ComposerException;

	/**
	 * Revoke the password assigned to a participant
	 * 
	 * @param userId
	 * @throws ComposerException
	 */
	void revokeIdentity(String userId) throws ComposerException;
}
