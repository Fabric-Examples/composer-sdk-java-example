/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.query;

import org.hyperledger.composer.ComposerAPI;
import org.hyperledger.composer.ComposerException;

public class QueryBuilder<T> {
	private String condition;
	private String limit;
	private String skip;
	private String[] orderFields;

	private int conditionOrder;
	private int orderOrder;
	private int limitOrder;
	private int skipOrder;

	private int currentOrder;
	private final Class<T> clazz;
	private ComposerAPI api;

	QueryBuilder(Class<T> clazz, ComposerAPI api) {
		this.clazz = clazz;
		this.conditionOrder = -1;
		this.orderOrder = -1;
		this.limitOrder = -1;
		this.skipOrder = -1;
		this.currentOrder = 0;
		this.api = api;
	}

	protected QueryBuilder(QueryBuilder<T> that) {
		this(that.clazz, that.api);
	}

	public QueryBuilder<T> where(String condition) throws ComposerException {
		if (this.conditionOrder >= 0) {
			throw new ComposerException(ComposerException.INVALID_INPUT_ERROR,
					"where clause can only be specified for once");
		}
		this.conditionOrder = this.currentOrder++;
		this.condition = condition;
		return this;
	}

	public QueryBuilder<T> orderBy(String... fields) throws ComposerException {
		if (this.orderOrder >= 0) {
			throw new ComposerException(ComposerException.INVALID_INPUT_ERROR,
					"orderBy clause can only be specified for once");
		}
		this.orderOrder = this.currentOrder++;
		for (String field : fields) {
			if (!field.endsWith("ASC") && !field.endsWith("DESC")) {
				throw new ComposerException(ComposerException.INVALID_INPUT_ERROR,
						"orderBy fields must end with either ASC or DESC.");
			}

			if (field.indexOf('?') >= 0) {
				throw new ComposerException(ComposerException.INVALID_INPUT_ERROR,
						"Cannot specify parameter for orderBy fields.");
			}
		}
		this.orderFields = fields;
		return this;
	}

	public QueryBuilder<T> limit(int limit) throws ComposerException {
		return limit(String.valueOf(limit));
	}

	public QueryBuilder<T> limit(String limit) throws ComposerException {
		if (this.limitOrder >= 0) {
			throw new ComposerException(ComposerException.INVALID_INPUT_ERROR,
					"where clause can only be specified for once");
		}
		this.limitOrder = this.currentOrder++;
		if (!"?".equals(limit)) {
			try {
				Integer.parseInt(limit);
			} catch (NumberFormatException e) {
				throw new ComposerException(ComposerException.INVALID_INPUT_ERROR,
						"limit should either be a number or '?'");
			}
		}
		this.limit = limit;
		return this;
	}

	public QueryBuilder<T> skip(long skip) throws ComposerException {
		return skip(String.valueOf(skip));
	}

	public QueryBuilder<T> skip(String skip) throws ComposerException {
		if (this.skipOrder >= 0) {
			throw new ComposerException(ComposerException.INVALID_INPUT_ERROR,
					"skip clause can only be specified for once");
		}
		this.skipOrder = this.currentOrder++;
		if (!"?".equals(skip)) {
			try {
				Long.parseLong(skip);
			} catch (NumberFormatException e) {
				throw new ComposerException(ComposerException.INVALID_INPUT_ERROR,
						"skip should either be a number or '?'");
			}
		}
		this.skip = skip;
		return this;
	}

	public CompiledQuery<T> build() {
		// return the query for execution
		return new CompiledQuery<>(this.api, clazz, this.condition, this.conditionOrder, this.orderFields, this.orderOrder,
                this.limit, this.limitOrder, this.skip, this.skipOrder);
	}

}