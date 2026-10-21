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

package org.eclipse.edc.iam.decentralizedclaims.spi.scope;

import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.protocol.spi.RequestContext;
import org.eclipse.edc.spi.result.Result;

import java.util.Set;

/**
 * Extracts the scopes needed for the DCP flow from an {@link AtomicConstraint}.
 * Extractors can be registered in {@link ScopeExtractorRegistry}
 */
@FunctionalInterface
public interface ScopeExtractor {

    /**
     * Performs the extraction of the scopes.
     *
     * @param leftValue  the left-side expression for the constraint.
     * @param operator   the operator.
     * @param rightValue the right-side expression for the constraint.
     * @param context    the request context
     * @return the extracted scopes if succeeded, otherwise a failure.
     */
    Result<Set<String>> extractScopes(Object leftValue, Operator operator, Object rightValue, RequestContext context);
}
