/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.model;

import org.hyperledger.composer.annotation.Concept;
import org.hyperledger.composer.annotation.DataField;

import java.io.Serializable;

@Concept
public class Entry implements Serializable{
	@DataField
	private String key;

	@DataField
	private String value;

	public String key() {
		return key;
	}

	public Entry key(String key) {
		this.key = key;
		return this;
	}

	public String value() {
		return value;
	}

	public Entry value(String value) {
		this.value = value;
		return this;
	}

	@Override
	public String toString() {
		return "Entry{" +
				"key='" + key + '\'' +
				", value='" + value + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Entry entry = (Entry) o;

		return (key != null ? key.equals(entry.key) : entry.key == null)
				&& (value != null ? value.equals(entry.value) : entry.value == null);
	}

	@Override
	public int hashCode() {
		int result = key != null ? key.hashCode() : 0;
		result = 31 * result + (value != null ? value.hashCode() : 0);
		return result;
	}
}
