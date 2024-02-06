/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.framework;

import org.eclipse.edc.connector.dataplane.framework.pipeline.PipelineServiceImpl;
import org.eclipse.edc.connector.dataplane.framework.registry.TransferServiceSelectionStrategy;
import org.eclipse.edc.connector.dataplane.framework.store.InMemoryDataPlaneStore;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.time.Clock;

@Extension(value = DataPlaneDefaultServicesExtension.NAME)
public class DataPlaneDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Framework Default Services";

    @Override
    public String name() {
        return NAME;
    }

    @Inject
    private Clock clock;

    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;

    @Provider(isDefault = true)
    public TransferServiceSelectionStrategy transferServiceSelectionStrategy() {
        return TransferServiceSelectionStrategy.selectFirst();
    }

    @Provider(isDefault = true)
    public DataPlaneStore dataPlaneStore() {
        return new InMemoryDataPlaneStore(clock, criterionOperatorRegistry);
    }

    @Provider(isDefault = true)
    public PipelineService pipelineService(ServiceExtensionContext context) {
        return new PipelineServiceImpl(context.getMonitor());
    }
}
