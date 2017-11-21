/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.model;

import org.hyperledger.composer.annotation.Asset;
import org.hyperledger.composer.annotation.Concept;
import org.hyperledger.composer.annotation.Participant;
import org.hyperledger.composer.annotation.Transaction;
import org.hyperledger.composer.bna.part.CTOPart;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Model {
	public static final List<Class<? extends Annotation>> MODEL_ANNOTATIONS
			= Arrays.asList(Asset.class, Participant.class, Transaction.class, Concept.class);

	String name;
	private String namespace;
	Model.ModelType type;
	private final List<FieldModel> fields;

	private FieldModel primaryKey;
	private String parentName;
	private final CTOPart ctoPart;


	public Model(CTOPart ctoPart) {
		this.ctoPart = ctoPart;
		this.ctoPart.addEntry(this);
		this.fields = new LinkedList<>();
	}

	public Model name(String name) {
		this.name = name;
		return this;
	}

	public Model namespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

	public Model type(Class<? extends Annotation> type) {
		this.type = ModelType.valueOf(type.getSimpleName().toUpperCase());
		return this;
	}

	public Model parent(String parentNamespace, String parentName) {
		this.parentName = parentName;
		this.addDependency(parentNamespace, parentNamespace + '.' + parentName);
		return this;
	}

	public Model addField(FieldModel field, boolean isPrimary) {
		if (type == ModelType.TRANSACTION) {
			String transactionName = namespace + "." + name;
			if (isPrimary) {
				throw new IllegalArgumentException("No primary key for transaction type:" + transactionName);
			}
			if ("transactionId".equals(field.name())) {
				throw new IllegalArgumentException("No field named 'transactionId' for transaction type:" + transactionName);
			}
		}
		if (isPrimary && primaryKey != null) {
			throw new IllegalArgumentException(
					String.format("Duplicate primary key: %s, %s", primaryKey.name(), field.name()));
		}
		this.fields.add(field);
		this.addDependency(field.namespace(), field.dependency());
		if (isPrimary) {
			this.primaryKey = field;
		}
		return this;
	}

	private void addDependency(String namespace, String dependency) {
		if (dependency != null && !this.namespace.equals(namespace)) {
			this.ctoPart.addEntry(dependency);
		}
	}

	static boolean isPointerType(Field field) {
		for (Class<? extends Annotation> annotation : MODEL_ANNOTATIONS) {
			if (checkFieldAnnotation(field, annotation)) return true;
		}
		return false;
	}

	static boolean checkFieldAnnotation(Field field, Class<? extends Annotation> annotation) {
		Class<?> type = field.getType();
		type = type.isArray() ? type.getComponentType() : type;
		return type.isAnnotationPresent(annotation);
	}


	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(type.toString().toLowerCase());
		builder.append(' ');
		builder.append(name);
		switch (type) {
			case TRANSACTION:
			case CONCEPT:
				if (parentName != null) {
					builder.append(" extends ");
					builder.append(parentName);
				}
				break;
			default:
				if (primaryKey == null) {
					if (parentName == null) {
						throw new IllegalArgumentException("primary key of String type must be given for " + this.namespace + "." + this.name);
					}
					builder.append(" extends ");
					builder.append(parentName);
				} else {
					builder.append(" identified by ");
					builder.append(primaryKey.name());
				}
		}
		builder.append(" {\n");
		for (FieldModel field : fields) {
			builder.append(field.toString()).append('\n');
		}
		builder.append("}\n");
		return builder.toString();
	}

	public enum ModelType {
		ASSET, PARTICIPANT, CONCEPT, TRANSACTION, ENUM
	}
}
