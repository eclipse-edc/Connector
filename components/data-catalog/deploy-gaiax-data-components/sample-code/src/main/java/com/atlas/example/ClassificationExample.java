package com.atlas.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;

import java.util.*;
import org.apache.commons.math3.util.Pair;


import static org.apache.atlas.type.AtlasTypeUtil.createTraitTypeDef;

public class ClassificationExample {
  private final AtlasClientV2 client;

  private final String ClassificationJsonFile;

  ClassificationExample(AtlasClientV2 client,
                        String entityJsonFile) {
    this.client = client;
    this.ClassificationJsonFile = entityJsonFile;
  }

  public Pair<AtlasTypesDef, Map<String, List<String>>> createCustomizedClassifications() throws Exception{
    ObjectMapper mapper = new ObjectMapper();

    // convert JSON array to list of entities
    Map<String, List<String>> classifications = mapper.readValue(this.getClass().getClassLoader().getResourceAsStream(this.ClassificationJsonFile), Map.class);

    List<AtlasClassificationDef> classificationDefs = new ArrayList<>();

    for (String classificationType : classifications.keySet()) {
      try {
        List<String> currentClassifications = (List<String>)classifications.get(classificationType);

        for (String currentClassification : currentClassifications) {
          AtlasClassificationDef classificationDef = createTraitTypeDef(currentClassification,  Collections.<String>emptySet());
          classificationDefs.add(classificationDef);
        }
      }
      catch (Exception e){
        System.out.println("Error parsing the data: " + e.getMessage());
      }
    }

    AtlasTypesDef typesDef = new AtlasTypesDef();
    typesDef.setClassificationDefs(classificationDefs);

    try {
      AtlasTypesDef atlasTypesDef = client.createAtlasTypeDefs(typesDef);

      return new Pair<AtlasTypesDef,  Map<String, List<String>>>(atlasTypesDef, classifications);
    }
    catch (Exception e){
      System.out.println("Error create classifications: " + e.getMessage());

      return new Pair<AtlasTypesDef,  Map<String, List<String>>>(null, classifications);
    }
  }
}