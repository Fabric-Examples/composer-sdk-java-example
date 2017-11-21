/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.hyperledger.composer.annotation.*;
import org.hyperledger.composer.model.Entry;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResourceSerializer {
	private static final List<Class<? extends Annotation>> MODEL_ANNOTATIONS = Arrays.asList(Asset.class,
			Participant.class, Transaction.class, Concept.class, Event.class);
	private static final Set<Class> PRIMITIVE_TYPE = new HashSet<>(Arrays.asList(String.class, String[].class,
			long.class, Long.class, long[].class, Long[].class, int.class, Integer.class, int[].class, Integer[].class,
			double.class, Double.class, double[].class, Double[].class, Date.class, Date[].class, boolean.class,
			Boolean.class, boolean[].class, Boolean[].class));
	private static final Pattern RESOURCE_PATTERN = Pattern.compile("resource:([^#]+)#(.+)");

	private static final ThreadLocal<ObjectMapper> mapper = ThreadLocal.withInitial(() -> {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(JsonAutoDetect.Visibility.ANY).withGetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
		return mapper;
	});

	public static JsonNode toJSON(Object resource) {
		return toJSON(resource, true, "<root>");
	}

	public static String toJSONString(Object resource) {
		return toJSON(resource).toString();
	}

	private static JsonNode toJSON(Object resource, boolean forceEmbedded, String parent) {
		if (resource == null) {
			return NullNode.getInstance();
		}
		Class<?> resourceClass = resource.getClass();
		if (PRIMITIVE_TYPE.contains(resourceClass)) {
			return mapperToJSON(resource);
		}

		if (resource instanceof Collection) {
			Collection resources = (Collection) resource;
			ArrayNode arrayNode = mapper.get().createArrayNode();
			for (Object o : resources) {
				arrayNode.add(toJSON(o, forceEmbedded, parent));
			}
			return arrayNode;
		}

		if (resourceClass.isArray()) {
			ArrayNode arrayNode = mapper.get().createArrayNode();
			int length = Array.getLength(resource);
			for (int i = 0; i < length; i++) {
				arrayNode.add(toJSON(Array.get(resource, i), forceEmbedded, parent));
			}
			return arrayNode;
		}

		if (resource instanceof Map) {
			return fromMap((Map) resource, parent);
		}

		for (Class<? extends Annotation> modelAnnotation : MODEL_ANNOTATIONS) {
			if (modelAnnotation.equals(Concept.class)) {
				forceEmbedded = true;
			}
			Class<?> fieldClass = resource.getClass();
			if (fieldClass.isAnnotationPresent(modelAnnotation)) {
				if (forceEmbedded) {
					ObjectNode result = mapper.get().createObjectNode();
					for (Field field : declaredFields(resourceClass)) {
						DataField dataField = field.getAnnotation(DataField.class);
						boolean embedded = false;
						if (dataField != null) {
							embedded = dataField.embedded();
						} else if (!field.isAnnotationPresent(Pointer.class)) {
							continue;
						}
						String key = field.getName();
						field.setAccessible(true);

						String fieldFullName = field.getDeclaringClass().getName() + "." + key;
						Object fieldValue;
						try {
							field.setAccessible(true);
							fieldValue = field.get(resource);
						} catch (IllegalAccessException e) {
							throw new IllegalArgumentException("unable to access field:" + fieldFullName, e);
						}
						result.set(key, toJSON(fieldValue, embedded, fieldFullName));
					}
					return result.put("$class", resource.getClass().getName());

				} else {
					return new TextNode(getResourceId(resource));
				}
			}
		}

		throw new IllegalArgumentException(
				"field is not a primitive or resource type:" + resource.getClass().getName());
	}

	private static JsonNode fromMap(Map resource, String parent) {
		List<Entry> result = new LinkedList<>();
		for (Object o : resource.entrySet()) {
			Map.Entry entry = (Map.Entry) o;
			if (!(entry.getValue() instanceof String)) {
				throw new IllegalArgumentException(String.format("only string-typed value in map is supported, got type '%s' for key %s",
						entry.getValue().getClass().getName(), entry.getKey()));
			}
			Entry e = new Entry();
			e.value(entry.getValue().toString());
			e.key(entry.getKey().toString());
			result.add(e);
		}
		return toJSON(result, true, parent);
	}

	private static Map toMap(JsonNode jsonNode) {
		HashMap<String, String> result = new HashMap<>();
		if (jsonNode.isArray()) {
			for (JsonNode n : jsonNode) {
				String value = n.get("value").asText();
				String key = n.get("key").asText();
				result.put(key, value);
			}
			return result;
		}
		throw new IllegalArgumentException("error parsing type Map<String, String>: expect jsonArray, got " + jsonNode.toString());
	}

	public static boolean isPrimitiveType(Class<?> resourceClass) {
		return PRIMITIVE_TYPE.contains(resourceClass);
	}

	public static String getResourceId(Object resource) {
		Class fieldClass = resource.getClass();

		Field idField = getAccessibleIdField(fieldClass);
		String idFieldFullName = fieldClass.getName() + "." + idField.getName();
		idField.setAccessible(true);
		try {
			Object id = idField.get(resource);
			if (id == null) {
				throw new IllegalArgumentException("null id field:" + idFieldFullName);
			}
			return "resource:" + fieldClass.getName() + '#' + id.toString();
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("unable to access id field:" + idFieldFullName);
		}
	}

	private static Field getAccessibleIdField(Class fieldClass) {
		for (Field field : declaredFields(fieldClass)) {
			DataField annotation = field.getAnnotation(DataField.class);
			if (annotation != null && annotation.primary()) {
				field.setAccessible(true);
				return field;
			}
		}
		throw new IllegalArgumentException("no id field specified:" + fieldClass.getName());
	}

	public static <T> T fromJSON(String resource, Class<T> resourceClass) {
		if (resource == null) {
			throw new IllegalArgumentException("resource is null");
		}
		if (resourceClass == null) {
			throw new IllegalArgumentException("resourceClass is null");
		}
		try {
			return fromJSON(mapper.get().readTree(resource), resourceClass);
		} catch (IOException e) {
			throw new IllegalArgumentException("unable to parse json:" + resource, e);
		}
	}

	private static <T> T fromJSON(JsonNode node, Class<T> resourceClass) throws JsonProcessingException {
		if (node == null || node instanceof NullNode) {
			return null;
		}
		if (PRIMITIVE_TYPE.contains(resourceClass)) {
			return mapper.get().treeToValue(node, resourceClass);
		}

		if (Collection.class.isAssignableFrom(resourceClass)) {
			if (!node.isArray()) {
				throw new IllegalArgumentException(resourceClass + " is Collection but json '" + node + "' is not array-typed");
			}
			Collection result;
			try {
				result = (Collection) resourceClass.newInstance();
			} catch (Exception e) {
				if (Set.class.equals(resourceClass)) {
					result = new HashSet();
				} else if (List.class.equals(resourceClass)) {
					result = new ArrayList();
				} else {
					throw new IllegalArgumentException(
							"resourceClass must be one of specify Set, List, or concrete implementation for Collection, got: " + resourceClass);
				}
			}
			for (JsonNode n : node) {
				if (n.isNumber()) {
					result.add(n.numberValue());
				} else if (n.isTextual()) {
					String text = n.asText();
					Object resource = parseResourceNode(text);
					if (resource == null) {
						result.add(text);
					} else {
						result.add(resource);
					}
				} else if (n.isBoolean()) {
					result.add(n.asBoolean());
				} else if (n.isObject()) {
					JsonNode className = n.get("$class");
					if (className == null) {
						throw new IllegalArgumentException("no $class found in node: " + n.toString());
					}
					try {
						result.add(fromJSON(n, Class.forName(className.asText())));
					} catch (ClassNotFoundException e) {
						throw new IllegalArgumentException("no such class:" + className.toString() + " for " + n.toString());
					}
				}
			}
			return resourceClass.cast(result);
		}

		if (resourceClass.isArray()) {
			if (!node.isArray()) {
				throw new IllegalArgumentException("resourceClass " + resourceClass + " is array-typed but json '" + node.toString() + "' is not");
			}
			int length = node.size();
			Class<?> componentType = resourceClass.getComponentType();
			Object result = Array.newInstance(componentType, length);
			for (int i = 0; i < length; i++) {
				Array.set(result, i, fromJSON(node.get(i), componentType));
			}
			return (T) result;
		}

		if (Map.class.equals(resourceClass)) {
			return (T) toMap(node);
		}

		if (node.isTextual()) {
			Object result = parseResourceNode(node.asText());
			if (result != null) {
				return (T) result;
			}
		}

		try {
			Class cls = Class.forName(node.get("$class").asText());
			if(resourceClass.isAssignableFrom(cls)) {
				resourceClass = cls;
			}
		} catch (Exception ignored) {
		}
		T result;
		try {
			Constructor<T> constructor = resourceClass.getDeclaredConstructor();
			constructor.setAccessible(true);
			result = constructor.newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("unable to create an instance for " + resourceClass.getName(), e);
		}
		for (Field field : declaredFields(resourceClass)) {
			if (!field.isAnnotationPresent(DataField.class) && !field.isAnnotationPresent(Pointer.class)) {
				continue;
			}
			field.setAccessible(true);
			try {
				field.set(result, fromJSON(node.get(field.getName()), field.getType()));
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException("unable to set value for " + field.getDeclaringClass().getName() + "." + field.getName(), e);
			}
		}
		return result;
	}

	static Object parseResourceNode(String resourceString) {
		Matcher matcher = RESOURCE_PATTERN.matcher(resourceString);
		if (matcher.find()) {
			String className = matcher.group(1);
			String id = matcher.group(2);
			try {
				return fromID(id, Class.forName(className));
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("unable to parse '" + resourceString + "': no such class " + className, e);
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> arrayFromJSON(String resourceArray, Class<T> resourceClass) {
		if (resourceArray == null) {
			throw new IllegalArgumentException("resource is null");
		}
		if (resourceClass == null) {
			throw new IllegalArgumentException("resourceClass is null");
		}

		Object arrayObj = fromJSON(resourceArray, Array.newInstance(resourceClass, 0).getClass());
		List<T> result = new LinkedList<>();
		int length = Array.getLength(arrayObj);
		for (int i = 0; i < length; i++) {
			result.add((T) Array.get(arrayObj, i));
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public static <T> T fromID(String id, Class<T> resourceClass) {
		if (id == null || id.trim().length() == 0) {
			throw new IllegalArgumentException("id is null");
		}
		if (resourceClass == null) {
			throw new IllegalArgumentException("resourceClass is null");
		}

		try {
			Object result = resourceClass.newInstance();
			for (Field field : declaredFields(resourceClass)) {
				DataField annotation = field.getAnnotation(DataField.class);
				if (annotation != null && annotation.primary()) {
					field.setAccessible(true);
					field.set(result, id);
					return (T) result;
				}
			}
			throw new IllegalArgumentException("No id field declared in " + resourceClass.getName());
		} catch (InstantiationException e) {
			throw new IllegalArgumentException("unable to create an instance for " + resourceClass.getName(), e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private static List<Field> declaredFields(Class c) {
		List<Field> result = new LinkedList<>();
		for (; c != Object.class; c = c.getSuperclass()) {
			result.addAll(Arrays.asList(c.getDeclaredFields()));
		}
		return result;
	}

	public static JsonNode toJsonNode(String json) throws IOException {
		return mapper.get().readTree(json);
	}

	public static ObjectNode createObjectNode() {
		return mapper.get().createObjectNode();
	}

	public static JsonNode mapperToJSON(Object o) {
		if (o == null) {
			throw new NullPointerException("mapperToJSON gets null");
		}
		return mapper.get().valueToTree(o);
	}
}
