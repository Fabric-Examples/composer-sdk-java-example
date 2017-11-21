/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.client;

import org.hyperledger.composer.ComposerException;

public class RegisterRequest {
	private String affiliation;
	private String userId;
	private String adminId;

	public RegisterRequest() {
	}

	public RegisterRequest(RegisterRequest that) {
		this();
		this.userId = that.userId;
		this.affiliation = that.affiliation;
		this.adminId = that.adminId;
	}

	public RegisterRequest affiliation(String affiliation) {
		this.affiliation = affiliation;
		return this;
	}

	public RegisterRequest userId(String userId) {
		this.userId = userId;
		return this;
	}

	public RegisterRequest adminId(String adminId) {
		this.adminId = adminId;
		return this;
	}

	protected String register(ComposerConnector connector) throws ComposerException {
		if(adminId == null || adminId.isEmpty()) {
			throw new ComposerException(ComposerException.INVALID_INPUT_ERROR, "adminId for registering is not specified");
		}
		if(userId == null || userId.isEmpty()) {
			throw new ComposerException(ComposerException.INVALID_INPUT_ERROR, "userId for registering is not specified");
		}
		connector.login(new ComposerIdentity().affiliation(affiliation).type("user").userID(adminId));
 		return connector.register(new ComposerIdentity().affiliation(affiliation).type("user").userID(userId));
	}

	protected EnrollRequest toEnrollRequest(String secret) {
		return new EnrollRequest().userId(userId).affiliation(affiliation).secret(secret);
	}
}
