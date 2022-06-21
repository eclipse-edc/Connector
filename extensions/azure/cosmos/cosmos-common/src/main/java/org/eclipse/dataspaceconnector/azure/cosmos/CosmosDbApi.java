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

package org.eclipse.dataspaceconnector.azure.cosmos;

import com.azure.cosmos.models.SqlQuerySpec;
import org.eclipse.dataspaceconnector.spi.system.health.ReadinessProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public interface CosmosDbApi extends ReadinessProvider {

    void saveItem(CosmosDocument<?> item);

    void saveItems(Collection<CosmosDocument<?>> definitions);

    Object deleteItem(String id);

    @Nullable Object queryItemById(String id);

    @Nullable Object queryItemById(String id, String partitionKey);

    List<Object> queryAllItems(String partitionKey);

    List<Object> queryAllItems();

    Stream<Object> queryItems(SqlQuerySpec querySpec);

    Stream<Object> queryItems(String query);

    String invokeStoredProcedure(String procedureName, String partitionKey, Object... args);

    /**
     * Uploads stored procedure into a container.
     *
     * @param name of stored procedure js file
     */
    void uploadStoredProcedure(String name);
}
