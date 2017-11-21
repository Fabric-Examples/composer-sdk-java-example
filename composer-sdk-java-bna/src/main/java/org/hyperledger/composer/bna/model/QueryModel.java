/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.model;

public class QueryModel {
	private String description;
	private String sql;

	public QueryModel(String description, String sql) {
		this.description = description;
		this.sql = sql;
	}

	public String description() {
		return description;
	}

	public String sql() {
		return sql;
	}

	@Override
	public String toString() {
		return "QueryModel{" +
				"description='" + description + '\'' +
				", sql='" + sql + '\'' +
				'}';
	}
}
