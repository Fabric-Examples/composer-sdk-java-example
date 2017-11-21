/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.part;

import org.hyperledger.composer.bna.model.Model;
import org.hyperledger.composer.bna.model.QueryModel;
import org.testng.annotations.Test;

import java.io.InputStream;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

public class BNAPartTest {

	@Test
	public void testCTOPart() {
		CTOPart ctoPart = new CTOPart("ns");
		ctoPart.addEntry("dependency");
		ctoPart.addEntry(when(mock(Model.class).toString()).thenReturn("asset A identified by id {\n\to strin id\n}").getMock());
		assertEquals(ctoPart.entryName(), "models/ns.cto");
		assertEquals(ctoPart.toString(), "namespace ns\nimport dependency\n\nasset A identified by id {\n\to strin id\n}");
	}

	@Test
	public void testPackageJSON() {
		PackageJsonPart packageJson = new PackageJsonPart("name", "version", "description");
		assertEquals(packageJson.toString(), "{\n" +
				"\t\"name\": \"name\",\n" +
				"\t\"version\": \"version\",\n" +
				"\t\"description\": \"description\"\n" +
				"}");
	}

	@Test
	public void testQueryPart() {
		QueryPart queryPart = new QueryPart();
		QueryModel mock = mock(QueryModel.class);
		when(mock.description()).thenReturn("description");
		when(mock.sql()).thenReturn("SELECT org.acme.model.Table WHERE (value > 'a')");
		queryPart.addEntry(mock);
		assertEquals(queryPart.toString(), "query Q0 {\n" +
				"\tdescription: \"description\"\n" +
				"\tstatement: SELECT org.acme.model.Table WHERE (value > 'a')\n" +
				"}\n\n");
		verify(mock).description();
		verify(mock).sql();
	}

	@Test
	public void testAclPart() throws Exception {
		ACLPart aclPart = new ACLPart();
		InputStream in = getClass().getClassLoader().getResourceAsStream("default.acl");
		byte[] b = new byte[4096];
		int count = in.read(b);
		assertEquals(aclPart.toString(), new String(b, 0, count));
	}

}