/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.negotiation.store.memory;

import org.eclipse.dataspaceconnector.spi.query.BaseCriterionToPredicateConverter;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;

import java.util.function.Predicate;

import static org.eclipse.dataspaceconnector.common.reflection.ReflectionUtil.getFieldValueSilent;

/**
 * Converts a {@link Criterion} into a {@link Predicate} for use with an {@link InMemoryContractNegotiationStore}.
 * Currently, only the "=" and "in" operators are supported (cf. {@link Criterion#getOperator()} and {@link BaseCriterionToPredicateConverter}).
 */
class ContractNegotiationPredicateConverter extends BaseCriterionToPredicateConverter<ContractNegotiation> {

    @Override
    protected <R> R property(String key, Object object) {
        return getFieldValueSilent(key, object);
    }
}
