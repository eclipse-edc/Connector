package com.atlas.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.commons.math3.util.Pair;

public class TypeDefExample {
  private final AtlasClientV2 client;

  private final String TypeJsonFile;
  private final String TypeKeyName = "typeName";
  private final String SuperTypesKeyName = "superTypeNames";

  private final HashSet<String> RequiredAttributes = new HashSet<String>(Arrays.asList(TypeKeyName, SuperTypesKeyName));

  TypeDefExample(AtlasClientV2 client,
                 String typeJsonFile) {
    this.client = client;
    this.TypeJsonFile = typeJsonFile;
  }

  public Pair<List<String>, List<AtlasTypesDef>> createCutomizedTypeDefinitions() throws Exception{
    List<AtlasTypesDef> atlasTypesDefs = new ArrayList<>();
    List<String> typeNames = new ArrayList<>();

    ObjectMapper mapper = new ObjectMapper();

    // convert JSON array to list of entities
    Map<?, ?>[] entities = mapper.readValue(this.getClass().getClassLoader().getResourceAsStream(this.TypeJsonFile), Map[].class);

    for (Map<?,?> entity : entities) {
      Set<?> keys = entity.keySet();

      if (!keys.containsAll(RequiredAttributes)) {
        throw new IllegalArgumentException(
          String.format(
            "%s input file contains a type that is missing one of the required top-level-object properties (%s %s)",
            this.TypeJsonFile,
            TypeKeyName,
            SuperTypesKeyName
          )
        );
      }

      typeNames.add((String)entity.get(TypeKeyName));
      AtlasEntityDef atlasEntityDef = AtlasTypeUtil.createClassTypeDef((String)entity.get(TypeKeyName),
        new HashSet<String>((ArrayList<String>)entity.get(SuperTypesKeyName)));

      List<AtlasStructDef.AtlasAttributeDef> atlasAttributes = new ArrayList<AtlasStructDef.AtlasAttributeDef>();

      for (Map<String, Object> attribute : (ArrayList<Map<String, Object>>)entity.get("attributes")) {
        String attributeType = (String)attribute.get("type");
        String attributeName = (String)attribute.get("name");

        if ((boolean)attribute.getOrDefault("required", false)) {
          atlasAttributes.add(AtlasTypeUtil.createRequiredAttrDef(attributeName, attributeType));
        }
        else {
          atlasAttributes.add(AtlasTypeUtil.createOptionalAttrDef(attributeName, attributeType));
        }
      }

      atlasEntityDef.setAttributeDefs(atlasAttributes);

      AtlasTypesDef typesDef = new AtlasTypesDef();
      typesDef.setEntityDefs(Collections.singletonList(atlasEntityDef));

      try {
        var atlasTypesDef = client.createAtlasTypeDefs(typesDef);

        atlasTypesDefs.add(atlasTypesDef);
      }
      catch (Exception e){
        System.out.println("Error creating types: " + e.getMessage());
      }
    }

    return new Pair<List<String>, List<AtlasTypesDef>>(typeNames, atlasTypesDefs);
  }

  public void deleteTypeDefs(List<AtlasTypesDef> atlasTypesDefs) throws Exception{
    for(int i = 0; i < atlasTypesDefs.size(); ++i){
      client.deleteAtlasTypeDefs(atlasTypesDefs.get(i));
    }
  }
}