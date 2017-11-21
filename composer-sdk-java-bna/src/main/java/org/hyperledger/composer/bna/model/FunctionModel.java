/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.model;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class FunctionModel {
	private Object thisObj;
	private final Method method;

	public FunctionModel(Method method) {
		this.method = method;
		this.method.setAccessible(true);
		if ((method.getModifiers() & Modifier.STATIC) == 0) {
			try {
				Constructor<?> constructor = method.getDeclaringClass().getDeclaredConstructor();
				constructor.setAccessible(true);
				this.thisObj = constructor.newInstance();
			} catch (Exception e) {
				throw new RuntimeException("declaring class for non-static transaction process method:"
						+ method.getName() + " should have a default constructor", e);
			}
		}
	}

	public Object invoke(Object... args) throws Throwable {
		try {
			return method.invoke(thisObj, args);
		} catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
	}

	@Override
	public String toString() {
		return method.toString();
	}
}
