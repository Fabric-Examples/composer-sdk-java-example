/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.query;

import org.hyperledger.composer.ComposerException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class QueryBuilderTest {

	@Test
	public void build() throws ComposerException {
		QueryBuilder<TestAsset> builder = new QueryBuilder<>(TestAsset.class, null);
		CompiledQuery<TestAsset> query = builder.where("strValue == ? and longValue > ?").orderBy("strValue ASC").limit(0).skip(1).build();
		String expectedSql = "SELECT org.hyperledger.composer.query.TestAsset WHERE (strValue == _$v0 and longValue > _$v1) ORDER BY [strValue ASC] LIMIT 0 SKIP 1";
		Assert.assertEquals(query.getSQL(), expectedSql);
	}

	@Test(expectedExceptions = ComposerException.class)
	public void notAllowQuestionMarkForOrderBy() throws ComposerException {
		QueryBuilder<TestAsset> builder = new QueryBuilder<>(TestAsset.class, null);
		builder.where("strValue == ? and longValue > ?").orderBy("? ASC").limit(0).skip(1).build();
	}


	@Test(expectedExceptions = ComposerException.class)
	public void notAllowIllegalLimit() throws ComposerException {
		QueryBuilder<TestAsset> builder = new QueryBuilder<>(TestAsset.class, null);
		builder.where("strValue == ? and longValue > ?").orderBy("longValue ASC").limit("ss").skip(1).build();
	}

	@Test(expectedExceptions = ComposerException.class)
	public void notAllowIllegalSkip() throws ComposerException {
		QueryBuilder<TestAsset> builder = new QueryBuilder<>(TestAsset.class, null);
		builder.where("strValue == ? and longValue > ?").orderBy("longValue ASC").limit("0").skip("????").build();
	}

	@Test
	public void allowNumericSkip() throws ComposerException {
		QueryBuilder<TestAsset> builder = new QueryBuilder<>(TestAsset.class, null);
		builder.where("strValue == ? and longValue > ?").orderBy("longValue ASC").limit(0).skip("123").build();
	}

	@Test
	public void allowQuestionLimitAndSkip() throws ComposerException {
		QueryBuilder<TestAsset> builder = new QueryBuilder<>(TestAsset.class, null);
		builder.where("strValue == ? and longValue > ?").orderBy("longValue ASC").limit("?").skip("?").build();
	}
}
