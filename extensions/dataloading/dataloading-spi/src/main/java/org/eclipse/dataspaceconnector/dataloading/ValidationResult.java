/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.dataloading;

public class ValidationResult {
    public static ValidationResult OK = new ValidationResult(null);
    private final String error;

    private ValidationResult(String error) {
        this.error = error;
    }

    public static ValidationResult error(String error) {
        return new ValidationResult(error);
    }


    public boolean isInvalid() {
        return error != null;
    }

    public String getError() {
        return error;
    }
}
