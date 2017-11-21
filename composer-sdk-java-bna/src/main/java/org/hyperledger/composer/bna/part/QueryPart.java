/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.part;

import org.hyperledger.composer.bna.model.QueryModel;

import java.util.LinkedList;

public class QueryPart extends BNAPart {

	public QueryPart() {
		super("queries.qry");
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		int queryId = 0;
		for (QueryModel model : new LinkedList<QueryModel>(entries)) {
			result.append("query Q").append(queryId++).append(" {\n");
			result.append("\tdescription: \"").append(model.description()).append("\"\n");
			result.append("\tstatement: ").append(model.sql()).append("\n}\n\n");
		}
		return result.toString();
	}
}
