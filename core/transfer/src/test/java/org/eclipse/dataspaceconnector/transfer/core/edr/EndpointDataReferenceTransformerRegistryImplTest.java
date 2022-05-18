/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.core.edr;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformer;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.transfer.core.edr.EndpointDataReferenceFixtures.createEndpointDataReference;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EndpointDataReferenceTransformerRegistryImplTest {

    private static final Faker FAKER = new Faker();

    private EndpointDataReferenceTransformer transformer1;
    private EndpointDataReferenceTransformer transformer2;
    private EndpointDataReferenceTransformerRegistry registry;

    @BeforeEach
    public void setUp() {
        transformer1 = mock(EndpointDataReferenceTransformer.class);
        transformer2 = mock(EndpointDataReferenceTransformer.class);
        registry = new EndpointDataReferenceTransformerRegistryImpl();
        registry.registerTransformer(transformer1);
        registry.registerTransformer(transformer2);
    }

    @Test
    void transform_onlyOneCanHandle_shouldReturnTransformedEdr() {
        var inputEdr = createEndpointDataReference();
        var outputEdr = createEndpointDataReference();
        when(transformer1.canHandle(inputEdr)).thenReturn(false);
        when(transformer2.canHandle(inputEdr)).thenReturn(true);
        when(transformer2.transform(inputEdr)).thenReturn(Result.success(outputEdr));

        var result = registry.transform(inputEdr);

        verify(transformer1, never()).transform(any());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo(outputEdr);
    }

    @Test
    void transform_bothCanHandle_shouldReturnAny() {
        var inputEdr = createEndpointDataReference();
        var outputEdr = createEndpointDataReference();
        when(transformer1.canHandle(inputEdr)).thenReturn(true);
        when(transformer2.canHandle(inputEdr)).thenReturn(true);
        when(transformer1.transform(inputEdr)).thenReturn(Result.success(outputEdr));
        when(transformer2.transform(inputEdr)).thenReturn(Result.success(outputEdr));

        var result = registry.transform(inputEdr);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo(outputEdr);
    }

    @Test
    void transform_noneCanHandle_shouldReturnInputEdr() {
        var inputEdr = createEndpointDataReference();
        when(transformer1.canHandle(inputEdr)).thenReturn(false);
        when(transformer2.canHandle(inputEdr)).thenReturn(false);

        var result = registry.transform(inputEdr);

        verify(transformer1, never()).transform(any());
        verify(transformer2, never()).transform(any());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo(inputEdr);
    }

    @Test
    void transform_transformerFailed_shouldReturnFailedResult() {
        var errorMsg = FAKER.lorem().sentence();
        var inputEdr = createEndpointDataReference();

        when(transformer1.canHandle(inputEdr)).thenReturn(true);
        when(transformer2.canHandle(inputEdr)).thenReturn(false);
        when(transformer1.transform(inputEdr)).thenReturn(Result.failure(errorMsg));

        var result = registry.transform(inputEdr);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly(errorMsg);
    }
}