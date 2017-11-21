/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.query;

import org.hyperledger.composer.ComposerAPI;

public class SelectQuery {

	private ComposerAPI api;

	public SelectQuery(ComposerAPI api) {
		this.api = api;
	}

	public SelectQuery() {
	}

	protected SelectQuery(SelectQuery that) {
		this(that.api);
	}

	public <T> QueryBuilder<T> from(Class<T> clazz) {
		return new QueryBuilder<>(clazz, api);
	}
}