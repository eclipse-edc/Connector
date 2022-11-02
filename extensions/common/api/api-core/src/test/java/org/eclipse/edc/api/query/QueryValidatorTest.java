/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.api.query;

import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QueryValidatorTest {

    private QueryValidator queryValidator;

    @BeforeEach
    void setUp() {
    }

    @Test
    void validate_isValid() {
        queryValidator = new QueryValidator(TestObject.class);
        var query = of("someString=foobar");

        var result = queryValidator.validate(query);

        assertThat(result.succeeded()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "someString.=foobar",
            ".someString=foobar"
    })
    void validate_keyHasLeadingOrTrailingDot(String filter) {
        queryValidator = new QueryValidator(TestObject.class);
        var query = of(filter);

        var result = queryValidator.validate(query);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureDetail()).startsWith("Invalid path expression");
    }


    @Test
    void validate_interface_withSubtypeMap() {
        queryValidator = new QueryValidator(TestObject.class, Map.of(TestInterface.class, List.of(NestedTestObject.class)));
        var query = of("nestedObject.nestedString=foobar");

        var result = queryValidator.validate(query);
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validate_interface_withoutSubtypeMap() {
        queryValidator = new QueryValidator(TestObject.class);
        var query = of("nestedObject.nestedString=foobar");

        var result = queryValidator.validate(query);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureDetail()).isEqualTo("Field nestedString not found on type " + TestInterface.class);
    }

    @Test
    void validate_isMapType() {
        queryValidator = new QueryValidator(TestObject.class);
        var query = of("someMap.foo=bar");

        var result = queryValidator.validate(query);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureDetail()).isEqualTo("Querying Map types is not yet supported");
    }

    @Test
    void validate_fieldNotExist() {
        queryValidator = new QueryValidator(TestObject.class, Map.of(TestInterface.class, List.of(NestedTestObject.class)));
        var query = of("nestedObject.notexist like (foobar, barbaz)");

        var result = queryValidator.validate(query);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureDetail()).isEqualTo("Field notexist not found on type " + TestInterface.class);
    }

    @Test
    void validate_withListType() {
        queryValidator = new QueryValidator(TestObject.class, Map.of(TestInterface.class, List.of(NestedTestObject.class)));
        var query = of("nestedObject.someList=foobar");

        var result = queryValidator.validate(query);
        assertThat(result.succeeded()).isTrue();
    }

    private QuerySpec of(String filter) {
        return QuerySpec.Builder.newInstance().filter(filter).build();
    }

    private interface TestInterface {
        double getNumber();
    }

    private static class TestObject {
        private String someString;
        private int someInteger;
        private TestInterface nestedObject;
        private Map<String, String> someMap;
    }

    private static class NestedTestObject implements TestInterface {
        private double someDouble;
        private String nestedString;
        private List<String> someList;

        @Override
        public double getNumber() {
            return someDouble;
        }
    }
}
