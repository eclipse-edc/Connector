/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.catalog.atlas.metadata;

import com.microsoft.dagx.schema.SchemaAttribute;
import com.microsoft.dagx.spi.DagxException;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.model.SearchFilter;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.typedef.*;
import org.apache.atlas.type.AtlasTypeUtil;
import org.assertj.core.api.ThrowableAssert;
import org.codehaus.jackson.map.ObjectMapper;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.microsoft.dagx.spi.util.ConfigurationFunctions.propOrEnv;
import static org.apache.atlas.type.AtlasTypeUtil.createRelationshipTypeDef;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
public class AtlasApiTest {

    private static final String RELATION_TYPE_NAME = "dataset_policy_relation";
    private static List<AtlasTypesDef> typeDef;
    private AtlasApi atlasApi;
    private AtlasClientV2 atlasClient;
    // these are the default atlas connection properties!!
    private String baseUrl = "http://localhost:21000";
    private String username = "admin";
    private String password = "admin";

    @BeforeEach
    void setup() throws IOException {
        baseUrl = propOrEnv("atlas.url", baseUrl);
        username = propOrEnv("atlas.username", username);
        password = propOrEnv("atlas.password", password);

        atlasClient = new AtlasClientV2(new String[]{baseUrl}, new String[]{username, password});
        atlasApi = new AtlasApiImpl(atlasClient);

        if (typeDef == null) {
            typeDef = createTypesAndRelations();
        }
    }

    @Test
    void createEntity() throws AtlasServiceException {
        var guid = atlasApi.createEntity("TestEntity", new HashMap<>() {{
            put("name", "TestEntity");
            put("displayName", "Sample Test Entity");
            put("qualifiedName", "This is just a Test Blob Entity");
            put("account", "TestAccount");
            put("someNumber", 42);
        }});

        assertThat(guid).isNotNull().isNotEmpty();
        AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo = atlasClient.getEntityByGuid(guid);
        assertThat(entityWithExtInfo).isNotNull();
        assertThat(entityWithExtInfo.getEntity().getAttributes())
                .hasFieldOrPropertyWithValue("someNumber", 42)
                .hasFieldOrPropertyWithValue("account", "TestAccount");
    }

    @Test
    void createEntity_typeDoesNotExist() {
        assertThatThrownBy(() -> atlasApi.createEntity("NotExist", new HashMap<>() {{
            put("name", "Test-Entity");
            put("displayName", "Sample Test Entity");
            put("qualifiedName", "This is just a Test Blob Entity");
            put("account", "TestAccount");
            put("someNumber", 42);
        }})).isInstanceOf(DagxException.class).hasMessageContaining("Type ENTITY with name NotExist does not exist");
    }

    @Test
    void createEntity_missingRequiredProps() {
        assertThatThrownBy(() -> atlasApi.createEntity("TestEntity", new HashMap<>() {{
            put("name", "Test-Entity");
            put("displayName", "Sample Test Entity");
            put("account", "TestAccount");
            put("someNumber", 42);
        }})).isInstanceOf(DagxException.class).hasMessageContaining("TestEntity.qualifiedName: mandatory attribute value missing");
    }

    @Test
    void createEntity_alreadyExists_shouldUpdate() throws AtlasServiceException {
        var guid = atlasApi.createEntity("TestEntity", new HashMap<>() {{
            put("name", "TestEntity");
            put("displayName", "Sample Test Entity");
            put("qualifiedName", "This is just a Test Blob Entity");
            put("account", "TestAccount");
            put("someNumber", 42);
        }});
        assertThat(guid).isNotNull();

        var guid2 = atlasApi.createEntity("TestEntity", new HashMap<>() {{
            put("name", "TestEntity");
            put("displayName", "Sample Test Entity");
            put("qualifiedName", "This is just a Test Blob Entity");
            put("account", "TestAccount");
            put("someNumber", 69);
        }});
        assertThat(guid).isEqualTo(guid2);
        AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo = atlasClient.getEntityByGuid(guid);
        assertThat(entityWithExtInfo.getEntity().getAttribute("someNumber")).isEqualTo(69);
    }

