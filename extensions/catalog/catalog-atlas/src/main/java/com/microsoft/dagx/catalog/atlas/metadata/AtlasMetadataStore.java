/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.catalog.atlas.metadata;

import com.microsoft.dagx.catalog.atlas.dto.AtlasEntity;
import com.microsoft.dagx.common.string.StringUtils;
import com.microsoft.dagx.policy.model.Identifiable;
import com.microsoft.dagx.policy.model.Policy;
import com.microsoft.dagx.schema.DataSchema;
import com.microsoft.dagx.schema.SchemaRegistry;
import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.metadata.MetadataStore;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

@SuppressWarnings("unchecked")
public class AtlasMetadataStore implements MetadataStore {
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
    public @Nullable DataEntry<?> findForId(String id) {

        var properties = atlasApi.getEntityById(id);

        if (properties == null) {

            final List<AtlasEntity.AtlasEntityWithExtInfo> entityWithExtInfos = schemaRegistry.getSchemas().stream()
                    .filter(schema -> schema instanceof DataSchema)
                    .map(schema -> {
                        try {
                            return atlasApi.dslSearchWithParams("from " + schema.getName() + " where name = '" + id + "'", QUERY_RESULT_LIMIT, 0);
                        } catch (AtlasQueryException atlasException) {
                            monitor.severe("AtlasMetaDataStore: error in findForId: " + atlasException.getMessage());
                            return null;
                        }
                    })
                    .filter(atlasSearchResult -> atlasSearchResult != null && atlasSearchResult.getEntities() != null)
                    .flatMap(atlasSearchResult -> atlasSearchResult.getEntities().stream())
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

        if (entry == null) {
            return null;
        }
        Map<String, String> policyProperties = (Map<String, String>) entry.getRelationshipAttribute("itsAccessPolicy");
        if (policyProperties == null) {
            return null;
        }
        final String policyGuid = policyProperties.get("guid");

        //lets make sure the policy actually exists
        final AtlasEntity.AtlasEntityWithExtInfo policyEntity = atlasApi.getEntityById(policyGuid);
        return policyEntity != null ? StringUtils.toString(policyEntity.getEntity().getAttribute("policyId")) : null;
    }

    @Override
    public void save(DataEntry<?> entry) {
        monitor.severe("Save not yet implemented");
    }

    @Override
    public @NotNull Collection<DataEntry<?>> queryAll(Collection<Policy> policies) {
        if (policies.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> policyIds = policies.stream().map(Identifiable::getUid).collect(toSet());
        try {
            var searchResult = atlasApi.dslSearchWithParams("from dagx_policy", 100, 0);

            //now we have all valid relationship entities, need to navigate "towards" its entity
            return searchResult.getEntities().stream()
                    .filter(eh -> eh.getStatus() == AtlasEntity.Status.ACTIVE)
                    .filter(eh -> {
                        var pe = atlasApi.getEntityById(eh.getGuid());
                        final Object policyId = pe.getEntity().getAttribute("policyId");
                        return policyId != null && policyIds.contains(policyId.toString());
                    })
                    .map(ph -> atlasApi.getEntityById(ph.getGuid()))
                    .flatMap(policy -> {
                        final Object itsEntity = policy.getEntity().getRelationshipAttribute("itsEntity");
                        final List<Map<String, String>> targetEntityProperties = (List<Map<String, String>>) itsEntity;
                        return targetEntityProperties.stream().map(te -> findForId(te.get("guid")));
                    })
                    .collect(Collectors.toList());
        } catch (DagxException dagxException) {
            monitor.severe("Error during queryAll(): ", dagxException);
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
