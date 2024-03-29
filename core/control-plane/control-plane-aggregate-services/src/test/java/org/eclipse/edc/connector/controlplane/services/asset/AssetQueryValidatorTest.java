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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

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

        assertThat(validator.validate(query).succeeded()).isTrue();
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

        assertThat(validator.validate(query).failed()).isTrue();
    }
}
