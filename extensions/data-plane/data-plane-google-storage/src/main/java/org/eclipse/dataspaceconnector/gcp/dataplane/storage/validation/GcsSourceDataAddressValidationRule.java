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

package org.eclipse.dataspaceconnector.gcp.dataplane.storage.validation;

import org.eclipse.dataspaceconnector.dataplane.common.validation.CompositeValidationRule;
import org.eclipse.dataspaceconnector.dataplane.common.validation.EmptyValueValidationRule;
import org.eclipse.dataspaceconnector.dataplane.common.validation.ValidationRule;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

import java.util.List;
import java.util.Map;

import static org.eclipse.dataspaceconnector.gcp.core.storage.GcsStoreSchema.BLOB_NAME;
import static org.eclipse.dataspaceconnector.gcp.core.storage.GcsStoreSchema.BUCKET_NAME;

public class GcsSourceDataAddressValidationRule implements ValidationRule<DataAddress> {

    private final CompositeValidationRule<Map<String, String>> mandatoryPropertyValidationRule  = new CompositeValidationRule<>(
            List.of(
                    new EmptyValueValidationRule(BLOB_NAME),
                    new EmptyValueValidationRule(BUCKET_NAME)
            )
    );

    @Override
    public Result<Void> apply(DataAddress dataAddress) {
        return mandatoryPropertyValidationRule.apply(dataAddress.getProperties());
    }
}
