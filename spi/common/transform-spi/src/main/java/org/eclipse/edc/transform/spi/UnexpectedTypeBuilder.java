/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.transform.spi;

import java.util.ArrayList;
import java.util.List;

/**
 * Reports an attempt to read a property value that is not of the expected type(s).
 */
public class UnexpectedTypeBuilder extends AbstractProblemBuilder<UnexpectedTypeBuilder> {
    private final TransformerContext context;

    private String actual = UNKNOWN;
    private List<String> expected = new ArrayList<>();

    public UnexpectedTypeBuilder(TransformerContext context) {
        this.context = context;
    }

    public UnexpectedTypeBuilder actual(String actual) {
        this.actual = actual;
        return this;
    }

    public UnexpectedTypeBuilder actual(Class<?> actual) {
        this.actual = actual == null ? null : actual.getName();
        return this;
    }

    public UnexpectedTypeBuilder actual(Enum<?> actual) {
        this.actual = actual == null ? null : actual.toString();
        return this;
    }

    public UnexpectedTypeBuilder expected(String expectedType) {
        if (expectedType == null) {
            return this;
        }
        expected.add(expectedType);
        return this;
    }

    public UnexpectedTypeBuilder expected(Enum<?> expectedType) {
        if (expectedType == null) {
            return this;
        }
        expected.add(expectedType.toString());
        return this;
    }

    public UnexpectedTypeBuilder expected(Class<?> expectedType) {
        if (expectedType == null) {
            return this;
        }
        if (expectedType.isEnum()) {
            for (var constant : expectedType.getEnumConstants()) {
                expected.add(constant.toString());
            }
        } else {
            expected.add(expectedType.getName());
        }
        return this;
    }

    @Override
    public void report() {
        var builder = new StringBuilder();
        if (type != null) {
            builder.append(type);
            if (property != null) {
                builder.append(" property '").append(property).append("'");
            }
        } else {
            if (property != null) {
                builder.append("Property '").append(property).append("'");
            }
        }
        if (expected.isEmpty()) {
            if (builder.length() == 0) {
                builder.append("Value ");
            }
            builder.append("was not of the expected type");
        } else {
            if (builder.length() == 0) {
                builder.append("Value");
            }
            builder.append(" must be ").append(concatList(expected));
        }
        if (actual != null) {
            builder.append(" but was: ").append(actual);
        }
        context.reportProblem(builder.toString());
    }

}
