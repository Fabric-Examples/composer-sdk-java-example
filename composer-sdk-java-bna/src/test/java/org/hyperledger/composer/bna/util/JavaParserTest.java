/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.util;

import org.hyperledger.composer.ComposerException;
import org.hyperledger.composer.annotation.Asset;
import org.hyperledger.composer.annotation.Query;
import org.hyperledger.composer.bna.part.CTOPart;
import org.hyperledger.composer.bna.part.QueryPart;
import org.hyperledger.composer.query.SelectQuery;
import org.hyperledger.composer.system.Network;
import org.reflections.Reflections;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class JavaParserTest {

	@DataProvider
	public static Object[][] methodErrorMessage() throws Exception {
		Method tooManyArgsQuery = QueryTest.class.getDeclaredMethod("tooManyArgsQuery", SelectQuery.class, TestAsset.class);
		Method errorArgTypeQuery = QueryTest.class.getDeclaredMethod("errorArgTypeQuery", TestAsset.class);
		Method errorReturnQuery = QueryTest.class.getDeclaredMethod("errorReturnQuery", SelectQuery.class);
		Method withException = QueryTest.class.getDeclaredMethod("withException", SelectQuery.class);
		return new Object[][]{
				{"expect 1 parameters for transaction processor:" + tooManyArgsQuery, tooManyArgsQuery.getName()},
				{"expect the 1nd parameter typeof SelectQuery for " + errorArgTypeQuery, errorArgTypeQuery.getName()},
				{"expect the return value typeof QueryBuilder for " + errorReturnQuery, errorReturnQuery.getName()},
				{"fail to build query " + withException + ":exception", withException.getName()}
		};
	}

	@Test
	public void testGetClassloaderForReflections() throws Exception {
		URL existingURL = new URL("file:/tmp/hello");
		URL notExistingURL = new URL("file:/tmp/world");
		URLClassLoader parentClassLoader = mock(URLClassLoader.class);
		File existing = mock(File.class);
		File notExisting = mock(File.class);
		File malformedURL = mock(File.class);
		when(parentClassLoader.getURLs()).thenReturn(new URL[]{existingURL});
		when(existing.toURI()).thenReturn(existingURL.toURI());
		when(notExisting.toURI()).thenReturn(notExistingURL.toURI());
		when(malformedURL.toURI()).thenReturn(new URI("composer:/hello/world"));
		List<File> sourceClassFiles = Arrays.asList(existing, notExisting, malformedURL);
		URLClassLoader result = JavaParser.getClassloaderForReflections(sourceClassFiles, parentClassLoader);
		assertEquals(result.getURLs().length, 1);
		assertEquals(result.getURLs()[0], notExistingURL);
		verify(parentClassLoader).getURLs();
		verify(existing).toURI();
		verify(notExisting).toURI();
	}

	@Test
	public void testParseQueryModel() throws Exception {
		Reflections reflections = mock(Reflections.class);
		when(reflections.getMethodsAnnotatedWith(Query.class))
				.thenReturn(Collections.singleton(QueryTest.class.getDeclaredMethod("normalQuery", SelectQuery.class)));
		JavaParser javaParser = new JavaParser(reflections);
		QueryPart queryPart = javaParser.parseQueryModel();
		assertEquals(queryPart.toString(), "query Q0 {\n" +
				"\tdescription: \"description\"\n" +
				"\tstatement: SELECT org.hyperledger.composer.bna.util.TestAsset WHERE (id = '1') ORDER BY [id DESC] LIMIT 10 SKIP 10\n" +
				"}\n\n");
		verify(reflections).getMethodsAnnotatedWith(Query.class);
	}

	@Test(dataProvider = "methodErrorMessage")
	public void errorQuery(String expectErrorMessage, String methodName) {
		Reflections reflections = mock(Reflections.class);

		for (Method m : QueryTest.class.getDeclaredMethods()) {
			if (m.getName().equals(methodName)) {
				when(reflections.getMethodsAnnotatedWith(Query.class))
						.thenReturn(Collections.singleton(m));
				JavaParser javaParser = new JavaParser(reflections);
				try {
					javaParser.parseQueryModel();
				} catch (Exception e) {
					assertTrue(e instanceof ComposerException);
					assertEquals(((ComposerException) e).getErrorCode(), ComposerException.INVALID_INPUT_ERROR);
					assertEquals(e.getMessage(), expectErrorMessage);
				}
				verify(reflections).getMethodsAnnotatedWith(Query.class);
				return;
			}
		}
	}

	@Test
	public void testParseCTOModel() throws ComposerException {
		Reflections reflections = mock(Reflections.class);

		when(reflections.getTypesAnnotatedWith(Asset.class)).thenReturn(
				new HashSet<>(Arrays.asList(TestAsset.class, Network.class)));
		JavaParser javaParser = new JavaParser(reflections);
		Collection<CTOPart> ctoParts = javaParser.parseCTOModel();
		assertNotNull(ctoParts);
		assertEquals(ctoParts.size(), 1);
		assertEquals(ctoParts.iterator().next().toString(), "namespace org.hyperledger.composer.bna.util\n" +
				"\n" +
				"asset TestAsset extends BaseAsset {\n" +
				"\t--> TestParticipant user \n" +
				"\to TestEnum e \n" +
				"}\n\n" +
				"enum TestEnum {\n" +
				"\to A\n" +
				"\to B\n" +
				"\to C\n" +
				"}\n");
	}

	@Test(expectedExceptions = ComposerException.class,
			expectedExceptionsMessageRegExp = "parent of 'org.hyperledger.composer.bna.util.IllegalParent' is 'java.util.ArrayList', not annotated with @Asset")
	public void testAssetWithIllegalParent() throws ComposerException {
		Reflections reflections = mock(Reflections.class);
		when(reflections.getTypesAnnotatedWith(Asset.class)).thenReturn(Collections.singleton(IllegalParent.class));
		JavaParser javaParser = new JavaParser(reflections);
		javaParser.parseCTOModel();
	}

	@Test
	public void testConstructor() throws Exception {
		new JavaParser(Collections.emptyList());
	}
}