    @Test
    void deleteEntity() throws AtlasServiceException {
        var ts = System.currentTimeMillis();
        var guid = atlasApi.createEntity("TestEntity", new HashMap<>() {{
            put("name", "ToDelete" + ts);
            put("displayName", "ToDelete Test Entity");
            put("qualifiedName", "somequalifiedname");
            put("account", "TestAccount");
            put("someNumber", 42);
        }});
        assertThat(atlasClient.getEntityByGuid(guid).getEntity()).isNotNull();

        atlasApi.deleteEntities(Collections.singletonList(guid));

        assertThat(atlasClient.getEntityByGuid(guid)).isNotNull();
        assertThat(atlasClient.getEntityByGuid(guid).getEntity().getStatus()).isEqualTo(AtlasEntity.Status.DELETED);
    }

    @Test
    void deleteEntity_notExist() {
        //does not throw
        atlasApi.deleteEntities(Collections.singletonList(UUID.randomUUID().toString()));
    }

    @Test
    void deleteEntity_alreadyDeleted() throws AtlasServiceException {
        var ts = System.currentTimeMillis();

        var guid = atlasApi.createEntity("TestEntity", new HashMap<>() {{
            put("name", "ToDelete" + ts);
            put("displayName", "ToDelete Test Entity");
            put("qualifiedName", "somequalifiedname");
            put("account", "TestAccount");
            put("someNumber", 42);
        }});
        atlasApi.deleteEntities(Collections.singletonList(guid));
        assertThat(atlasClient.getEntityByGuid(guid).getEntity().getStatus()).isEqualTo(AtlasEntity.Status.DELETED);

        atlasApi.deleteEntities(Collections.singletonList(guid));
    }

    @Test
    void findEntityById() throws AtlasServiceException {
        var guid = atlasApi.createEntity("TestEntity", new HashMap<>() {{
            put("name", "TestEntity");
            put("displayName", "Sample Test Entity");
            put("qualifiedName", "somequalifiedname");
            put("account", "TestAccount");
            put("someNumber", 42);
        }});
        assertThat(atlasClient.getEntityByGuid(guid).getEntity()).isNotNull();

        AtlasEntity entityById = atlasApi.getEntityById(guid);
        assertThat(entityById).isNotNull();
        assertThat(entityById.getAttribute("qualifiedName")).isEqualTo("somequalifiedname");
        assertThat(entityById.getAttribute("someNumber")).isEqualTo(42);
    }

    @Test
    void findEntity_notExist() {
        AtlasEntity entityById = atlasApi.getEntityById(UUID.randomUUID().toString());
        assertThat(entityById).isNull();
    }

    @Test
    void findEntity_notAGuid() {
        AtlasEntity entityById = atlasApi.getEntityById("not a valid id");
        assertThat(entityById).isNull();
    }

    @Test
    void createClassification() {
        var ts = System.currentTimeMillis();
        var typesDef = atlasApi.createClassifications("TestClassification1_" + ts, "TestClassification2_" + ts);
        assertThat(typesDef).isNotNull();
        assertThat(typesDef.getClassificationDefs()).hasSize(2);

    }

    @Test
    void createClassification_alreadyExists() throws AtlasServiceException {
        var ts = System.currentTimeMillis();
        var name = "TestClass_" + ts;
        atlasApi.createClassifications(name);
        assertThatThrownBy(() -> atlasApi.createClassifications(name)).isInstanceOf(DagxException.class).hasMessageContaining("already exists");
        SearchFilter searchFilter = new SearchFilter();
        searchFilter.setParam("name", name);
        AtlasTypesDef allTypeDefs = atlasClient.getAllTypeDefs(searchFilter);
        assertThat(allTypeDefs.getClassificationDefs()).hasSize(1);
    }

    @Test
    void deleteClassification() throws AtlasServiceException {
        var ts = System.currentTimeMillis();
        final var name = "TestClass_" + ts;
        atlasApi.createClassifications(name);

        atlasApi.deleteClassification(name);

        SearchFilter searchFilter = new SearchFilter();
        searchFilter.setParam("name", name);
        AtlasTypesDef allTypeDefs = atlasClient.getAllTypeDefs(searchFilter);
        assertThat(allTypeDefs.getClassificationDefs()).isEmpty();
    }

    @Test
    void deleteClassification_notExist() {

        assertThatThrownBy(() -> atlasApi.deleteClassification("I_Dont_Exist")).isInstanceOf(DagxException.class)
                .hasMessageContaining("No Classification types exist for the given names");
    }

