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

package org.eclipse.edc.connector.controlplane.api.management.contractagreement.v31alpha;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.connector.controlplane.api.management.contractagreement.BaseContractAgreementApiControllerTest;

import static io.restassured.RestAssured.given;

class ContractAgreementApiV31AlphaControllerTest extends BaseContractAgreementApiControllerTest {
    @Override
    protected Object controller() {
        return new ContractAgreementApiV31AlphaController(service, transformerRegistry, monitor, validatorRegistry);
    }

    protected RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port + "/v3.1alpha/contractagreements")
                .when();
    }
}