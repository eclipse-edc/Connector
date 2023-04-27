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

package org.eclipse.edc.connector.service.asset;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

class AssetQueryValidatorTest {

    private final AssetQueryValidator validator = new AssetQueryValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            Asset.PROPERTY_ID,
            Asset.PROPERTY_NAME,
            Asset.PROPERTY_DESCRIPTION,
            Asset.PROPERTY_VERSION,
            Asset.PROPERTY_CONTENT_TYPE
    })
    void validate_validProperty(String key) {
        var query = QuerySpec.Builder.newInstance().filter(key + "=someval").build();
        assertThat(validator.validate(query).succeeded())
                .isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "asset_prop_id in (foo, bar)", // invalid key
            "customProp=whatever", // no custom properties supported
            EDC_NAMESPACE + "id.=something", // trailing dot
            "." + EDC_NAMESPACE + ":id=something" // leading dot
    })
    void validate_invalidProperty(String filter) {
        var query = QuerySpec.Builder.newInstance()
                .filter(filter)
                .build();

        assertThat(validator.validate(query).failed()).isTrue();
    }
}
