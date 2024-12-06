/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.protocol.dsp.catalog.http.api.decorator;

import jakarta.json.Json;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_PROPERTY_FILTER_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_08;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContinuationTokenManagerImplTest {

    private final Base64continuationTokenSerDes serDes = mock();
    private final ContinuationTokenManagerImpl continuationTokenManager = new ContinuationTokenManagerImpl(serDes, DSP_NAMESPACE_V_08, mock());

    @Test
    void apply_shouldReplaceQueryWithTheOnePassedInTheToken() {
        var filter = Json.createObjectBuilder().add("offset", 1).build();
        when(serDes.deserialize(any())).thenReturn(Result.success(filter));

        var result = continuationTokenManager.applyQueryFromToken(Json.createObjectBuilder().build(), "token");

        assertThat(result).isSucceeded().satisfies(jsonObject -> {
            assertThat(jsonObject.getJsonArray(DSPACE_PROPERTY_FILTER_IRI)).hasSize(1).first().isEqualTo(filter);
        });
    }

    @Test
    void apply_shouldFail_whenSerDesFails() {
        when(serDes.deserialize(any())).thenReturn(Result.failure("error"));

        var result = continuationTokenManager.applyQueryFromToken(Json.createObjectBuilder().build(), "token");

        assertThat(result).isFailed();
    }
}
