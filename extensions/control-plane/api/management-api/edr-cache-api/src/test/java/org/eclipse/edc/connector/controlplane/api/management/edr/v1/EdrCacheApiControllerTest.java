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

package org.eclipse.edc.connector.controlplane.api.management.edr.v1;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.connector.controlplane.api.management.edr.BaseEdrCacheApiControllerTest;
import org.eclipse.edc.junit.annotations.ApiTest;

import static io.restassured.RestAssured.given;
import static org.mockito.Mockito.mock;

@ApiTest
public class EdrCacheApiControllerTest extends BaseEdrCacheApiControllerTest {


    @Override
    protected Object controller() {
        return new EdrCacheApiV1Controller(edrStore, transformerRegistry, validator, mock());
    }


    protected RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port + "/v1")
                .when();
    }
}
