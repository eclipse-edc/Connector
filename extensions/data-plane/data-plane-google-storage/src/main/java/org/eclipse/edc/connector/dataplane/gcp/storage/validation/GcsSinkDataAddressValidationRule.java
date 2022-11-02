/*
 *  Copyright (c) 2022 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH
 *
 */

package org.eclipse.edc.connector.dataplane.gcp.storage.validation;

import org.eclipse.edc.connector.dataplane.util.validation.CompositeValidationRule;
import org.eclipse.edc.connector.dataplane.util.validation.EmptyValueValidationRule;
import org.eclipse.edc.connector.dataplane.util.validation.ValidationRule;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.List;
import java.util.Map;

import static org.eclipse.edc.gcp.storage.GcsStoreSchema.BUCKET_NAME;

public class GcsSinkDataAddressValidationRule implements ValidationRule<DataAddress> {
    private final CompositeValidationRule<Map<String, String>> mandatoryPropertyValidationRule = new CompositeValidationRule<>(
            List.of(
                    new EmptyValueValidationRule(BUCKET_NAME)
            )
    );

    @Override
    public Result<Void> apply(DataAddress dataAddress) {
        return mandatoryPropertyValidationRule.apply(dataAddress.getProperties());
    }
}
