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

import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.CriterionConverter;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

import static org.eclipse.dataspaceconnector.negotiation.store.memory.ContractNegotiationFunctions.property;

/**
 * Converts a {@link Criterion} into a {@link Predicate<ContractNegotiation>} for use with an {@link InMemoryContractNegotiationStore}.
 * Currently, only the "=" and "in" operators are supported (cf. {@link Criterion#getOperator()}.
 */
class CriterionToPredicateConverter implements CriterionConverter<Predicate<ContractNegotiation>> {
    @Override
    public Predicate<ContractNegotiation> convert(Criterion criterion) {
        if ("=".equals(criterion.getOperator())) {
            return negotiation -> {
                Object property = property(negotiation, (String) criterion.getOperandLeft());
                if (property == null) {
                    return false; //property does not exist on negotiation
                }
                return Objects.equals(property, criterion.getOperandRight());
            };
        } else if ("in".equalsIgnoreCase(criterion.getOperator())) {
            return asset -> {
                String property = property(asset, (String) criterion.getOperandLeft());
                var list = (String) criterion.getOperandRight();
                // some cleanup needs to happen
                list = list.replace("(", "").replace(")", "").replace(" ", "");
                var items = list.split(",");
                return Arrays.asList(items).contains(property);
            };
        }
        throw new IllegalArgumentException(String.format("Operator [%s] is not supported by this converter!", criterion.getOperator()));
    }


}
