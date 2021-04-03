package com.atlas.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.commons.math3.util.Pair;

public class AtlasExample {
    private final AtlasClientV2 client;

    private final TypeDefExample typeDefExample;
    private final ClassificationExample classificationExample;
    private final EntityExample entityExample;

    /* This is code for glossary and terms which are available in 3.0.0, leave the code here for future references
    private final String glossaryPoliciesName = "Policies";
    private final String[] termsPoliciesNames = new String[] {"Policy A", "Policy B", "Policy C"};
    private AtlasGlossary glossaryPolicies = null;
    private AtlasGlossaryTerm[] termsPolicies = new AtlasGlossaryTerm[termsPoliciesNames.length];

    private final String glossaryPermissionsName = "Permissions";
    private final String[] termsPermissionNames = new String[] {"Permission A", "Permission B", "Permission C"};
    private AtlasGlossary glossaryPermissions = null;
    private AtlasGlossaryTerm[] termsPermission = new AtlasGlossaryTerm[termsPermissionNames.length];

    private final String glossaryDutiesName = "Duties";
    private final String[] termsDutyNames = new String[] {"Duty A", "Duty B", "Duty C"};
    private AtlasGlossary glossaryDuties = null;
    private AtlasGlossaryTerm[] termsDuties = new AtlasGlossaryTerm[termsDutyNames.length];
    */

    private boolean cleanDataAfter;

    // you need to update IP to your atlas cluster ip
    public AtlasExample() {
        client = new AtlasClientV2(
          new String[]{PropertyCache.getInstance().getProperty("atlas.rest.address")},
          new String[]{PropertyCache.getInstance().getProperty("atlas.account.username"),
              PropertyCache.getInstance().getProperty("atlas.account.password")});

        cleanDataAfter = Boolean.parseBoolean(PropertyCache.getInstance().getProperty("atlas.cleandata"));

        // Create type sample instance
        String typeFile = PropertyCache.getInstance().getProperty("atlas.quickstart.types.jsonfile");

        typeDefExample = new TypeDefExample(client, typeFile);

        // Create classification sample instance
        String classificationFile = PropertyCache.getInstance().getProperty("atlas.quickstart.classifications.jsonfile");

        classificationExample = new ClassificationExample(client, classificationFile);

        // Create entity sample instance
        String entityFile = PropertyCache.getInstance().getProperty("atlas.quickstart.entities.jsonfile");

        entityExample = new EntityExample(client, entityFile);
    }
    
    public static void main(String[] args) throws Exception{
        AtlasExample atlasDemo = new AtlasExample();
        atlasDemo.run();
    }
    
    private void run() throws Exception{
        var atlasTypeDefsResult = createAtlasTypeDefs();

        List<String> typeNames = atlasTypeDefsResult.getFirst();
        List<AtlasTypesDef> atlasTypeDefs = atlasTypeDefsResult.getSecond();

        var classificationResult = createClassifications();
        Map<String, List<String>> classifications = classificationResult.getSecond();
        AtlasTypesDef classificationTypes = classificationResult.getFirst();

        var entityGuidList = createEntities();

        searchOperation(typeNames, classifications);

        if(cleanDataAfter){
            deleteEntities(entityGuidList);

            if(classificationTypes != null){
                deleteTypes(Collections.singletonList(classificationTypes));
            }

            deleteTypes(atlasTypeDefs);
        }
    }

    private Pair<List<String>, List<AtlasTypesDef>> createAtlasTypeDefs() throws Exception{
        Pair<List<String>, List<AtlasTypesDef>> atlasTypesDefs = typeDefExample.createCutomizedTypeDefinitions();

        return atlasTypesDefs;
    }

    private void deleteTypes(List<AtlasTypesDef> atlasTypesDefs){
        try {
            typeDefExample.deleteTypeDefs(atlasTypesDefs);
        }
        catch (Exception e){
            System.out.println("Delete types failed. " + e.getMessage());
        }
    }

    private Pair<AtlasTypesDef, Map<String, List<String>>> createClassifications(){
        try{
            return classificationExample.createCustomizedClassifications();
        }
        catch (Exception e){
            System.out.println(e.getMessage());
            return null;
        }
    }

