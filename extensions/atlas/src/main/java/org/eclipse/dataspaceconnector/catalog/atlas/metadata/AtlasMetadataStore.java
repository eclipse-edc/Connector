/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.atlas.metadata;

import org.eclipse.dataspaceconnector.catalog.atlas.dto.AtlasEntity;
import org.eclipse.dataspaceconnector.catalog.atlas.dto.AtlasSearchResult;
import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.policy.model.Identifiable;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.schema.DataSchema;
import org.eclipse.dataspaceconnector.schema.SchemaRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.metadata.MetadataListener;
import org.eclipse.dataspaceconnector.spi.metadata.MetadataObservable;
import org.eclipse.dataspaceconnector.spi.metadata.MetadataStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.DataEntry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

@SuppressWarnings("unchecked")
public class AtlasMetadataStore extends MetadataObservable implements MetadataStore {
    private static final String ATLAS_PROPERTY_KEYNAME = "keyName";
    private static final String ATLAS_PROPERTY_TYPE = "type";
    private final AtlasApi atlasApi;
    private final Monitor monitor;
    private final SchemaRegistry schemaRegistry;
    private final int QUERY_RESULT_LIMIT = 100;

    public AtlasMetadataStore(AtlasApi atlasApi, Monitor monitor, SchemaRegistry schemaRegistry) {
        this.atlasApi = atlasApi;
        this.monitor = monitor;
        this.schemaRegistry = schemaRegistry;
    }

    @Override
    public @Nullable DataEntry findForId(String id) {
        getListeners().forEach(MetadataListener::searchInitiated);
        var properties = atlasApi.getEntityById(id);

        if (properties == null) {

            final AtlasSearchResult searchResult = atlasApi.dslSearchWithParams("from DataSet where name = '" + id + "'", QUERY_RESULT_LIMIT, 0);

            if (searchResult == null || searchResult.getEntities() == null) {
                monitor.info("AtlasMetaDataStore: no Atlas entities with name " + id + " were found.");
                return null;
            }

            final List<AtlasEntity.AtlasEntityWithExtInfo> entityWithExtInfos = searchResult.getEntities().stream()
                    .filter(entityHeader -> entityHeader.getStatus() == AtlasEntity.Status.ACTIVE)
                    .map(entityHeader -> atlasApi.getEntityById(entityHeader.getGuid()))
                    .collect(Collectors.toList());

            if (entityWithExtInfos.isEmpty()) {
                monitor.info("AtlasMetaDataStore: no Atlas entities with name " + id + " were found.");
                return null;
            }
            if (entityWithExtInfos.size() > 1) { // more than one entity found with the same name
                monitor.info("AtlasMetaDataStore: " + entityWithExtInfos.size() + " entities with ID " + id + " were found. This is a sign of non-unique data! The first element will be used. If that's not what you want, consider prefixing the entry's name.");
            }
            properties = entityWithExtInfos.stream().findFirst().orElse(null);
        }

        final AtlasEntity entity = properties.getEntity();
        if (entity == null) {
            monitor.info("AtlasMetadataStore: no DataEntry found for ID " + id);
            return null;
        }
        var policyId = getPolicyIdForEntity(entity);
        var address = DataAddress.Builder.newInstance()
                .keyName(StringUtils.toString(entity.getAttribute(ATLAS_PROPERTY_KEYNAME)))
                .type(StringUtils.toString(entity.getAttribute(ATLAS_PROPERTY_TYPE)))
                .properties(convert(entity.getAttributes()))
                .build();


        return DataEntry.Builder.newInstance()
                .id(StringUtils.toString(entity.getAttribute("name")))
                .policyId(policyId)
                .catalogEntry(new AtlasDataCatalogEntry(address))
                .build();

    }

    private String getPolicyIdForEntity(AtlasEntity entry) {

        return StringUtils.toString(entry.getAttribute("policyId"));
    }

    @Override
    public void save(DataEntry entry) {
        getListeners().forEach(MetadataListener::metadataItemAdded);
        monitor.severe("Save not yet implemented");
    }

    @Override
    public @NotNull Collection<DataEntry> queryAll(Collection<Policy> policies) {
        getListeners().forEach(MetadataListener::querySubmitted);
        if (policies.isEmpty()) {
            return Collections.emptyList();
        }
        String policyIds = String.join("\", \"", policies.stream().map(Identifiable::getUid).collect(toSet()));

        var allDataSchemas = schemaRegistry.getSchemas().stream().filter(s -> s instanceof DataSchema).collect(Collectors.toList());
        try {
            return allDataSchemas.stream()
                    .map(dataSchema -> {
                        var queryString = "from " + dataSchema.getName() + " where policyId = [\"" + policyIds + "\"]";
                        return atlasApi.dslSearchWithParams(queryString, QUERY_RESULT_LIMIT, 0);
                    })
                    .filter(atlasSearchResult -> atlasSearchResult.getEntities() != null && !atlasSearchResult.getEntities().isEmpty())
                    .flatMap(searchResult -> searchResult.getEntities().stream())
                    .map(entityHeader -> findForId(entityHeader.getGuid()))
                    .collect(Collectors.toList());
        } catch (EdcException EdcException) {
            monitor.severe("Error during queryAll(): ", EdcException);
        }
        return Collections.emptyList();

    }

    private Map<String, String> convert(Map<String, Object> attributes) {
        return attributes.entrySet()
                .stream()
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().toString()));
    }
}