    @Test
    void deleteClassification_hasAssociatedEntity() {
        var ts = System.currentTimeMillis();
        var name = "MyClassification" + ts;
        AtlasTypesDef def = atlasApi.createClassifications(name);
        var guid = atlasApi.createEntity("TestEntity", new HashMap<>() {{
            String entityname = "TestEntity" + ts;
            put("name", entityname);
            put("displayName", entityname);
            put("qualifiedName", entityname);
            put("account", "TestAccount");
            put("someNumber", 42);
            put("classifications", Collections.singletonList(name));
        }});

        assertThat(guid).isNotNull();
        assertThatThrownBy(() -> atlasApi.deleteClassification(name)).isInstanceOf(DagxException.class);
    }

    @Test
    void createCustomType() throws AtlasServiceException {
        var ts = System.currentTimeMillis();
        String typeName = "MyCustomType" + ts;
        AtlasTypesDef customTypes = atlasApi.createCustomTypes(typeName, Collections.singleton("DataSet"), new ArrayList<>() {{
            add(new AtlasCustomTypeAttribute("width", "float", true));
            add(new AtlasCustomTypeAttribute("height", "int", true));
        }});
        assertThat(customTypes).isNotNull();
        assertThat(customTypes.getEntityDefs()).hasSize(1);

        var sf = new SearchFilter();
        sf.setParam(SearchFilter.PARAM_NAME, typeName);
        AtlasTypesDef allcustomTypes = atlasClient.getAllTypeDefs(sf);
        assertThat(allcustomTypes).isNotNull();
    }

    @Test
    void createCustomType_invalidAttributeType() {
        var ts = System.currentTimeMillis();
        ThrowableAssert.ThrowingCallable action = () -> atlasApi.createCustomTypes("MyCustomType" + ts, Collections.singleton("DataSet"), new ArrayList<>() {{
            add(new AtlasCustomTypeAttribute("width", "float", true));
            add(new AtlasCustomTypeAttribute("height", "foobar", true));
        }});

        assertThatThrownBy(action).isInstanceOf(DagxException.class).hasMessageContaining("Given typename foobar was invalid");
    }

    @Test
    void createCustomType_alreadyExists_shouldUpdate() {
        var ts = System.currentTimeMillis();
        String typeName = "MyCustomType" + ts;
        ArrayList<SchemaAttribute> attributes = new ArrayList<>() {{
            add(new AtlasCustomTypeAttribute("name", "float", true));
        }};
        atlasApi.createCustomTypes(typeName, Collections.singleton("DataSet"), attributes);

        attributes.add(new SchemaAttribute("newAttribute", false));
        var updatedDef = atlasApi.createCustomTypes(typeName, Collections.singleton("DataSet"), attributes);

        assertThat(updatedDef.getEntityDefs().get(0).getAttribute("newAttribute")).isNotNull();
    }

    @Test
    void createCustomType_alreadyExists_cannotUpdateRequiredField() {
        var ts = System.currentTimeMillis();
        String typeName = "MyCustomType" + ts;
        ArrayList<SchemaAttribute> attributes = new ArrayList<>() {{
            add(new AtlasCustomTypeAttribute("name", "float", true));
        }};
        atlasApi.createCustomTypes(typeName, Collections.singleton("DataSet"), attributes);

        attributes.add(new SchemaAttribute("newAttribute", true));
        assertThatThrownBy(() -> atlasApi.createCustomTypes(typeName, Collections.singleton("DataSet"), attributes))
                .isInstanceOf(DagxException.class)
                .hasMessageContaining("can not add mandatory attribute");
    }

    @Test
    void createCustomType_invalidTypeName() {
        String typeName = "My-Custom-Type";
        assertThatThrownBy(() -> atlasApi.createCustomTypes(typeName, Collections.singleton("DataSet"), new ArrayList<>() {{
            add(new AtlasCustomTypeAttribute("name", "float", true));
        }})).isInstanceOf(DagxException.class).hasMessageContaining("invalid");
    }

    @Test
    void deleteCustomType() throws AtlasServiceException {
        var ts = System.currentTimeMillis();
        String typeName = "MyCustomType" + ts;
        var typeDef = atlasApi.createCustomTypes(typeName, Collections.singleton("DataSet"), new ArrayList<>() {{
            add(new AtlasCustomTypeAttribute("name", "float", true));
        }});

        atlasApi.deleteCustomType(typeName);

        var sf = new SearchFilter();
        sf.setParam(SearchFilter.PARAM_NAME, typeName);

        var typesDef = atlasClient.getAllTypeDefs(sf);
        assertThat(typesDef.getEntityDefs()).isEmpty();

    }

