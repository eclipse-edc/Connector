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

package org.eclipse.edc.connector.controlplane.api.management.catalog.v3;

import org.eclipse.edc.connector.controlplane.api.management.catalog.BaseCatalogApiControllerTest;

class CatalogApiV3ControllerTest extends BaseCatalogApiControllerTest {

    @Override
    protected String baseUrl() {
        return "/v3/catalog";
    }

    @Override
    protected Object controller() {
        return new CatalogApiV3Controller(service, transformerRegistry, validatorRegistry, participantContextSupplier);
    }
}