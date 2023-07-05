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

package org.eclipse.edc.connector.api.management.asset.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.api.validation.DataAddressDtoValidator;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryObject;
import org.eclipse.edc.validator.jsonobject.validators.OptionalIdNotBlank;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.connector.api.management.asset.model.AssetEntryNewDto.EDC_ASSET_ENTRY_DTO_ASSET;
import static org.eclipse.edc.connector.api.management.asset.model.AssetEntryNewDto.EDC_ASSET_ENTRY_DTO_DATA_ADDRESS;
import static org.eclipse.edc.spi.types.domain.asset.Asset.EDC_ASSET_PRIVATE_PROPERTIES;
import static org.eclipse.edc.spi.types.domain.asset.Asset.EDC_ASSET_PROPERTIES;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Contains the AssetEntryDto validator definition
 *
 * @deprecated this will supersede by {@link AssetValidator}
 */
@Deprecated(since = "0.1.3", forRemoval = true)
public class AssetEntryDtoValidator {

    public static Validator<JsonObject> assetEntryValidator() {
        return JsonObjectValidator.newValidator()
                .verify(EDC_ASSET_ENTRY_DTO_ASSET, MandatoryObject::new)
                .verifyObject(EDC_ASSET_ENTRY_DTO_ASSET, v -> v
                        .verifyId(OptionalIdNotBlank::new)
                        .verify(EDC_ASSET_PROPERTIES, MandatoryObject::new)
                        .verify(path -> new AssetPropertiesUniqueness())
                )
                .verify(EDC_ASSET_ENTRY_DTO_DATA_ADDRESS, MandatoryObject::new)
                .verifyObject(EDC_ASSET_ENTRY_DTO_DATA_ADDRESS, DataAddressDtoValidator::instance)
                .build();
    }

    public static Validator<JsonObject> assetValidator() {
        return JsonObjectValidator.newValidator()
                .verifyId(OptionalIdNotBlank::new)
                .verify(EDC_ASSET_PROPERTIES, MandatoryObject::new)
                .verify(path -> new AssetPropertiesUniqueness())
                .build();
    }

    private static class AssetPropertiesUniqueness implements Validator<JsonObject> {
        @Override
        public ValidationResult validate(JsonObject input) {
            if (!input.containsKey(EDC_ASSET_PROPERTIES) || !input.containsKey(EDC_ASSET_PRIVATE_PROPERTIES)) {
                return ValidationResult.success();
            }
            var properties = input.getJsonArray(EDC_ASSET_PROPERTIES).getJsonObject(0);
            var privateProperties = input.getJsonArray(EDC_ASSET_PRIVATE_PROPERTIES).getJsonObject(0);

            if (properties.keySet().stream().anyMatch(privateProperties::containsKey)) {
                return ValidationResult.failure(violation("cannot exists duplicated keys between 'properties' and 'privateProperties'", EDC_ASSET_ENTRY_DTO_ASSET + "/" + EDC_ASSET_PROPERTIES));
            }
            return ValidationResult.success();
        }
    }

}
