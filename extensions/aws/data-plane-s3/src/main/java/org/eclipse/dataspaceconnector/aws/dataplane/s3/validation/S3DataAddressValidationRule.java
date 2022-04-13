/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.aws.dataplane.s3.validation;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

import java.util.List;

import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.BUCKET_NAME;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.REGION;

public class S3DataAddressValidationRule implements ValidationRule<DataAddress> {

    @Override
    public Result<Void> apply(DataAddress dataAddress) {
        var composite = new CompositeValidationRule<>(
                List.of(
                        new EmptyValueValidationRule(BUCKET_NAME),
                        new EmptyValueValidationRule(REGION)
                )
        );

        return composite.apply(dataAddress.getProperties());
    }
}
