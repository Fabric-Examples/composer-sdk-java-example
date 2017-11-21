/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hyperledger.composer.annotation.Asset;
import org.hyperledger.composer.annotation.DataField;
import org.hyperledger.composer.annotation.Participant;
import org.hyperledger.composer.annotation.Pointer;
import org.hyperledger.composer.model.Entry;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.*;

public class ResourceSerializerTest {

	@Test
	public void testResourceSerializer() throws Exception {
		HashMap<String, String> mapField = new HashMap<>();
		mapField.put("key1", "value1");
		mapField.put("key2", "value2");
		TestAsset asset = new TestAsset().id("123").mapField(mapField)
				.setField(new HashSet<>(Arrays.asList(new User("11"), new User("22"))))
				.listField(Arrays.asList(new User("33"), new User("44")))
				.arrayField(new User[]{new User("55"), new User("66")})
				.embeddedArrayField(new User[]{new User("77"), new User("88")})
				.notIncluded(new BigDecimal(123.45));
		assertEquals(ResourceSerializer.fromJSON(ResourceSerializer.toJSONString(asset), asset.getClass()), asset);
		assertEquals(ResourceSerializer.toJSONString(null), "null");
	}

	@Test(expectedExceptions = NullPointerException.class,
			expectedExceptionsMessageRegExp = "mapperToJSON gets null")
	public void testJsonNode() throws Exception {
		ObjectNode node = ResourceSerializer.createObjectNode().put("hello", "world");
		assertEquals(ResourceSerializer.toJsonNode("{\"hello\": \"world\"}"), node);
		ResourceSerializer.mapperToJSON(null);
	}

	@DataProvider
	public static Object[][] fromJsonException() {
		return new Object[][]{
				{null, User.class.getName(), "resource is null"},
				{"{}", null, "resourceClass is null"},
				{"{123}", User.class.getName(), "unable to parse json:{123}"},
				{"null", User.class.getName(), ""},
				{"123", Set.class.getName(), "interface java.util.Set is Collection but json '123' is not array-typed"},
				{"[123, 22]", Queue.class.getName(), "resourceClass must be one of specify Set, List, or concrete implementation for Collection, got: interface java.util.Queue"},
				{"[{\"id\":\"1\"}]", List.class.getName(), "no $class found in node: {\"id\":\"1\"}"},
				{"[true, 1, \"ss\", \"resource:org.hyperledger.composer.User#123\", " +
						"{\"key\":\"key1\",\"value\":\"value1\",\"$class\":\"org.hyperledger.composer.model.Entry\"}, " +
						"{\"id\": \"12\", \"$class\": \"not.exist.Class\"}]",
						List.class.getName(), "no such class:\"not.exist.Class\" for {\"id\":\"12\",\"$class\":\"not.exist.Class\"}"},
				{"\"resource:not.exist.Class#123\"", AtomicInteger.class.getName(), "unable to parse 'resource:not.exist.Class#123': no such class not.exist.Class"},
				{"{\"id\": \"123\"}", User[].class.getName(), "resourceClass class [Lorg.hyperledger.composer.User; is array-typed but json '{\"id\":\"123\"}' is not"},
				{"2.2", BigDecimal.class.getName(), "unable to create an instance for java.math.BigDecimal"},
				{"{\"key\":\"key1\",\"value\":\"value1\",\"$class\":\"org.hyperledger.composer.model.Entry\"}", Map.class.getName(), "error parsing type Map<String, String>: expect jsonArray, got {\"key\":\"key1\",\"value\":\"value1\",\"$class\":\"org.hyperledger.composer.model.Entry\"}"},
		};
	}

	@Test(dataProvider = "fromJsonException")
	public void fromJsonException(String node, String className, String errorMessage) throws Exception {
		try {
			ResourceSerializer.fromJSON(node, className == null ? null : Class.forName(className));
		} catch (IllegalArgumentException e) {
			assertNotNull(e);
			assertEquals(e.getMessage(), errorMessage);
		}
	}

	@Test
	public void testIsPrimitiveType() throws Exception {
		assertTrue(ResourceSerializer.isPrimitiveType(Double.class));
		assertTrue(ResourceSerializer.isPrimitiveType(Double[].class));
		assertFalse(ResourceSerializer.isPrimitiveType(NoIdField.class));
	}

	@DataProvider
	public static Object[][] toJsonException() {
		Map<String, User> illegalValueTypeMap = new HashMap<>();
		illegalValueTypeMap.put("12", new User("12"));
		return new Object[][]{
				{illegalValueTypeMap, "only string-typed value in map is supported, got type 'org.hyperledger.composer.User' for key 12"},
				{new BigDecimal(123.33), "field is not a primitive or resource type:java.math.BigDecimal"},
		};
	}

	@Test(dataProvider = "toJsonException")
	public void toJsonException(Object o, String errorMessage) throws Exception {
		try {
			ResourceSerializer.toJSON(o);
		} catch (IllegalArgumentException e) {
			assertNotNull(e);
			assertEquals(e.getMessage(), errorMessage);
		}
	}

