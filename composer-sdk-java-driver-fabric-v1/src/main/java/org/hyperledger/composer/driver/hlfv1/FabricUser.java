/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.driver.hlfv1;

import org.hyperledger.composer.client.ComposerUser;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;

import java.security.PrivateKey;
import java.util.Set;

public class FabricUser implements User {
	private ComposerUser user;

	public FabricUser(ComposerUser user) {
		this.user = user;
	}

	public ComposerUser getComposerUser() {
		return user;
	}

	@Override
	public String getName() {
		return user.getName();
	}

	@Override
	public Set<String> getRoles() {
		return user.getRoles();
	}

	@Override
	public String getAccount() {
		return user.getAccount();
	}

	@Override
	public String getAffiliation() {
		return user.getAffiliation();
	}

	@Override
	public Enrollment getEnrollment() {
		return new ComposerEnrollment(user.getPrivateKey(), user.getCert());
	}

	@Override
	public String getMspId() {
		return user.getMspId();
	}

	static boolean isEnrolled(User user) {
		return user != null && user.getEnrollment() != null;
	}

	private class ComposerEnrollment implements Enrollment {
		private PrivateKey key;
		private String cert;

		public ComposerEnrollment(PrivateKey key, String cert) {
			this.key = key;
			this.cert = cert;
		}

		@Override
		public PrivateKey getKey() {
			return key;
		}

		@Override
		public String getCert() {
			return cert;
		}
	}
}