    /*
    // Create terms and glossaries
    private void termOperations(){
        GlossaryExample glossaryExample = new GlossaryExample(client);

        try{
            glossaryPolicies = glossaryExample.createGlossary(glossaryPoliciesName);

            for(int i = 0; i < termsPoliciesNames.length; ++i){
                termsPolicies[i] = glossaryExample.createGlossaryTerm(glossaryPolicies, termsPoliciesNames[i]);
            }
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }

        try{
            glossaryPermissions = glossaryExample.createGlossary(glossaryPermissionsName);

            for(int i = 0; i < termsPermissionNames.length; ++i){
                termsPermission[i] = glossaryExample.createGlossaryTerm(glossaryPermissions, termsPermissionNames[i]);
            }
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }

        try{
            glossaryDuties = glossaryExample.createGlossary(glossaryDutiesName);

            for(int i = 0; i < termsDutyNames.length; ++i){
                termsDuties[i] = glossaryExample.createGlossaryTerm(glossaryDuties, termsDutyNames[i]);
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
*/

    private List<String> createEntities(){
        try{
            var guidList = entityExample.createCustomizedEntities();

            return guidList;
        }
        catch (Exception e){
            System.out.println(e.getMessage());
            return new ArrayList<>();
        }
    }

    private void deleteEntities(List<String> entityGuids){
        try{
            entityExample.deleteEntities(entityGuids);
        }
        catch (Exception e){
            System.out.println("Delete entities failed: " + e.getMessage());
        }
    }

    // Assign terms to entities
    /*
    // This is only supported by v3.0.0 and above of the client, which appears to have been removed from Maven
    private void assignTermsToEntities(List<String> entityGuidList) throws Exception{
        Random rd = new Random();

        GlossaryExample glossaryExample = new GlossaryExample(client);

        for(int i = 0; i < entityGuidList.size(); ++i){
            var entity = client.getEntityByGuid(entityGuidList.get(i));

            int next = rd.nextInt(termsPolicies.length + 1);

            if(next < termsPolicies.length){
                glossaryExample.assignTermToEntity(termsPolicies[next], entity.getEntity());
            }

            next = rd.nextInt(termsPermission.length + 1);

            if(next < termsPermission.length){
                glossaryExample.assignTermToEntity(termsPermission[next], entity.getEntity());
            }

            next = rd.nextInt(termsDuties.length + 1);

            if(next < termsDuties.length){
                glossaryExample.assignTermToEntity(termsDuties[next], entity.getEntity());
            }
        }
    }

     */

    /*
     * In this example, I just do two sets of queries,
     * the first set is querying based on one classification
     * the second set is querying based on two classifications
     */

    private void searchOperation(List<String> typeNames, Map<String, List<String>> classifications){
        for(int k = 0; k < typeNames.size(); ++k){
            String typeName = typeNames.get(k);

            QueryExample queryExample = new QueryExample(client, typeName);

            if(classifications.size() > 0){
                for (Map.Entry<String, List<String>> entry : classifications.entrySet()){
                    List<String> classificationNames = entry.getValue();

                    for(int i = 0; i < classificationNames.size(); ++i){
                        queryExample.SearchByClassification1(classificationNames.get(i));
                    }
                }
            }

            if(classifications.size() > 1){
                boolean getFirst = false;

                List<String> classificationNames1 = new ArrayList<>();
                List<String> classificationNames2 = new ArrayList<>();

                for (Map.Entry<String, List<String>> entry : classifications.entrySet()){
                    if(!getFirst){
                        classificationNames1 = entry.getValue();
                        getFirst = true;
                    }
                    else{
                        classificationNames2 = entry.getValue();
                        break;
                    }
                }

                for(int i = 0; i < classificationNames1.size(); ++i){
                    for(int j = 0; j < classificationNames2.size(); ++j){
                        queryExample.SearchByTwoClassification(
                            classificationNames1.get(i),
                            classificationNames2.get(j));
                    }
                }
            }
        }
    }
}

