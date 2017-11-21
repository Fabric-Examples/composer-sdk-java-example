/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.model;

import org.hyperledger.composer.bna.part.CTOPart;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.assertEquals;

public class EnumModelTest {

	private EnumModel enumModel;

	@BeforeClass
	public void setUp() {
		CTOPart mock = mock(CTOPart.class);
		enumModel = new EnumModel(mock, TestEnum.class);
	}

	@Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ".*No annotation for enum")
	public void testType() throws Exception {
		enumModel.type(null);
	}

	@Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ".*No parents for enum")
	public void testParent() throws Exception {
		enumModel.parent(null, null);
	}

	@Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ".*Do not add fields for enum")
	public void testAddField() throws Exception {
		enumModel.addField(null, false);
	}

	@Test
	public void testToString() throws Exception {
		assertEquals("enum TestEnum {\n\to a\n\to b\n\to c\n}\n", enumModel.toString());
	}
}