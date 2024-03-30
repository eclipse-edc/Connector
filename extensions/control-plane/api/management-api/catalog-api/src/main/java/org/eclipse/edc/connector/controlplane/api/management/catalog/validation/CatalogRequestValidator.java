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
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.jsonobject.validators.model.QuerySpecValidator;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static java.lang.String.format;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_PROTOCOL;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_PROVIDER_URL;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_QUERY_SPEC;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_TYPE;

public class CatalogRequestValidator {

    public static Validator<JsonObject> instance(Monitor monitor, CriterionOperatorRegistry criterionOperatorRegistry) {
        return JsonObjectValidator.newValidator()
                .verify(path -> new MandatoryCounterPartyAddressOrProviderUrl(path, monitor))
                .verify(CATALOG_REQUEST_PROTOCOL, MandatoryValue::new)
                .verifyObject(CATALOG_REQUEST_QUERY_SPEC, path -> QuerySpecValidator.instance(path, criterionOperatorRegistry))
                .build();
    }

    /**
     * This custom validator can be removed once `providerUrl` is deleted and exists only for legacy reasons
     */
    private record MandatoryCounterPartyAddressOrProviderUrl(JsonLdPath path,
                                                             Monitor monitor) implements Validator<JsonObject> {
        @Override
        public ValidationResult validate(JsonObject input) {
            var counterPartyAddress = new MandatoryValue(path.append(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS));
            var validateCounterParty = counterPartyAddress.validate(input);
            if (validateCounterParty.succeeded()) {
                return ValidationResult.success();
            }
            var providerUrl = new MandatoryValue(path.append(CATALOG_REQUEST_PROVIDER_URL));
            var validateProviderUrl = providerUrl.validate(input);
            if (validateProviderUrl.succeeded()) {
                monitor.warning(format("The attribute %s has been deprecated in type %s, please use %s",
                        CATALOG_REQUEST_PROVIDER_URL, CATALOG_REQUEST_TYPE, CATALOG_REQUEST_COUNTER_PARTY_ADDRESS));
                return ValidationResult.success();
            }
            return validateCounterParty;
        }
    }
}
