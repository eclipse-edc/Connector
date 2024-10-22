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

package org.eclipse.edc.connector.controlplane.services.asset;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class AssetQueryValidatorTest {

    private final AssetQueryValidator validator = new AssetQueryValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            Asset.PROPERTY_ID,
            Asset.PROPERTY_NAME,
            Asset.PROPERTY_DESCRIPTION,
            Asset.PROPERTY_VERSION,
            Asset.PROPERTY_CONTENT_TYPE,
            "someCustomVal",
            "_anotherValidVal",
            "'http://some.url/property'.nestedvalue"
    })
    void validate_validProperty(String key) {
        var query = QuerySpec.Builder.newInstance().filter(List.of(new Criterion(key, "=", "someval"))).build();

        var result = validator.validate(query);

        assertThat(result).isSucceeded();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "+asset_prop_id", // leading +
            "  customProp", // leading space
            "." + EDC_NAMESPACE + "id", // leading dot
            "/someValue", //leading slash
            "42ValidValues" //leading number
    })
    void validate_invalidProperty(String key) {
        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion(key, "=", "something")))
                .build();

        var result = validator.validate(query);

        assertThat(result).isFailed();
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidFilters.class)
    void search_invalidFilter(Criterion filter) {
        var query = QuerySpec.Builder.newInstance().filter(filter).build();

        var result = validator.validate(query);

        assertThat(result).isFailed();
    }

    private static class InvalidFilters implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    arguments(criterion("  asset_prop_id", "in", "(foo, bar)")), // invalid leading whitespace
                    arguments(criterion(".customProp", "=", "whatever"))  // invalid leading dot
            );
        }
    }
}
