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

package org.eclipse.edc.connector.controlplane.api.management.catalog.v2;

import org.eclipse.edc.connector.controlplane.api.management.catalog.BaseCatalogApiControllerTest;

import static org.mockito.Mockito.mock;

class CatalogApiV2ControllerTest extends BaseCatalogApiControllerTest {

    @Override
    protected Object controller() {
        return new CatalogApiV2Controller(service, transformerRegistry, validatorRegistry, mock());
    }

    @Override
    protected String baseUrl() {
        return "/v2/catalog";
    }
}