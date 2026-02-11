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

package org.eclipse.edc.connector.dataplane.selector;

import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.RandomSelectionStrategy;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.SelectionStrategyRegistry;
import org.eclipse.edc.connector.dataplane.selector.store.InMemoryDataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.strategy.DefaultSelectionStrategyRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;

import java.time.Clock;

/**
 * Provides default service implementations for fallback
 * Omitted {@link Extension} since this module contains the extension {@link DataPlaneSelectorExtension}
 */
public class DataPlaneSelectorDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Selector Default Services";

    @Inject
    private Clock clock;

    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public DataPlaneInstanceStore instanceStore() {
        return new InMemoryDataPlaneInstanceStore(clock, criterionOperatorRegistry);
    }

    @Provider(isDefault = true)
    public SelectionStrategyRegistry selectionStrategyRegistry() {
        var strategy = new DefaultSelectionStrategyRegistry();
        strategy.add(new RandomSelectionStrategy());
        return strategy;
    }

}
