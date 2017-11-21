/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataField {
	boolean primary() default false;

	boolean optional() default false;

	boolean embedded() default true;

	String regex() default "";

	String range() default "";      // Double, Long or Integer

	String defaultValue() default "";

	Class<?> genericType() default Object.class;
}
