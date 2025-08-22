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

package org.eclipse.edc.connector.controlplane.api.management.catalog.v4;

import org.eclipse.edc.connector.controlplane.api.management.catalog.BaseCatalogApiControllerTest;

class CatalogApiV4ControllerTest extends BaseCatalogApiControllerTest {

    @Override
    protected String baseUrl() {
        return "/v4alpha/catalog";
    }

    @Override
    protected Object controller() {
        return new CatalogApiV4Controller(service, transformerRegistry, validatorRegistry);
    }
}