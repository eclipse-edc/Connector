/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.v5;

import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.ContractNegotiationApiControllerTestBase;
import org.eclipse.edc.junit.annotations.ApiTest;

@ApiTest
class ContractNegotiationApiV5ControllerTest extends ContractNegotiationApiControllerTestBase {

    @Override
    protected Object controller() {
        return new ContractNegotiationApiV5Controller(service, participantContextService, authorizationService, transformerRegistry, monitor);
    }

    @Override
    protected String versionPath() {
        return "v5beta";
    }
}
