/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.util;

import org.hyperledger.composer.ComposerException;
import org.hyperledger.composer.annotation.DataField;
import org.hyperledger.composer.annotation.Pointer;
import org.hyperledger.composer.annotation.Query;
import org.hyperledger.composer.bna.model.*;
import org.hyperledger.composer.bna.part.CTOPart;
import org.hyperledger.composer.bna.part.QueryPart;
import org.hyperledger.composer.query.QueryBuilder;
import org.hyperledger.composer.query.SelectQuery;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class JavaParser {
	protected static final Logger logger = LoggerFactory.getLogger(JavaParser.class);

	private Reflections reflections;

	protected JavaParser(Reflections reflections) {
		this.reflections = reflections;
	}

	public JavaParser(List<File> sourceClassFiles) {
		this(sourceClassFiles, null);
	}

	public JavaParser(List<File> sourceClassFiles, ClassLoader parent) {
		this(new Reflections(getClassloaderForReflections(sourceClassFiles, parent),
				new TypeAnnotationsScanner(), new FieldAnnotationsScanner(),
				new SubTypesScanner(), new MethodAnnotationsScanner()));
	}

	static URLClassLoader getClassloaderForReflections(List<File> sourceClassFiles, ClassLoader parentClassLoader) {
		List<URL> sourceClassURLs = new LinkedList<>();
		Set<URL> loadedUrl = new HashSet<>();
		if (parentClassLoader instanceof URLClassLoader) {
			loadedUrl.addAll(Arrays.asList(((URLClassLoader) parentClassLoader).getURLs()));
		}
		for (File file : sourceClassFiles) {
			try {
				URL url = file.toURI().toURL();
				logger.info("find {}", url.toString());
				if (!loadedUrl.contains(url)) {
					sourceClassURLs.add(url);
					loadedUrl.add(url);
				}
			} catch (MalformedURLException e) {
				logger.warn("fail to get uri of {}: {}", file.getAbsolutePath(), e.getMessage());
			}
		}
		return new URLClassLoader(sourceClassURLs.toArray(new URL[sourceClassURLs.size()]), parentClassLoader);
	}

	protected QueryPart parseQueryModel() throws ComposerException {
		QueryPart manager = new QueryPart();
		for (Method m : getMethodsAnnotatedWith(Query.class)) {
			if (m.getParameterCount() != 1) {
				throw new ComposerException(ComposerException.INVALID_INPUT_ERROR, "expect 1 parameters for transaction processor:" + m);
			}

			Class<?> selectQueryClass = m.getParameterTypes()[0];
			if (!SelectQuery.class.isAssignableFrom(selectQueryClass)) {
				throw new ComposerException(ComposerException.INVALID_INPUT_ERROR, "expect the 1nd parameter typeof SelectQuery for " + m);
			}

			if (!QueryBuilder.class.isAssignableFrom(m.getReturnType())) {
				throw new ComposerException(ComposerException.INVALID_INPUT_ERROR, "expect the return value typeof QueryBuilder for " + m);
			}

			FunctionModel model = new FunctionModel(m);
			logger.debug("Found QueryBuilderFunction {}", model);
			final Query query = m.getAnnotation(Query.class);
			final QueryBuilder builder;
			try {
				builder = (QueryBuilder) model.invoke(selectQueryClass.newInstance());
			} catch (Throwable e) {
				throw new ComposerException(ComposerException.INVALID_INPUT_ERROR, "fail to build query " + m + ":" + e.getMessage());
			}
			manager.addEntry(new QueryModel(query.description(), builder.build().getSQL()));
		}
		return manager;
	}

	protected Collection<CTOPart> parseCTOModel() throws ComposerException {
		Map<String, CTOPart> result = new HashMap<>();
		for (Class<? extends Annotation> annotation : Model.MODEL_ANNOTATIONS) {
			for (Class c : getTypesAnnotatedWith(annotation)) {
				parseCTOModel(result, annotation, c);
			}
		}
		return result.values();
	}

	protected void parseCTOModel(Map<String, CTOPart> result, Class<? extends Annotation> annotation, Class c) throws ComposerException {
		String namespace = c.getPackage().getName();
		if ("org.hyperledger.composer.system".equals(namespace)) {
			return;
		}
		Model model = new Model(result.computeIfAbsent(namespace, CTOPart::new))
				.namespace(namespace).type(annotation).name(c.getSimpleName());

		Class superClass = c.getSuperclass();
		if (!Object.class.equals(superClass)) {
			if (!superClass.isAnnotationPresent(annotation)) {
				throw new ComposerException(ComposerException.INVALID_INPUT_ERROR,
						"parent of '" + c.getName() + "' is '" + superClass.getName() + "', not annotated with @" + annotation.getSimpleName());
			} else {
				model.parent(superClass.getPackage().getName(), superClass.getSimpleName());
			}
		}

		for (Field field : c.getDeclaredFields()) {
			FieldModel fieldModel = null;

			DataField dataField = field.getAnnotation(DataField.class);
			if (dataField != null) {
				fieldModel = new FieldModel(field, dataField.optional(), dataField.embedded(),
						dataField.regex(), dataField.range(), dataField.defaultValue(), dataField.genericType());
				model.addField(fieldModel, dataField.primary());
			}
			Pointer pointerField = field.getAnnotation(Pointer.class);
			if (pointerField != null) {
				fieldModel = new FieldModel(field, pointerField.optional(), false, null,
						null, null, pointerField.genericType());
				model.addField(fieldModel, false);
			}
			if (fieldModel == null) {
				continue;
			}

			if (fieldModel.isEnum()) {
				Class<?> enumType = field.getType();
				new EnumModel(result.computeIfAbsent(enumType.getPackage().getName(), CTOPart::new), enumType);
			}
		}
	}

	protected Set<Method> getMethodsAnnotatedWith(Class<? extends Annotation> annotation) {
		return reflections.getMethodsAnnotatedWith(annotation);
	}

	protected Set<Class> getTypesAnnotatedWith(Class<? extends Annotation>... annotations) {
		Set<Class> result = new HashSet<>();
		for (Class<? extends Annotation> annotation : annotations) {
			result.addAll(reflections.getTypesAnnotatedWith(annotation));
		}
		return result;
	}

}
