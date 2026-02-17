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

package org.eclipse.edc.connector.controlplane.api.management.catalog.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.jsonobject.validators.model.QuerySpecValidator;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ID;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_PROTOCOL;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_QUERY_SPEC;

public class CatalogRequestValidator {

    public static Validator<JsonObject> instance(CriterionOperatorRegistry criterionOperatorRegistry) {
        return JsonObjectValidator.newValidator()
                .verify(CATALOG_REQUEST_COUNTER_PARTY_ID, MandatoryValue::new)
                .verify(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, MandatoryValue::new)
                .verify(CATALOG_REQUEST_PROTOCOL, MandatoryValue::new)
                .verifyObject(CATALOG_REQUEST_QUERY_SPEC, path -> QuerySpecValidator.instance(path, criterionOperatorRegistry))
                .build();
    }

}
