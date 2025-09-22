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

package org.eclipse.edc.connector.controlplane.api.management.transferprocess.v3;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.BaseTransferProcessApiControllerTest;

import static io.restassured.RestAssured.given;

public class TransferProcessApiV3ControllerTest extends BaseTransferProcessApiControllerTest {
    @Override
    protected Object controller() {
        return new TransferProcessApiV3Controller(monitor, service, transformerRegistry, validatorRegistry, participantContextSupplier);
    }

    @Override
    protected RequestSpecification baseRequest() {
        return given()
                .port(port)
                .baseUri("http://localhost:" + port + "/v3/transferprocesses");
    }
}
