/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.client;

import java.io.Serializable;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.Set;

/**
 * Harmony user compatible to fabric-sdk-java
 */
public class ComposerUser implements Identifiable, Serializable {

	private static final long serialVersionUID = 4262606818133755087L;

	private String name;
	private String affiliation;
	private String account;
	private String mspId;
	private PrivateKey privateKey;
	private String cert;

	public ComposerUser(ComposerUser that) {
		this(that.name, that.mspId, that.affiliation, that.privateKey, that.cert);
	}

	public ComposerUser(String name, String mspId, String affiliation, PrivateKey privateKey, String cert) {
		this.name = name;
		this.account = name;
		this.mspId = mspId;
		this.affiliation = affiliation;
		this.cert = cert;
		this.privateKey = privateKey;
	}

	/**
	 * Name - what identifies the user.
	 *
	 * @return - User's name.
	 */

	public String getName() {
		return this.name;
	}

	/**
	 * The roles
	 *
	 * @return
	 */
	public Set<String> getRoles() {
		return Collections.emptySet();
	}

	/**
	 * Users account
	 *
	 * @return
	 */
	public String getAccount() {
		return this.account;
	}

	/**
	 * Users affiliation
	 *
	 * @return
	 */
	public String getAffiliation() {
		return this.affiliation;
	}

	/**
	 * An ID provided by the users organization
	 *
	 * @return mspid
	 */
	public String getMspId() {
		return this.mspId;
	}

	/**
	 * Get the user's private key
	 *
	 * @return private key.
	 */
	public PrivateKey getPrivateKey() {
		return this.privateKey;
	}

	/**
	 * Get the user's signed certificate.
	 *
	 * @return a certificate.
	 */
	public String getCert() {
		return this.cert;
	}

	@Override
	public String getId() {
		return name + "@" + (affiliation == null || affiliation.isEmpty() ? "__composerDefault" : affiliation);
	}

	@Override
	public String toString() {
		return "ComposerUser{" +
				"name='" + name + '\'' +
				", affiliation='" + affiliation + '\'' +
				", account='" + account + '\'' +
				", mspId='" + mspId + '\'' +
				", privateKey=" + privateKey +
				", cert='" + cert + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ComposerUser that = (ComposerUser) o;

		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (affiliation != null ? !affiliation.equals(that.affiliation) : that.affiliation != null) return false;
		if (account != null ? !account.equals(that.account) : that.account != null) return false;
		if (mspId != null ? !mspId.equals(that.mspId) : that.mspId != null) return false;
		if (privateKey != null ? !privateKey.equals(that.privateKey) : that.privateKey != null) return false;
		return cert != null ? cert.equals(that.cert) : that.cert == null;
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (affiliation != null ? affiliation.hashCode() : 0);
		result = 31 * result + (account != null ? account.hashCode() : 0);
		result = 31 * result + (mspId != null ? mspId.hashCode() : 0);
		result = 31 * result + (privateKey != null ? privateKey.hashCode() : 0);
		result = 31 * result + (cert != null ? cert.hashCode() : 0);
		return result;
	}
}
