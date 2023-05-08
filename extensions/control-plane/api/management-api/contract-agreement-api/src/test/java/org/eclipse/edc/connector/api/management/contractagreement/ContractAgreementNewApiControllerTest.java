/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.api.management.contractagreement;

import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

class ContractAgreementNewApiControllerTest extends RestControllerTestBase {

    @BeforeEach
    void setUp() {
        fail("not implemented");
    }

    @Test
    void getAllAgreements() {
        fail("not implemented");
    }

    @Test
    void queryAllAgreements() {
        fail("not implemented");
    }

    @Test
    void getContractAgreement() {
        fail("not implemented");
    }

    @Override
    protected Object controller() {
        return new ContractAgreementNewApiController();
    }
}