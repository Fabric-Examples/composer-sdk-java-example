/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.model;

import org.hyperledger.composer.annotation.Concept;
import org.hyperledger.composer.model.Entry;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FieldModel {
	private final String name;
	private String namespace;
	private String typeName;
	private String dependency;
	private boolean isEnum, isConcept, isEmbedded;
	private String optional = "";
	private String defaultValue = "";
	private String range = "";
	private String regex = "";

	public FieldModel(Field field, boolean optional, boolean isEmbedded, String regex,
	                  String range, String defaultValue, Class<?> genericType) {
		if (optional) {
			this.optional = "optional ";
		}
		if (defaultValue != null && !defaultValue.isEmpty()) {
			this.defaultValue = "default=" + defaultValue + ' ';
		}
		if (range != null && !range.isEmpty()) {
			this.range = "range=" + range + ' ';
		}
		if (regex != null && !regex.isEmpty()) {
			this.regex = "regex=" + regex + ' ';
		}
		this.name = field.getName();
		Class<?> type = field.getType();
		this.isEnum = type.isEnum();
		this.isEmbedded = isEmbedded;
		this.typeName = PRIMITIVE_TYPE_MAP.get(type);
		if (this.typeName == null) {
			if (Collection.class.isAssignableFrom(type)) {
				if (genericType == null) {
					throw new IllegalArgumentException("genericType is required for collection type:" + field);
				}
				type = genericType;
				if (type.isArray()) {
					throw new IllegalArgumentException("unable to specify array as generic type for collection type:" + field);
				}
				this.typeName = type.getSimpleName() + "[]";
				if (PRIMITIVE_TYPE_MAP.get(type) != null) {
					return;
				}
				this.namespace = genericType.getPackage().getName();
			} else if (Map.class.isAssignableFrom(type)) {
				this.typeName = "Entry[]";
				this.namespace = Entry.class.getPackage().getName();
			} else if (!this.isEnum && !Model.isPointerType(field)) {
				throw new IllegalArgumentException("illegal non-primitive type of DataField:" + field);
			} else {
				this.typeName = type.getSimpleName();
				this.namespace = type.getPackage() == null ? null : type.getPackage().getName();
			}

			this.isConcept = Model.checkFieldAnnotation(field, Concept.class);

            /* add dependency */
			type = type.isArray() ? type.getComponentType() : type;
			if (Map.class.isAssignableFrom(type)) {
				this.dependency = Entry.class.getName();
			} else {
				this.dependency = type.getName();
			}
		}

	}

	String namespace() {
		return this.namespace;
	}

	String dependency() {
		return dependency;
	}

	String name() {
		return this.name;
	}

	public boolean isEnum() {
		return isEnum;
	}

	@Override
	public String toString() {
		return "\t" + ((isEnum || isConcept || dependency == null || isEmbedded) ? "o " : "--> ") +
				typeName + ' ' +
				name + ' ' +
				optional +
				defaultValue +
				range +
				regex;
	}


	private static final Map<Type, String> PRIMITIVE_TYPE_MAP = new HashMap<Type, String>() {
		{
			//String : a UTF8 encoded String
			put(String.class, "String");
			put(String[].class, "String[]");
			//Long : a 64 bit signed whole number
			put(long.class, "Long");
			put(Long.class, "Long");
			put(long[].class, "Long[]");
			put(Long[].class, "Long[]");
			//Integer : a 32 bit signed whole number
			put(int.class, "Integer");
			put(Integer.class, "Integer");
			put(int[].class, "Integer[]");
			put(Integer[].class, "Integer[]");
			//Double : a double precision 64 bit numeric value
			put(double.class, "Double");
			put(Double.class, "Double");
			put(double[].class, "Double[]");
			put(Double[].class, "Double[]");
			//DateTime : an ISO-8601 compatible time instance, with optional time zone and UTZ offset
			put(Date.class, "DateTime");
			put(Date[].class, "DateTime[]");
			//Boolean : a Boolean value, either true or false
			put(boolean.class, "Boolean");
			put(Boolean.class, "Boolean");
			put(boolean[].class, "Boolean[]");
			put(Boolean[].class, "Boolean[]");
		}
	};
}
