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

package org.eclipse.edc.connector.api.management.catalog.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.api.validation.QuerySpecDtoValidator;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.catalog.spi.CatalogRequest.EDC_CATALOG_REQUEST_PROTOCOL;
import static org.eclipse.edc.catalog.spi.CatalogRequest.EDC_CATALOG_REQUEST_PROVIDER_URL;
import static org.eclipse.edc.catalog.spi.CatalogRequest.EDC_CATALOG_REQUEST_QUERY_SPEC;

public class CatalogRequestValidator {

    public static Validator<JsonObject> instance() {
        return JsonObjectValidator.newValidator()
                .verify(EDC_CATALOG_REQUEST_PROVIDER_URL, MandatoryValue::new)
                .verify(EDC_CATALOG_REQUEST_PROTOCOL, MandatoryValue::new)
                .verifyObject(EDC_CATALOG_REQUEST_QUERY_SPEC, QuerySpecDtoValidator::instance)
                .build();
    }
}