    @Test
    void deleteCustomType_notExist() {
        assertThatThrownBy(() -> atlasApi.deleteCustomType("I_DONT_EXIST")).isInstanceOf(DagxException.class)
                .hasMessage("No Custom TypeDef types exist for the given names: I_DONT_EXIST");
    }

    @Test
    void deleteCustomType_hasAssociatedEntity() {
        var ts = System.currentTimeMillis();
        String typeName = "MyCustomType" + ts;
        var typeDef = atlasApi.createCustomTypes(typeName, Collections.singleton("DataSet"), new ArrayList<>() {{
            add(new AtlasCustomTypeAttribute("name", "string", true));
        }});

        var guid = atlasApi.createEntity(typeName, new HashMap<>() {{
            put("name", "CustomTypeZombie");
            put("displayName", "Sample Test Entity");
            put("qualifiedName", "This is just a Test Blob Entity");
            put("account", "TestAccount");
            put("someNumber", 42);
        }});
        assertThat(guid).isNotNull();
        assertThat(typeDef.getEntityDefs()).hasSize(1);

        assertThatThrownBy(() -> atlasApi.deleteCustomType(typeName)).isInstanceOf(DagxException.class)
                .hasMessageContaining("Given type " + typeName + " has references");
    }

    @Test
    void createRelation() {
        var id = UUID.randomUUID().toString();
        var entityId = atlasApi.createEntity("TestEntity", new HashMap<>() {{
            put("name", "TestEntity" + id);
            put("displayName", "Sample Test Entity");
            put("qualifiedName", "This is just a Test Blob Entity " + id);
            put("account", "TestAccount");
            put("someNumber", 42);
        }});
        var policyId = atlasApi.createEntity("Policy", new HashMap<>() {{
            put("name", "RegionalPolicy" + id);
            put("qualifiedName", "entity-policy-relation " + id);
            put("expression", "foo-bar-baz");
        }});

        var relation = atlasApi.createRelation(entityId, policyId, RELATION_TYPE_NAME);

        assertThat(relation).isNotNull();
        assertThat(relation.getEnd1().getTypeName()).isEqualTo("TestEntity");
        assertThat(relation.getEnd1().getGuid()).isEqualTo(entityId);
        assertThat(relation.getEnd2().getTypeName()).isEqualTo("Policy");
        assertThat(relation.getEnd2().getGuid()).isEqualTo(policyId);
    }

