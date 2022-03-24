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

package org.eclipse.dataspaceconnector.dataplane.selector;

import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstance;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

import java.util.Collection;
import java.util.List;

/**
 * Wrapper service to encapsulate all functionality required by DPF selector clients (e.g. API controllers), e.g. to
 * get, add and find a particular {@link DataPlaneInstance}
 */
public interface DataPlaneSelectorService {
    List<DataPlaneInstance> getAll();

    DataPlaneInstance select(DataAddress source, DataAddress destination);

    DataPlaneInstance select(DataAddress source, DataAddress destination, String selectionStrategy);

    Collection<String> getAllStrategies();

    void addInstance(DataPlaneInstance instance);
}
