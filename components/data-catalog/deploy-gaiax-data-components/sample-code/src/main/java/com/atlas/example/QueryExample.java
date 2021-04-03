package com.atlas.example;

import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.model.instance.AtlasEntityHeader;

import java.util.List;

public class QueryExample {

  private final AtlasClientV2 client;

  private final String typeName;

  QueryExample(AtlasClientV2 client, String typeName) {
    this.client = client;
    this.typeName = typeName;
  }

  /* This is an example of querying by property permission
  public void SearchByProperties() {
    String query = "from " + typeName + " where permission=\"None\"";

    try {

      AtlasSearchResult       result      = client.dslSearchWithParams(query, 10, 0);
      List<AtlasEntityHeader> entities    = result != null ? result.getEntities() : null;
      int                     resultCount = entities == null ? 0 : entities.size();

      System.out.println("DSL Query: " + query);
      System.out.println("  result count: " + resultCount);

      for (int i = 0; i < resultCount; i++) {
        System.out.println("  result # " + (i + 1) + ": " + entities.get(i));
      }
    } catch (Exception e) {
      System.out.println("query -: " + query + " failed");
    }
  }
   */

  // Search classification using dsl
  public void SearchByClassification2(String classifiationName) {
    String query = "from " + typeName + " where " + typeName + " isa " + classifiationName;

      try {
        AtlasSearchResult       result      = client.dslSearchWithParams(query, 100, 0);
        List<AtlasEntityHeader> entities    = result != null ? result.getEntities() : null;
        int                     resultCount = entities == null ? 0 : entities.size();

        System.out.println("DSL Query: " + query);
        System.out.println("  result count: " + resultCount);

        FetchEntitiesFromAtlas(entities);
      } catch (Exception e) {
        System.out.println("query -: " + query + " failed");
      }
  }

  // Search classification using basic search
  public void SearchByClassification1(String classificationName){
    String query = "";
    basicSearch(typeName, classificationName, query);
  }

  private void basicSearch(String typeName, String classification, String query) {
    try {
      AtlasSearchResult       result      = client.basicSearch(typeName, classification, query, false, 100, 0);
      List<AtlasEntityHeader> entities    = result != null ? result.getEntities() : null;
      int                     resultCount = entities == null ? 0 : entities.size();

      System.out.println("Basic search: typeName=" + typeName + ", classification=" + classification + ", query=" + query);
      System.out.println("  result count: " + resultCount);

      FetchEntitiesFromAtlas(entities);
    } catch (Exception e){
      System.out.println("query by classification failed");
    }
  }

  /* This is an example of querying by a classification GDPR and a property permission
  public void SearchByClassificationAndProperty() {
    String query = typeName + " isa GDPR and permission=\"None\"";

    try {

      AtlasSearchResult       result      = client.dslSearchWithParams(query, 10, 0);
      List<AtlasEntityHeader> entities    = result != null ? result.getEntities() : null;
      int                     resultCount = entities == null ? 0 : entities.size();

      System.out.println("DSL Query: " + query);
      System.out.println("  result count: " + resultCount);

      for (int i = 0; i < resultCount; i++) {
        System.out.println("  result # " + (i + 1) + ": " + entities.get(i));
      }
    } catch (Exception e) {
      System.out.println("query -: " + query + " failed");
    }
  }
   */

  public void SearchByTwoClassification(String classifiationName1, String classifiationName2) {
    String query = typeName + " isa " + classifiationName1 + " and " + typeName + " isa " + classifiationName2;

    try {
      AtlasSearchResult       result      = client.dslSearchWithParams(query, 100, 0);
      List<AtlasEntityHeader> entities    = result != null ? result.getEntities() : null;
      int                     resultCount = entities == null ? 0 : entities.size();

      System.out.println("DSL Query: " + query);
      System.out.println("  result count: " + resultCount);

      FetchEntitiesFromAtlas(entities);

      /*
      for (int i = 0; i < resultCount; i++) {
        System.out.println("  result # " + (i + 1) + ": " + entities.get(i));
      }
       */
    } catch (Exception e) {
      System.out.println("query -: " + query + " failed");
    }
  }

  private void FetchEntitiesFromAtlas(List<AtlasEntityHeader> entityHeaders) throws Exception{
    if(entityHeaders == null){
      return;
    }

    for(int i = 0; i < entityHeaders.size(); ++i){
      var response = client.getEntityByGuid(entityHeaders.get(i).getGuid());

      System.out.println("  result # " + (i + 1) + ": " + response.getEntity().toString());
    }
  }
}