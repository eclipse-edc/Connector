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

package org.eclipse.edc.connector.dataplane.selector.spi;

import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Collection;
import java.util.List;

/**
 * Wrapper service to encapsulate all functionality required by DPF selector clients (e.g. API controllers), e.g. to
 * get, add and find a particular {@link DataPlaneInstance}
 */
@ExtensionPoint
public interface DataPlaneSelectorService {
    List<DataPlaneInstance> getAll();

    DataPlaneInstance select(DataAddress source, DataAddress destination);

    DataPlaneInstance select(DataAddress source, DataAddress destination, String selectionStrategy);

    Collection<String> getAllStrategies();

    ServiceResult<Void> addInstance(DataPlaneInstance instance);

}
