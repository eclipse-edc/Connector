package com.atlas.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;

public class EntityExample {
  private final AtlasClientV2 client;
  private final String EntityJsonFile;

  private final String EntityTypeKeyName = "entityTypeName";
  private final String EntityArrayKeyName = "entities";
  private final String ClassificationKeyName = "classifications";

  private final HashSet<String> RequiredAttributes = new HashSet<String>(Arrays.asList("name", "qualifiedName"));

  EntityExample(
    AtlasClientV2 client, 
    String entityJsonFile) {
    this.client = client;
    this.EntityJsonFile = entityJsonFile;
  }

  public List<String> createCustomizedEntities() throws Exception {
    // create object mapper instance
    ObjectMapper mapper = new ObjectMapper();

    // convert JSON array to list of entities
    Map<?, ?> entities = mapper.readValue(this.getClass().getClassLoader().getResourceAsStream(this.EntityJsonFile), Map.class);

    String entityTypeName = (String)entities.getOrDefault(EntityTypeKeyName, null);
    List<Map<String, Object>> entityList = (List<Map<String, Object>>) entities.getOrDefault(EntityArrayKeyName, null);

    if (entityTypeName == null || entityList == null) {
      throw new IllegalArgumentException(
        String.format(
          "%s input file is missing one of the required top-level-object properties (%s %s)",
          this.EntityJsonFile,
          EntityTypeKeyName,
          EntityArrayKeyName
        ));
    }

    System.out.format("Creating entities of type %s", entityTypeName);

    List<String> entityGuids = new ArrayList<>();

    for (Map<String, Object> entity : entityList) {
      entityGuids.add(createEntityOfType(entityTypeName, entity));
    }

    return entityGuids;
  }

  private String createEntityOfType(String typeName, Map<String, Object> entity) throws Exception{
    AtlasEntity atlasEntity = new AtlasEntity(typeName);

    int requiredAttributesFound = 0;

    for (String key : entity.keySet()) {
      if(key.equals(ClassificationKeyName)){
        continue;
      }

      atlasEntity.setAttribute(key, (String)entity.get(key));

      if (RequiredAttributes.contains(key)) {
        requiredAttributesFound++;
      }
    }

    if (requiredAttributesFound != RequiredAttributes.size()) {
      throw new IllegalArgumentException(
        String.format("Entity is missing one of the required attributes {%s}", RequiredAttributes.toArray()));
    }

    List<String> classificationNames = (List<String>) entity.get(ClassificationKeyName);

    atlasEntity.setClassifications(toAtlasClassifications(classificationNames));

    var response = client.createEntity(new AtlasEntityWithExtInfo(atlasEntity));

    var guidMap = response.getGuidAssignments();

    if(guidMap.size() != 1){
      System.out.println("Contains more than 1 guid.");
      throw new Exception("Try to create one entity but received multiple guid back.");
    }
    else {
      for (Map.Entry<String, String> entry : guidMap.entrySet()) {
        return entry.getValue();
      }
    }

    return null;    
  }

  private List<AtlasClassification> toAtlasClassifications(List<String> classificationNames) {
    List<AtlasClassification> ret = new ArrayList<>();

    if (classificationNames != null) {
      for (String classificationName : classificationNames) {
        ret.add(new AtlasClassification(classificationName));
      }
    }

    return ret;
  }

  public void deleteEntities(List<String> entityGuids) throws Exception{
    client.deleteEntitiesByGuids(entityGuids);
  }
}