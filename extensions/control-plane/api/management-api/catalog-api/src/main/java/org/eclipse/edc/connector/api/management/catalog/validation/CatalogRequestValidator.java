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
import org.eclipse.edc.api.validation.QuerySpecValidator;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.catalog.spi.CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.catalog.spi.CatalogRequest.CATALOG_REQUEST_PROTOCOL;
import static org.eclipse.edc.catalog.spi.CatalogRequest.CATALOG_REQUEST_PROVIDER_URL;
import static org.eclipse.edc.catalog.spi.CatalogRequest.CATALOG_REQUEST_QUERY_SPEC;

public class CatalogRequestValidator {

    public static Validator<JsonObject> instance() {
        return JsonObjectValidator.newValidator()
                .verify(MandatoryCounterPartyAddressOrProviderUrl::new)
                .verify(CATALOG_REQUEST_PROTOCOL, MandatoryValue::new)
                .verifyObject(CATALOG_REQUEST_QUERY_SPEC, QuerySpecValidator::instance)
                .build();
    }

    private record MandatoryCounterPartyAddressOrProviderUrl(JsonLdPath path) implements Validator<JsonObject> {

        @Override
        public ValidationResult validate(JsonObject input) {
            var counterPartyAddress = new MandatoryValue(path.append(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS));
            var providerUrl = new MandatoryValue(path.append(CATALOG_REQUEST_PROVIDER_URL));

            var validateCounterParty = counterPartyAddress.validate(input);
            var validateProviderUrl = providerUrl.validate(input);

            if (validateCounterParty.succeeded() || validateProviderUrl.succeeded()) {
                return ValidationResult.success();
            } else {
                return validateCounterParty;
            }
        }
    }
}