    @Test
    void queryEntities_byPolicy() throws AtlasServiceException {
        var entity1 = atlasApi.createEntity("TestEntity", new HashMap<>() {{
            put("name", "TestEntity1");
            put("displayName", "File1");
            put("qualifiedName", "This is just a Test Blob Entity");
            put("account", "TestAccount");
            put("someNumber", 42);
        }});
        assertThat(entity1).isNotNull().isNotEmpty();

        var entity2 = atlasApi.createEntity("TestEntity", new HashMap<>() {{
            put("name", "TestEntity1");
            put("displayName", "File2");
            put("qualifiedName", "This is just another Test Blob Entity");
            put("account", "TestAccount");
            put("someNumber", 69);
        }});

        var policyId = atlasApi.createEntity("Policy", new HashMap<>() {{
            put("name", "TestPolicy");
            put("expression", "foo-bar");
            put("qualifiedName", "This is a test policy");
        }});

        var rel1 = atlasApi.createRelation(entity1, policyId, RELATION_TYPE_NAME);
        var rel2 = atlasApi.createRelation(entity2, policyId, RELATION_TYPE_NAME);

        try {
            assertThat(entity1).isNotNull().isNotEmpty();
            assertThat(entity1).isNotNull().isNotEmpty();
            assertThat(rel1).isNotNull();
            assertThat(rel2).isNotNull();

            //query the policy, navigate to its associated entities
            var searchResult = atlasClient.dslSearchWithParams("from Policy", 100, 0);
            var policyHeader = searchResult.getEntities().stream().filter(header -> header.getDisplayText().equals("TestPolicy")).findFirst().orElseThrow();
            var policy = atlasApi.getEntityById(policyHeader.getGuid());
            assertThat(policy.getRelationshipAttributes()).containsKey("DagxEntity");

            // get the list of all relations (i.e. relationsip attributes)
            var attributes = policy.getRelationshipAttribute("DagxEntity");
            assertThat(attributes).isNotNull().isInstanceOf(List.class);

            //noinspection unchecked
            var relationships = (List<Map<String, Object>>) attributes;
            assertThat(relationships).describedAs("Policy should have 2 associated entities").hasSize(2);
            assertThat(relationships).describedAs("The relations should both be active and point to TestEntities").allSatisfy(stringObjectMap -> {
                assertThat(stringObjectMap.get("relationshipType")).isEqualTo(RELATION_TYPE_NAME);
                assertThat(stringObjectMap.get("relationshipStatus")).isEqualTo("ACTIVE");
                assertThat(stringObjectMap.get("typeName")).isEqualTo("TestEntity");
            });
        } finally {
            System.out.println("Cleaning up entities and policy");
            atlasClient.deleteEntitiesByGuids(Arrays.asList(entity1, entity2, policyId));
            atlasClient.purgeEntitiesByGuids(Set.of(entity1, entity2, policyId));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<AtlasTypesDef> createTypesAndRelations() throws IOException {
        List<AtlasTypesDef> result = new ArrayList<>();
        var mapper = new ObjectMapper();
        Map[] types = mapper.readValue(getClass().getClassLoader().getResourceAsStream("test-types.json"), Map[].class);
        var entityDefs = new ArrayList<AtlasEntityDef>();

        //create entity type defs from json file
        for (Map<?, ?> type : types) {
            AtlasEntityDef atlasEntityDef = AtlasTypeUtil.createClassTypeDef((String) type.get("typeName"),
                    new HashSet<>((ArrayList<String>) type.get("superTypeNames")));

            List<AtlasStructDef.AtlasAttributeDef> atlasAttributes = new ArrayList<>();
            for (Map<String, Object> attribute : (ArrayList<Map<String, Object>>) type.get("attributes")) {
                String attributeType = (String) attribute.get("type");
                String attributeName = (String) attribute.get("name");

                if ((boolean) attribute.getOrDefault("required", false)) {
                    atlasAttributes.add(AtlasTypeUtil.createRequiredAttrDef(attributeName, attributeType));
                } else {
                    atlasAttributes.add(AtlasTypeUtil.createOptionalAttrDef(attributeName, attributeType));
                }
            }
            atlasEntityDef.setAttributeDefs(atlasAttributes);
            entityDefs.add(atlasEntityDef);

        }

        //create relation from code
        AtlasRelationshipDef relationshipDef;
        var entityEnd = new AtlasRelationshipEndDef("TestEntity", "DagxAccessPolicy", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE);
        var policyEnd = new AtlasRelationshipEndDef("Policy", "DagxEntity", AtlasStructDef.AtlasAttributeDef.Cardinality.SET);
        relationshipDef = createRelationshipTypeDef(RELATION_TYPE_NAME, "Links an entity to a policy", "1.0", AtlasRelationshipDef.RelationshipCategory.ASSOCIATION, AtlasRelationshipDef.PropagateTags.NONE, entityEnd, policyEnd);


        var relTypesDef = new AtlasTypesDef();
        relTypesDef.setRelationshipDefs(Collections.singletonList(relationshipDef));

        Stream<AtlasTypesDef> typesDefStream = entityDefs.stream()
                .map(ed -> {
                    var atlasTypesDef = new AtlasTypesDef();
                    atlasTypesDef.setEntityDefs(Collections.singletonList(ed));
                    return atlasTypesDef;
                });
        return Stream.concat(typesDefStream, Stream.of(relTypesDef))
                .map(this::createOrUpdate)
                .collect(Collectors.toList());
    }

    @Nullable
    private AtlasTypesDef createOrUpdate(AtlasTypesDef typesDef) {
        try {
            return atlasClient.createAtlasTypeDefs(typesDef);
        } catch (AtlasServiceException e) {
            System.out.print("Error creating types, attempting update: " + e.getMessage());
            try {
                AtlasTypesDef atlasTypesDef = atlasClient.updateAtlasTypeDefs(typesDef);
                System.out.println(" --> Update successful!");
                return atlasTypesDef;
            } catch (AtlasServiceException atlasServiceException) {
                System.out.println("\nError updating types: " + e.getMessage());
                return null;
            }
        }
    }

}
