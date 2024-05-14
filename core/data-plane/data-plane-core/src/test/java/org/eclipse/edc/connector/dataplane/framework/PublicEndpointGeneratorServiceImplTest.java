/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.framework;

import org.eclipse.edc.connector.dataplane.spi.Endpoint;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;


class PublicEndpointGeneratorServiceImplTest {

    private final PublicEndpointGeneratorServiceImpl generatorService = new PublicEndpointGeneratorServiceImpl();

    @Test
    void generateFor() {
        var endpoint = new Endpoint("fizz", "bar-type");
        generatorService.addGeneratorFunction("testtype", dataAddress -> endpoint);

        assertThat(generatorService.generateFor(DataAddress.Builder.newInstance().type("testtype").build())).isSucceeded()
                .isEqualTo(endpoint);
    }

    @Test
    void generateFor_noFunction() {
        assertThat(generatorService.generateFor(DataAddress.Builder.newInstance().type("testtype").build()))
                .isFailed()
                .detail()
                .isEqualTo("No Endpoint generator function registered for source data type 'testtype'");
    }

    @Test
    void supportedTypes() {
        generatorService.addGeneratorFunction("type", dataAddress -> new Endpoint("any", "any"));

        var result = generatorService.supportedDestinationTypes();

        assertThat(result).containsOnly("type");
    }
}