	@DataProvider
	public static Object[][] fromIdException() {
		return new Object[][]{
				{null, null, "id is null"},
				{"123", null, "resourceClass is null"},
				{"123", NoIdField.class.getName(), "No id field declared in org.hyperledger.composer.NoIdField"},
				{"123", NoDefaultConstructor.class.getName(), "unable to create an instance for org.hyperledger.composer.NoDefaultConstructor"},
		};
	}

	@Test(dataProvider = "fromIdException")
	public void fromIdException(String id, String className, String errorMessage) throws Exception {
		try {
			ResourceSerializer.fromID(id, className == null ? null : Class.forName(className));
		} catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), errorMessage);
		}
	}

	@Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "null id field:org.hyperledger.composer.User.id")
	public void testGetResourceIdWithNoIdSet() {
		ResourceSerializer.getResourceId(new User());
	}

	@Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "no id field specified:org.hyperledger.composer.NoIdField")
	public void testGetResourceIdWithNoIdField() {
		ResourceSerializer.getResourceId(new NoIdField());
	}

	@Test
	public void testArrayFromJSON() {
		List<Entry> entries = ResourceSerializer.arrayFromJSON("[{\"key\":\"key1\",\"value\":\"value1\",\"$class\":\"org.hyperledger.composer.model.Entry\"}, " +
				"{\"key\":\"key2\",\"value\":\"value2\",\"$class\":\"org.hyperledger.composer.model.Entry\"}]", Entry.class);
		assertNotNull(entries);
		assertEquals(entries.size(), 2);
		assertEquals(entries.get(0), new Entry().key("key1").value("value1"));
		assertEquals(entries.get(1), new Entry().key("key2").value("value2"));
	}

	@Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "resource is null")
	public void testArrayFromJSONWithNullResource() {
		ResourceSerializer.arrayFromJSON(null, Entry.class);
	}

	@Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "resourceClass is null")
	public void testArrayFromJSONWithNullResourceClass() {
		ResourceSerializer.arrayFromJSON("{\"key\":\"key2\",\"value\":\"value2\",\"$class\":\"org.hyperledger.composer.model.Entry\"}", null);
	}
}

@Asset
class TestAsset {
	@DataField(primary = true)
	private String id;

	@DataField
	private Map<String, String> mapField;

	@DataField
	private List<User> listField;

	@DataField
	private Set<User> setField;

	@Pointer
	private User[] arrayField;

	@DataField
	private User[] embeddedArrayField;

	private BigDecimal notIncluded;

	public TestAsset id(String id) {
		this.id = id;
		return this;
	}

	public TestAsset mapField(Map<String, String> mapField) {
		this.mapField = mapField;
		return this;
	}

	public TestAsset listField(List<User> listField) {
		this.listField = listField;
		return this;
	}

	public TestAsset setField(Set<User> setField) {
		this.setField = setField;
		return this;
	}

	public TestAsset arrayField(User[] arrayField) {
		this.arrayField = arrayField;
		return this;
	}

	public TestAsset embeddedArrayField(User[] embeddedArrayField) {
		this.embeddedArrayField = embeddedArrayField;
		return this;
	}

	public TestAsset notIncluded(BigDecimal notIncluded) {
		this.notIncluded = notIncluded;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		TestAsset testAsset = (TestAsset) o;

		if (id != null ? !id.equals(testAsset.id) : testAsset.id != null) return false;
		if (mapField != null ? !mapField.equals(testAsset.mapField) : testAsset.mapField != null) return false;
		if (listField != null ? !listField.equals(testAsset.listField) : testAsset.listField != null) return false;
		if (setField != null ? !setField.equals(testAsset.setField) : testAsset.setField != null) return false;
		// Probably incorrect - comparing Object[] arrays with Arrays.equals
		if (!Arrays.equals(arrayField, testAsset.arrayField)) return false;
		// Probably incorrect - comparing Object[] arrays with Arrays.equals
		return Arrays.equals(embeddedArrayField, testAsset.embeddedArrayField);
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (mapField != null ? mapField.hashCode() : 0);
		result = 31 * result + (listField != null ? listField.hashCode() : 0);
		result = 31 * result + (setField != null ? setField.hashCode() : 0);
		result = 31 * result + Arrays.hashCode(arrayField);
		result = 31 * result + Arrays.hashCode(embeddedArrayField);
		return result;
	}
}

@Participant
class User {
	@DataField(primary = true)
	String id;

	User() {
	}

	public User(String id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		User user = (User) o;

		return id != null ? id.equals(user.id) : user.id == null;
	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}
}

@Asset
class NoIdField {
	@DataField
	String fakeId;
}

@Asset
class NoDefaultConstructor {
	@DataField(primary = true)
	String id;

	NoDefaultConstructor(String id) {
		this.id = id;
	}
}