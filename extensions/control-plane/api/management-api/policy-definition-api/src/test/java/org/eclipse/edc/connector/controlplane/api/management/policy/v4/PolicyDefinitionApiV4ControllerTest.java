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

package org.eclipse.edc.connector.controlplane.api.management.policy.v4;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.connector.controlplane.api.management.policy.BasePolicyDefinitionApiControllerTest;

import static io.restassured.RestAssured.given;

public class PolicyDefinitionApiV4ControllerTest extends BasePolicyDefinitionApiControllerTest {

    @Override
    protected Object controller() {
        return new PolicyDefinitionApiV4Controller(monitor, transformerRegistry, service, validatorRegistry, participantContextSupplier);
    }

    @Override
    protected RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:%d/v4alpha/policydefinitions".formatted(port))
                .port(port);
    }

}
