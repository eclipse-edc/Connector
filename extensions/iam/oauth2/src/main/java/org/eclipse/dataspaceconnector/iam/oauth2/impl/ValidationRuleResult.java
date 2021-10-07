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

package org.eclipse.dataspaceconnector.iam.oauth2.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents the result of the validation of one single {@link ValidationRule}
 * <p>
 * Multiple {@link ValidationRuleResult} can be collated into one using the {@link ValidationRuleResult#merge(ValidationRuleResult)} method
 */
public class ValidationRuleResult {

    private final List<String> errorMessages = new ArrayList<>();

    public boolean isSuccess() {
        return errorMessages.isEmpty();
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void reportsError(String message) {
        errorMessages.add(message);
    }

    public void reportsErrors(Collection<String> messages) {
        errorMessages.addAll(messages);
    }

    public ValidationRuleResult merge(ValidationRuleResult other) {
        reportsErrors(other.errorMessages);
        return this;
    }
}
