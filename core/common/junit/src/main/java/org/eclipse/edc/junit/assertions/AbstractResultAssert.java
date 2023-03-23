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

package org.eclipse.edc.junit.assertions;

import org.assertj.core.api.AbstractAssert;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Failure;

public class AbstractResultAssert<T, F extends Failure> extends AbstractAssert<AbstractResultAssert<T, F>, AbstractResult<T, F>> {
    protected AbstractResultAssert(AbstractResult<T, F> tfAbstractResult) {
        super(tfAbstractResult, AbstractResultAssert.class);
    }

    public static AbstractResultAssert assertThat(AbstractResult<?, ?> actual) {
        return new AbstractResultAssert(actual);
    }

    public AbstractResultAssert isSucceeded() {
        isNotNull();
        if (!actual.succeeded()) {
            failWithMessage("Expected result to be succeeded, but it was failed");
        }
        return this;
    }

    public AbstractResultAssert isFailed() {
        isNotNull();
        if (!actual.failed()) {
            failWithMessage("Expected result to be failed, but it was succeeded");
        }
        return this;
    }

    public AbstractResultAssert hasFailureMessage(String message) {
        isNotNull();
        if (!actual.getFailureMessages().contains(message)) {
            failWithMessage("Expected result to have failure message <%s>, but it was <%s>", message, actual.getFailureMessages());
        }
        return this;
    }

    public AbstractResultAssert hasFailureMessageContaining(String message) {
        isNotNull();
        if (!actual.getFailureMessages().stream().anyMatch(m -> m.contains(message))) {
            failWithMessage("Expected result to have failure message containing <%s>, but it was <%s>", message, actual.getFailureMessages());
        }
        return this;
    }

    public AbstractResultAssert hasFailureMessageMatching(String regex) {
        isNotNull();
        if (!actual.getFailureMessages().stream().anyMatch(m -> m.matches(regex))) {
            failWithMessage("Expected result to have failure message matching <%s>, but it was <%s>", regex, actual.getFailureMessages());
        }
        return this;
    }
}
