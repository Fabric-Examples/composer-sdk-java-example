/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.util;

import org.hyperledger.composer.ComposerException;
import org.hyperledger.composer.bna.part.CTOPart;
import org.hyperledger.composer.bna.part.QueryPart;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

public class BnaGeneratorTest {

	@Mock
	JavaParser javaParser;
	@Mock
	ZipHolder zipHolder;

	@BeforeClass
	public void init() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testGenerate() throws Exception {
		CTOPart ctoPart = spy(new CTOPart("ns"));
		QueryPart queryPart = spy(QueryPart.class);
		when(ctoPart.entryName()).thenReturn("ctoEntry");
		String ctoContent = "namespace ns\nimport dependency\n\nasset A identified by id {\n\to strin id\n}";
		when(ctoPart.toString()).thenReturn(ctoContent);
		when(javaParser.parseCTOModel()).thenReturn(Collections.singletonList(ctoPart));
		when(queryPart.entryName()).thenReturn("queryEntry");
		String queryContent = "query Q0 {\n" +
				"\tdescription: \"description\"\n" +
				"\tstatement: SELECT org.acme.model.Table WHERE (value > 'a')\n" +
				"}\n\n";
		when(queryPart.toString()).thenReturn(queryContent);
		when(javaParser.parseQueryModel()).thenReturn(queryPart);
		String packageJson = "{\n" +
				"\t\"name\": \"pk\",\n" +
				"\t\"version\": \"ver\",\n" +
				"\t\"description\": \"des\"\n" +
				"}";
		byte[] b = new byte[4096];
		int count = getClass().getClassLoader().getResourceAsStream("default.acl").read(b);
		String acl = new String(b, 0, count);
		new BnaGenerator(javaParser, zipHolder, "pk", "ver", "des")
				.generate();
		ArgumentCaptor<String> entryName = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<byte[]> content = ArgumentCaptor.forClass(byte[].class);
		verify(zipHolder, times(4)).addPart(entryName.capture(), content.capture());
		verify(zipHolder).end();
		assertEquals(entryName.getAllValues(), Arrays.asList("ctoEntry", "queryEntry", "package.json", "permissions.acl"));
		Iterator<String> iterator = Arrays.asList(ctoContent, queryContent, packageJson, acl).iterator();
		for (byte[] bytes : content.getAllValues()) {
			assertEquals(new String(bytes), iterator.next());
		}
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "Permission Denied")
	void testIOException() throws Exception {
		when(javaParser.parseQueryModel()).thenReturn(null);
		when(javaParser.parseCTOModel()).thenReturn(Collections.emptySet());
		doThrow(new IOException("Permission Denied")).when(zipHolder).addPart(anyString(), any());
		new BnaGenerator(javaParser, zipHolder, "pk", "ver", "des")
				.generate();
	}

}