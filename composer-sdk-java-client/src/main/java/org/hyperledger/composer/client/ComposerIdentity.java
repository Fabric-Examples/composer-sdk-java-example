/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.client;

import java.io.Serializable;
import java.util.ArrayList;

import org.apache.commons.lang3.tuple.Pair;

public class ComposerIdentity implements Identifiable, Serializable {
	private String userID;
	private String type;
	private String secret;
	private int maxEnrollments;
	private String affiliation;
	private final ArrayList<Pair<String, String>> attrs;

	private boolean issuer;

	public ComposerIdentity() {
		this.attrs = new ArrayList<>();
	}
	
	public ComposerIdentity(ComposerIdentity that) {
		this();
		this.userID = that.userID;
		this.type = that.type;
		this.secret = that.secret;
		this.maxEnrollments = that.maxEnrollments;
		this.affiliation = that.affiliation;
		this.attrs.addAll(that.attrs);
	}

	public ComposerIdentity userID(String userID) {
		this.userID = userID;
		return this;
	}

	public ComposerIdentity type(String type) {
		this.type = type;
		return this;
	}

	public ComposerIdentity secret(String secret) {
		this.secret = secret;
		return this;
	}

	public ComposerIdentity maxEnrollments(int maxEnrollments) {
		this.maxEnrollments = maxEnrollments;
		return this;
	}

	public ComposerIdentity issuer(boolean issuer) {
		this.issuer = issuer;
		return this;
	}

	public ComposerIdentity addAttr(String key, String value) {
		attrs.add(Pair.of(key, value));
		return this;
	}

	public ArrayList<Pair<String, String>> attrs() {
		return attrs;
	}

	public String userID() {
		return userID;
	}

	public String type() {
		return type;
	}

	public String secret() {
		return secret;
	}

	public int maxEnrollments() {
		return maxEnrollments;
	}

	public String affiliation() {
		return affiliation;
	}

	public ComposerIdentity affiliation(String affiliation) {
		this.affiliation = affiliation;
		return this;
	}

	public boolean issuer() {
		return issuer;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ComposerIdentity that = (ComposerIdentity) o;

		return maxEnrollments == that.maxEnrollments && issuer == that.issuer
				&& (userID != null ? userID.equals(that.userID) : that.userID == null)
				&& (type != null ? type.equals(that.type) : that.type == null)
				&& (secret != null ? secret.equals(that.secret) : that.secret == null)
				&& (affiliation != null ? affiliation.equals(that.affiliation) : that.affiliation == null)
				&& attrs.equals(that.attrs);
	}

	@Override
	public int hashCode() {
		int result = userID != null ? userID.hashCode() : 0;
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + (secret != null ? secret.hashCode() : 0);
		result = 31 * result + maxEnrollments;
		result = 31 * result + (affiliation != null ? affiliation.hashCode() : 0);
		result = 31 * result + attrs.hashCode();
		result = 31 * result + (issuer ? 1 : 0);
		return result;
	}

	@Override
	public String toString() {
		return "ComposerIdentity{" +
				"userID='" + userID + '\'' +
				", type='" + type + '\'' +
				", secret='" + secret + '\'' +
				", maxEnrollments=" + maxEnrollments +
				", affiliation='" + affiliation + '\'' +
				", attrs=" + attrs +
				", issuer=" + issuer +
				'}';
	}

	@Override
	public String getId() {
		return userID + "@" + (affiliation == null || affiliation.isEmpty() ? "__composerDefault" : affiliation);
	}
}
