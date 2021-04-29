package com.microsoft.dagx.catalog.atlas.metadata;

import com.microsoft.dagx.spi.DagxException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.model.SearchFilter;
import org.apache.atlas.model.instance.AtlasClassification;
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
        var defs = Arrays.stream(classificationName).map(name ->
                createTraitTypeDef(name, Collections.emptySet()))
                .collect(Collectors.toList());

        var typedef = new AtlasTypesDef();
        typedef.setClassificationDefs(defs);

        try {
            atlasClient.createAtlasTypeDefs(typedef);
            return typedef;
        } catch (AtlasServiceException e) {
            throw new DagxException(e);
        }

    }

    @Override
    public void deleteClassification(String... classificationName) {

        var mvm = new MultivaluedMapImpl();
        for (var classN : classificationName) {
            mvm.add(SearchFilter.PARAM_NAME, classN);
        }
        var sf = new SearchFilter(mvm);

        try {
            var typesDef = atlasClient.getAllTypeDefs(sf);
            if (typesDef.getClassificationDefs().isEmpty())
                throw new DagxException("No Classification types exist for the given names: " + String.join(", ", classificationName));

            deleteType(Collections.singletonList(typesDef));
        } catch (AtlasServiceException e) {
            throw new DagxException(e);
        }
    }

    @Override
    public AtlasTypesDef createCustomTypes(String typeName, Set<String> superTypeNames, List<AtlasCustomTypeAttribute> attributes) {
        var attrs = attributes.stream().map(attr -> attr.isRequired()
                ? createRequiredAttrDef(attr.getName(), attr.getType())
                : createOptionalAttrDef(attr.getName(), attr.getType())).collect(Collectors.toList());

        AtlasEntityDef atlasEntityDef = createClassTypeDef(typeName, superTypeNames);
        atlasEntityDef.setAttributeDefs(attrs);

        var typesDef = new AtlasTypesDef();
        typesDef.setEntityDefs(Collections.singletonList(atlasEntityDef));

        try {
            return atlasClient.createAtlasTypeDefs(typesDef);
        } catch (AtlasServiceException e) {
            throw new DagxException(e);
        }
    }

    @Override
    public void deleteCustomType(String typeName) {
        var sf= new SearchFilter();
        sf.setParam(SearchFilter.PARAM_NAME, typeName);

        try {
            var typesDef = atlasClient.getAllTypeDefs(sf);
            if (typesDef.getEntityDefs().isEmpty())
                throw new DagxException("No Custom TypeDef types exist for the given names: " + typeName);

            deleteType(Collections.singletonList(typesDef));
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
    public AtlasEntity getEntityById(String id) {
        try {
            return atlasClient.getEntityByGuid(id).getEntity();
        } catch (AtlasServiceException e) {
            if(e.getStatus() == ClientResponse.Status.NOT_FOUND)
                return null;
            throw new DagxException(e);
        }
    }

    @Override
    public String createEntity(String typeName, Map<String, Object> properties) {
        AtlasEntity atlasEntity = new AtlasEntity(typeName);

        for (String key : properties.keySet()) {
            if (key.equals("classifications")) {
                continue;
            }
            atlasEntity.setAttribute(key, properties.get(key));
        }

        List<String> classificationNames = (List<String>) properties.get("classifications");
        atlasEntity.setClassifications(toAtlasClassifications(classificationNames));
        EntityMutationResponse response = null;
        try {
            response = createEntity(new AtlasEntity.AtlasEntityWithExtInfo(atlasEntity));
        } catch (AtlasServiceException e) {
            throw new DagxException(e);
        }
        var guidMap = response.getGuidAssignments();
        if (guidMap.size() != 1) {
            throw new DagxException("Try to create one entity but received multiple guids back.");
        } else {
            return guidMap.entrySet().iterator().next().getValue();
        }
    }

    private List<AtlasClassification> toAtlasClassifications(List<String> classificationNames) {
        if (classificationNames != null) {
            return classificationNames.stream().map(AtlasClassification::new).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private EntityMutationResponse createEntity(AtlasEntity.AtlasEntityWithExtInfo atlasEntityWithExtInfo) throws AtlasServiceException {
        return atlasClient.createEntity(atlasEntityWithExtInfo);
    }
}
