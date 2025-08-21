/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.v4;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.BaseContractNegotiationApiControllerTest;
import org.eclipse.edc.junit.annotations.ApiTest;

import static io.restassured.RestAssured.given;

@ApiTest
class ContractNegotiationApiV4ControllerTest extends BaseContractNegotiationApiControllerTest {

    @Override
    protected Object controller() {
        return new ContractNegotiationApiV4Controller(service, transformerRegistry, monitor, validatorRegistry);
    }

    protected RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port + "/v4alpha/contractnegotiations")
                .when();
    }
}
