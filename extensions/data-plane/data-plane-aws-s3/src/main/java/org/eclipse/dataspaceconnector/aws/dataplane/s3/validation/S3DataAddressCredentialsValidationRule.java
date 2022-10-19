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

import org.eclipse.dataspaceconnector.dataplane.common.validation.CompositeValidationRule;
import org.eclipse.dataspaceconnector.dataplane.common.validation.EmptyValueValidationRule;
import org.eclipse.dataspaceconnector.dataplane.common.validation.ValidationRule;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

import java.util.List;

import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.ACCESS_KEY_ID;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.SECRET_ACCESS_KEY;

public class S3DataAddressCredentialsValidationRule implements ValidationRule<DataAddress> {

    @Override
    public Result<Void> apply(DataAddress dataAddress) {
        var composite = new CompositeValidationRule<>(
                List.of(
                        new EmptyValueValidationRule(ACCESS_KEY_ID),
                        new EmptyValueValidationRule(SECRET_ACCESS_KEY)
                )
        );

        return composite.apply(dataAddress.getProperties());
    }
}
