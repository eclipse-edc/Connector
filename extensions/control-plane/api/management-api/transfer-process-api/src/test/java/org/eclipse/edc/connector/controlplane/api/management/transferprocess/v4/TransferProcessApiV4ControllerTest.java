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

package org.eclipse.edc.connector.controlplane.api.management.transferprocess.v4;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.BaseTransferProcessApiControllerTest;

import static io.restassured.RestAssured.given;

public class TransferProcessApiV4ControllerTest extends BaseTransferProcessApiControllerTest {
    @Override
    protected Object controller() {
        return new TransferProcessApiV4Controller(monitor, service, transformerRegistry, validatorRegistry);
    }

    @Override
    protected RequestSpecification baseRequest() {
        return given()
                .port(port)
                .baseUri("http://localhost:" + port + "/v4alpha/transferprocesses");
    }
}
