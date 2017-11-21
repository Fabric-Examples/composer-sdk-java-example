/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.util;

import org.hyperledger.composer.ComposerException;
import org.hyperledger.composer.annotation.*;
import org.hyperledger.composer.annotation.Enum;
import org.hyperledger.composer.query.QueryBuilder;
import org.hyperledger.composer.query.SelectQuery;

import java.util.ArrayList;

class QueryTest {
	@Query(description = "description")
	public QueryBuilder normalQuery(SelectQuery query) throws ComposerException {
		return query.from(TestAsset.class).where("id = '1'").orderBy("id DESC").limit(10).skip(10);
	}

	@Query
	public QueryBuilder tooManyArgsQuery(SelectQuery query, TestAsset noUse) throws ComposerException {
		return query.from(TestAsset.class);
	}

	@Query
	public TestAsset errorReturnQuery(SelectQuery query) throws ComposerException {
		return new TestAsset();
	}

	@Query
	public QueryBuilder errorArgTypeQuery(TestAsset query) throws ComposerException {
		return new SelectQuery().from(TestAsset.class);
	}

	@Query
	public QueryBuilder withException(SelectQuery query) {
		throw new IllegalArgumentException("exception");
	}
}

@Asset
class TestAsset extends BaseAsset {
	@Pointer
	public TestParticipant user;

	public Long l;

	@DataField
	public TestEnum e;
}

@Asset
class BaseAsset {
	@DataField(primary = true)
	public String id;
}

@Participant
class TestParticipant {
	@DataField(primary = true)
	public String id;
}

@Enum
enum TestEnum {
	A, B, C
}

@Asset
class IllegalParent extends ArrayList {
	@DataField(primary = true)
	private String id;
}