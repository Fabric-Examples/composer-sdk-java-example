/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.model;

import org.testng.annotations.Test;

public class FunctionModelTest {
	@Test
	public void testInvoke() throws Throwable {
		FunctionModel model = new FunctionModel(TestModel.class.getDeclaredMethod("hello"));
		model.invoke();
		System.out.println(model.toString());
	}

	@Test(expectedExceptions = RuntimeException.class,
			expectedExceptionsMessageRegExp = "declaring class for non-static transaction process method:hello should have a default constructor")
	public void testNonStaticWithoutDefaultConstructor() throws Exception {
		new FunctionModel(AnAsset.class.getDeclaredMethod("hello"));
	}

}