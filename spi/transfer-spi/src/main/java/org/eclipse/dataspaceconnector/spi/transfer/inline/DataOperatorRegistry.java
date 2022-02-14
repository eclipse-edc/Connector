/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.transfer.inline;

import org.eclipse.dataspaceconnector.spi.system.Feature;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.Nullable;

@Feature("edc:core:transfer:inline:dataoperatorregistry")
public interface DataOperatorRegistry {
    void registerStreamPublisher(DataStreamPublisher streamer);

    void registerReader(DataReader reader);

    void registerWriter(DataWriter writer);

    @Nullable DataStreamPublisher getStreamPublisher(DataRequest dataRequest);

    @Nullable DataReader getReader(String type);

    @Nullable DataWriter getWriter(String type);
}
