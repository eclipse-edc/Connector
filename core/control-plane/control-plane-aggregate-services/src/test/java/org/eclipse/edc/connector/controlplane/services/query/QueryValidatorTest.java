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

package org.eclipse.edc.connector.controlplane.services.query;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedContentResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedDataAddressResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedDataDestinationResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class QueryValidatorTest {

    @Test
    void validate_isValid() {
        var queryValidator = queryValidators.get("TestObject");
        var query = with(criterion("someString", "=", "foobar"));

        var result = queryValidator.validate(query);

        assertThat(result.succeeded()).isTrue();
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidFilters.class)
    void validate_keyHasLeadingOrTrailingDot(Criterion filter) {
        var queryValidator = new QueryValidator(TestObject.class);
        var query = with(filter);

        var result = queryValidator.validate(query);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureDetail()).startsWith("Invalid path expression");
    }

    @Test
    void validate_interface_withSubtypeMap() {
        var queryValidator = queryValidators.get("TestObjectWithSubtypeMap");
        var query = with(criterion("nestedObject.nestedString", "=", "foobar"));

        var result = queryValidator.validate(query);
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validate_interface_withoutSubtypeMap() {
        var queryValidator = queryValidators.get("TestObject");
        var query = with(criterion("nestedObject.nestedString", "=", "foobar"));

        var result = queryValidator.validate(query);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureDetail()).isEqualTo("Field nestedString not found on type " + TestInterface.class);
    }

    @Test
    void validate_isMapTypeTrue() {
        var queryValidator = queryValidators.get("TestObject");
        var query = with(criterion("someMap.foo", "=", "bar"));

        var result = queryValidator.validate(query);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldPermitQueryToJsonLdTags() {
        var queryValidator = queryValidators.get("TestObject");
        var query = with(criterion("someMap.foo.@id", "=", "bar"));

        var result = queryValidator.validate(query);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validate_isMapTypeFalse() {
        var queryValidator = queryValidators.get("TestObject");
        var query = with(criterion("someMap.foo[*].test", "=", "bar"));

        var result = queryValidator.validate(query);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureDetail()).isEqualTo("Querying Map types is not yet supported");
    }

    @Test
    void validate_fieldNotExist() {
        var queryValidator = queryValidators.get("TestObjectWithSubtypeMap");
        var query = with(criterion("nestedObject.notexist", "like", "(foobar, barbaz)"));

        var result = queryValidator.validate(query);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureDetail()).isEqualTo("Field notexist not found on type " + TestInterface.class);
    }

    @Test
    void validate_withListType() {
        var queryValidator = queryValidators.get("TestObjectWithSubtypeMap");
        var query = with(criterion("nestedObject.someList", "=", "foobar"));

        var result = queryValidator.validate(query);
        assertThat(result.succeeded()).isTrue();
    }

    private static class InvalidFilters implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    arguments(criterion("someString.", "=", "foobar")), // trailing dot
                    arguments(criterion(".someString", "=", "foobar")) // leading root
            );
        }
    }

    private QuerySpec with(Criterion criterion) {
        return QuerySpec.Builder.newInstance().filter(criterion).build();
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

    private static Map<String, QueryValidator> queryValidators = Map.of(
            "TestObject", QueryValidator.Builder.newInstance()
                    .withCanonicalType(TestObject.class).build(),
            "TestObjectWithSubtypeMap", QueryValidator.Builder.newInstance()
                    .withCanonicalType(TestObject.class)
                    .withSubtypeMap(Map.of(TestInterface.class, List.of(NestedTestObject.class))).build()
    );

    @Nested
    class QueryValidatorFromTransferProcessServiceImpl {
        @ParameterizedTest
        @ArgumentsSource(InvalidFilters.class)
        void validate_invalidFilter(Criterion filter) {
            var query = with(filter);
            var result = this.queryValidator.validate(query);

            assertThat(result.succeeded()).isFalse();
        }

        private static QueryValidator queryValidator = QueryValidator.Builder.newInstance()
                .withCanonicalType(TransferProcess.class)
                .withSubtypeMap(Map.of(ProvisionedResource.class, List.of(ProvisionedDataAddressResource.class),
                        ProvisionedDataAddressResource.class,
                        List.of(ProvisionedDataDestinationResource.class, ProvisionedContentResource.class)))
                .build();

        private static class InvalidFilters implements ArgumentsProvider {
            @Override
            public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
                return Stream.of(
                        arguments(criterion("provisionedResourceSet.resources.hastoken", "=", "true")), // wrong case
                        arguments(criterion("resourceManifest.definitions.notexist", "=", "foobar")), // property not exist
                        arguments(criterion("contentDataAddress.properties[*].someKey", "=", "someval")) // map types not supported
                );
            }
        }

        private static class ValidFilters implements ArgumentsProvider {
            @Override
            public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
                return Stream.of(
                        arguments(criterion("deprovisionedResources.provisionedResourceId", "=", "someval")),
                        arguments(criterion("type", "=", "CONSUMER")),
                        arguments(criterion("provisionedResourceSet.resources.hasToken", "=", "true"))
                );
            }
        }

    }
}
