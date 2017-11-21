/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.client;

import org.hyperledger.composer.ComposerException;

public class EnrollRequest {
	private String userId;
	private String secret;
	private String affiliation;
	private boolean ignoreExisting;

	public EnrollRequest() {
	}

	public EnrollRequest(EnrollRequest that) {
		this();
		this.userId = that.userId;
		this.secret = that.secret;
		this.affiliation = that.affiliation;
		this.ignoreExisting = that.ignoreExisting;
	}

	public EnrollRequest userId(String userId) {
		this.userId = userId;
		return this;
	}

	public EnrollRequest secret(String secret) {
		this.secret = secret;
		return this;
	}

	public EnrollRequest affiliation(String affiliation) {
		this.affiliation = affiliation;
		return this;
	}

	public EnrollRequest ignoreExisting(boolean ignoreExisting) {
		this.ignoreExisting = ignoreExisting;
		return this;
	}

	protected void enroll(ComposerConnector connector) throws ComposerException {
		if(userId == null || userId.isEmpty()) {
			throw new ComposerException(ComposerException.INVALID_INPUT_ERROR, "userId for enrolling not specified");
		}
		if(secret == null || secret.isEmpty()) {
			throw new ComposerException(ComposerException.INVALID_INPUT_ERROR, "secret for enrolling not specified");
		}
		connector.enroll(new ComposerIdentity().affiliation(affiliation).secret(secret).userID(userId), ignoreExisting);
	}
}
