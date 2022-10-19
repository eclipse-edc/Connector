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

package org.eclipse.dataspaceconnector.dataplane.common.validation;

import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.List;

public class CompositeValidationRule<T> implements ValidationRule<T> {

    private final List<ValidationRule<T>> rules;

    public CompositeValidationRule(List<ValidationRule<T>> rules) {
        this.rules = rules;
    }

    @Override
    public Result<Void> apply(T object) {
        return rules.stream().reduce(t -> Result.success(), ValidationRule::and).apply(object);
    }
}
