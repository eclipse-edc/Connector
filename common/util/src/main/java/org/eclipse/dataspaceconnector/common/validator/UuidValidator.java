/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.eclipse.dataspaceconnector.common.annotations.Uuid;

import java.util.UUID;

public class UuidValidator implements ConstraintValidator<Uuid, String> {

    @Override
    public void initialize(Uuid contactNumber) {
    }

    @Override
    public boolean isValid(String uuid, ConstraintValidatorContext cxt) {
        if (uuid == null) {
            return false;
        }

        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException exception) {
            return false;
        }

        return true;
    }
}
