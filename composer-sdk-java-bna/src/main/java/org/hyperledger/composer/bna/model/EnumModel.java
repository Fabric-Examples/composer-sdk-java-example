/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.model;

import org.hyperledger.composer.bna.part.CTOPart;

import java.lang.annotation.Annotation;

public class EnumModel extends Model {
    private final Class<?> enumType;

    public EnumModel(CTOPart ctoPart, Class<?> enumType) {
        super(ctoPart);
        this.type = ModelType.ENUM;
        this.enumType = enumType;
        this.name = enumType.getSimpleName();
        this.namespace(enumType.getPackage().getName());
    }

    @Override
    public Model type(Class<? extends Annotation> type) {
        throw new RuntimeException(new IllegalAccessException("No annotation for enum"));
    }

    @Override
    public Model parent(String parentNamespace, String parentName) {
        throw new RuntimeException(new IllegalAccessException("No parents for enum"));
    }

    @Override
    public Model addField(FieldModel field, boolean isPrimary) {
        throw new RuntimeException(new IllegalAccessException("Do not add fields for enum"));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(type.toString().toLowerCase());
        builder.append(' ');
        builder.append(name);

        builder.append(" {\n");
        for (Object constant : enumType.getEnumConstants()) {
            builder.append("\to ").append(constant).append('\n');
        }
        builder.append("}\n");
        return builder.toString();
    }
}
