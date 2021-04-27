package com.microsoft.dagx.catalog.atlas.metadata;

import com.microsoft.dagx.spi.DagxException;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.model.SearchFilter;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.atlas.type.AtlasTypeUtil.*;
import static org.apache.atlas.type.AtlasTypeUtil.createClassTypeDef;

public class AtlasApiImpl implements AtlasApi {
    private final AtlasClientV2 atlasClient;

    public AtlasApiImpl(AtlasClientV2 atlasClient) {
        this.atlasClient = atlasClient;
    }

    @Override
    public AtlasTypesDef createClassifications(String... classificationName) {
        var defs= Arrays.stream(classificationName).map(name ->
        createTraitTypeDef(name, Collections.emptySet()))
                .collect(Collectors.toList());

        var typedef= new AtlasTypesDef();
        typedef.setClassificationDefs(defs);

        try {
            atlasClient.createAtlasTypeDefs(typedef);
            return typedef;
        } catch (AtlasServiceException e) {
            throw new DagxException(e);
        }

    }

    @Override
    public AtlasTypesDef createTypesDef(String typeKeyName, Set<String> superTypeNames, List<TypeAttribute> attributes) {
        var attrs = attributes.stream().map(attr -> attr.isRequired()
                ? createRequiredAttrDef(attr.getName(), attr.getType())
                : createOptionalAttrDef(attr.getName(), attr.getType())).collect(Collectors.toList());

        AtlasEntityDef atlasEntityDef = createClassTypeDef(typeKeyName, superTypeNames);
        atlasEntityDef.setAttributeDefs(attrs);

        var typesDef= new AtlasTypesDef();
        typesDef.setEntityDefs(Collections.singletonList(atlasEntityDef));

        try {
            return atlasClient.createAtlasTypeDefs(typesDef);
        } catch (AtlasServiceException e) {
            throw new DagxException(e);
        }
    }

    @Override
    public EntityMutationResponse createEntity(AtlasEntity.AtlasEntityWithExtInfo atlasEntityWithExtInfo) {
        try {
            return atlasClient.createEntity(atlasEntityWithExtInfo);
        } catch (AtlasServiceException e) {
            throw new DagxException(e);
        }
    }

    @Override
    public void deleteEntities(List<String> entityGuids) {
        try {
            atlasClient.deleteEntitiesByGuids(entityGuids);
        } catch (AtlasServiceException e) {
            throw new DagxException(e);
        }
    }

    @Override
    public void deleteType(List<AtlasTypesDef> classificationTypes) {
        try {
            for (AtlasTypesDef type : classificationTypes) {
                atlasClient.deleteAtlasTypeDefs(type);
            }
        } catch (AtlasServiceException e) {
            throw new DagxException(e);
        }
    }

    @Override
    public Optional<AtlasEntity> getEntityById(String id) {
        try {
            return Optional.of(atlasClient.getEntityByGuid(id).getEntity());
        } catch (AtlasServiceException e) {
            return Optional.empty();
        }
    }

}
