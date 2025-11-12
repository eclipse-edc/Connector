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

package org.eclipse.edc.connector.controlplane.api.management.contractdefinition.v4;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.connector.controlplane.api.management.contractdefinition.BaseContractDefinitionApiControllerTest;

import static io.restassured.RestAssured.given;

class ContractDefinitionApiV4ControllerTest extends BaseContractDefinitionApiControllerTest {
    @Override
    protected RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port + "/v4beta/contractdefinitions")
                .when();

    }

    @Override
    protected Object controller() {
        return new ContractDefinitionApiV4Controller(transformerRegistry, service, monitor, validatorRegistry, participantContextSupplier);
    }
}