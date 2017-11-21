/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.model;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class QueryModelTest {

	private QueryModel model;

	@Test
	public void testQueryModel() {
		model = new QueryModel("description", "sql");
		assertEquals(model.description(), "description");
		assertEquals(model.sql(), "sql");
		System.out.println(model.toString());
	}
}