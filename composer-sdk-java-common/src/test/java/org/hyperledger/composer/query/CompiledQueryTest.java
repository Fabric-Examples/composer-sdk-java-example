/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.query;

import org.hyperledger.composer.ComposerAPI;
import org.hyperledger.composer.ComposerException;
import org.hyperledger.composer.annotation.Asset;
import org.hyperledger.composer.annotation.DataField;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;


public class CompiledQueryTest {

	@Mock
	ComposerAPI api;

	@BeforeMethod(alwaysRun = true)
	public void init() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void execute() throws ComposerException {
		QueryBuilder<TestAsset> builder = new QueryBuilder<>(TestAsset.class, api);
		CompiledQuery<TestAsset> query = builder.where("strValue == ? and longValue > ?").orderBy("strValue ASC")
				.limit(0).skip(1).build();
		query.bind(1, "123");
		query.bind(2, 123);
		query.execute();
		verify(api).executeQuery(eq(TestAsset.class), eq("build"),
				eq("SELECT org.hyperledger.composer.query.TestAsset WHERE (strValue == _$v0 and longValue > _$v1) ORDER BY [strValue ASC] LIMIT 0 SKIP 1"),
				eq("{\"v0\":\"123\",\"v1\":123}"));
	}

	@Test
	public void getSQL() throws ComposerException {
		QueryBuilder<TestAsset> builder = new QueryBuilder<>(TestAsset.class, api);
		CompiledQuery<TestAsset> query = builder.where("strValue == ? and longValue > ?").orderBy("strValue ASC")
				.limit(0).skip(1).build();
		String expectedSql = "SELECT org.hyperledger.composer.query.TestAsset WHERE (strValue == _$v0 and longValue > _$v1) ORDER BY [strValue ASC] LIMIT 0 SKIP 1";
		assertEquals(query.getSQL(), expectedSql);
	}

	@Test
	public void getSimpleSQL() throws ComposerException {
		QueryBuilder<TestAsset> builder = new QueryBuilder<>(TestAsset.class, api);
		CompiledQuery<TestAsset> query = builder.build();
		String expectedSql = "SELECT org.hyperledger.composer.query.TestAsset";
		assertEquals(query.getSQL(), expectedSql);
	}

	@Test
	public void getDisoderedOrderSQL() throws ComposerException {
		QueryBuilder<TestAsset> builder = new QueryBuilder<>(TestAsset.class, api);
		CompiledQuery<TestAsset> query = builder.where("strValue == ? and longValue > ?")
				.limit(0).skip(1).orderBy("strValue ASC").build();
		String expectedSql = "SELECT org.hyperledger.composer.query.TestAsset WHERE (strValue == _$v0 and longValue > _$v1) ORDER BY [strValue ASC] LIMIT 0 SKIP 1";
		assertEquals(query.getSQL(), expectedSql);
	}

	@Test
	public void getDisoderedLimitSQL() throws ComposerException {
		QueryBuilder<TestAsset> builder = new QueryBuilder<>(TestAsset.class, api);
		CompiledQuery<TestAsset> query = builder.limit("?").where("strValue == ? and longValue > ?").orderBy("strValue ASC")
				.skip(1).build();
		String expectedSql = "SELECT org.hyperledger.composer.query.TestAsset WHERE (strValue == _$v1 and longValue > _$v2) ORDER BY [strValue ASC] LIMIT _$v0 SKIP 1";
		assertEquals(query.getSQL(), expectedSql);
	}
}

@Asset
class TestAsset {
	@DataField(primary = true)
	public String id;

	@DataField
	public String strValue;

	@DataField
	public Long longValue;

}