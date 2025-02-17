/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.store;

import org.eclipse.edc.spi.entity.StateResolver;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StatefulEntityCriteriaToPredicateTest {

    private final CriterionOperatorRegistry operatorRegistry = mock();
    private final StateResolver stateResolver = mock();
    private final StatefulEntityCriteriaToPredicate<?> criteriaToPredicate = new StatefulEntityCriteriaToPredicate<>(operatorRegistry, stateResolver);

    @BeforeEach
    void setUp() {
        when(operatorRegistry.toPredicate(any())).thenReturn(mock());
    }

    @Test
    void shouldNotResolveState_whenItIsInteger() {
        var criterion = criterion("state", "=", 600);

        criteriaToPredicate.convert(List.of(criterion));

        verify(operatorRegistry).toPredicate(criterion);
        verifyNoInteractions(stateResolver);
    }

    @Test
    void shouldResolveState_whenItIsString() {
        when(stateResolver.resolve(any())).thenReturn(600);
        var criterion = criterion("state", "=", "STATE_NAME");

        criteriaToPredicate.convert(List.of(criterion));

        verify(operatorRegistry).toPredicate(criterion("state", "=", 600));
        verify(stateResolver).resolve("STATE_NAME");
    }
}
