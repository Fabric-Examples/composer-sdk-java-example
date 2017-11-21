/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.model;

import org.hyperledger.composer.model.Entry;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.testng.Assert.assertEquals;

public class FieldModelTest {


	@DataProvider
	public static Object[][] fieldModel() throws Exception {
		return new Object[][]{
				{TestModel.class.getDeclaredField("assets"), false, false, "", "", "", AnAsset.class, "\t--> AnAsset[] assets ", AnAsset.class, AnAsset.class},
				{TestModel.class.getDeclaredField("assetArray"), false, true, "", "", "", AnAsset.class, "\to AnAsset[] assetArray ", null, AnAsset.class},
				{TestModel.class.getDeclaredField("mapField"), false, true, "", "", "", null, "\to Entry[] mapField ", Entry.class, Entry.class},
				{TestModel.class.getDeclaredField("ints"), false, true, "", "", "", Integer.class, "\to Integer[] ints ", null, null},
				{TestModel.class.getDeclaredField("asset"), false, false, "", "", "", null, "\t--> AnAsset asset ", AnAsset.class, AnAsset.class},
				{TestModel.class.getDeclaredField("asset"), true, true, "a", "b", "c", null, "\to AnAsset asset optional default=c range=b regex=a ", AnAsset.class, AnAsset.class}
		};
	}

	@Test(dataProvider = "fieldModel")
	public void test(Field field, boolean optional, boolean isEmbedded, String regex,
	                 String range, String defaultValue, Class<?> genericType,
	                 String expect, Class<?> nsClass, Class<?> dependencyClass) throws Exception {
		FieldModel model = new FieldModel(field, optional, isEmbedded, regex, range, defaultValue, genericType);
		assertEquals(model.toString(), expect);
		assertEquals(model.name(), field.getName());
		assertEquals(model.namespace(), nsClass == null ? null : nsClass.getPackage().getName());
		assertEquals(model.dependency(), dependencyClass == null ? null : dependencyClass.getName());
		assertEquals(model.isEnum(), field.getType().isEnum());
	}

	@Test(expectedExceptions = IllegalArgumentException.class,
			expectedExceptionsMessageRegExp = "genericType is required for collection type:.*assets")
	public void testCollectionWithoutGenericType() throws Exception {
		Field field = TestModel.class.getDeclaredField("assets");
		FieldModel model = new FieldModel(field, false, false, "", "", "", null);
		System.out.println(model.toString());
	}

	@Test(expectedExceptions = IllegalArgumentException.class,
			expectedExceptionsMessageRegExp = "unable to specify array as generic type for collection type:.*intsList")
	public void testCollectionWithArrayGenericType() throws Exception {
		Field field = TestModel.class.getDeclaredField("intsList");
		FieldModel model = new FieldModel(field, false, false, "", "", "", int[].class);
		System.out.println(model.toString());
	}

	@Test(expectedExceptions = IllegalArgumentException.class,
			expectedExceptionsMessageRegExp = "illegal non-primitive type of DataField:.*decimal")
	public void testIllegalNonPrimitiveType() throws Exception {
		Field field = TestModel.class.getDeclaredField("decimal");
		FieldModel model = new FieldModel(field, false, true, "", "", "", null);
		System.out.println(model.toString());
	}
}