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

package org.eclipse.edc.connector.controlplane.api.management.catalog.v5;


import org.eclipse.edc.connector.controlplane.api.management.catalog.BaseCatalogApiControllerTest;
import org.eclipse.edc.junit.annotations.ApiTest;

@ApiTest
class CatalogApiV5ControllerTest extends BaseCatalogApiControllerTest {

    @Override
    protected String baseUrl(String participantContextId) {
        return "/v5alpha/participants/%s/catalog".formatted(participantContextId);
    }

    @Override
    protected Object controller() {
        return new CatalogApiV5Controller(service, transformerRegistry, authorizationService, participantContextService);
    }
}