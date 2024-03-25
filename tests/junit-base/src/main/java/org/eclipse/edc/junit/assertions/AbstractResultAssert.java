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
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Failure;

/**
 * AssertJ's assertions for {@link AbstractResult}.
 *
 * @param <SELF> self type
 * @param <RESULT> the result type
 * @param <C> the content type
 * @param <F> the failure type
 */
public class AbstractResultAssert<SELF extends AbstractAssert<SELF, RESULT>, RESULT extends AbstractResult<C, F, RESULT>, C, F extends Failure>
        extends AbstractAssert<SELF, RESULT> {

    protected AbstractResultAssert(RESULT tfAbstractResult) {
        super(tfAbstractResult, AbstractResultAssert.class);
    }

    @SuppressWarnings("unchecked")
    public static <SELF extends AbstractResultAssert<SELF, RESULT, C, F>, RESULT extends AbstractResult<C, F, RESULT>, C, F extends Failure> SELF assertThat(RESULT actual) {
        return (SELF) new AbstractResultAssert<>(actual);
    }

    public ObjectAssert<C> isSucceeded() {
        isNotNull();
        if (!actual.succeeded()) {
            failWithMessage("Expected result to be succeeded, but it was failed: " + actual.getFailureDetail());
        }
        return Assertions.assertThat(actual.getContent());
    }

    public FailureAssert<F, ?> isFailed() {
        isNotNull();
        if (!actual.failed()) {
            failWithMessage("Expected result to be failed, but it was succeeded");
        }
        return new FailureAssert<>(actual.getFailure(), FailureAssert.class);
    }
}
