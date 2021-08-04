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

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.assertj.core.api.ThrowableAssert;
import org.eclipse.dataspaceconnector.catalog.atlas.dto.*;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.schema.SchemaAttribute;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.catalog.atlas.dto.Functions.*;
import static org.eclipse.dataspaceconnector.common.ConfigurationFunctions.propOrEnv;

@IntegrationTest
public class AtlasApiTest {

    private static final String RELATION_TYPE_NAME = "dataset_policy_relation";
    private static List<AtlasTypesDef> typeDef;
    private AtlasApi atlasApi;
    // these are the default atlas connection properties!!
    private String baseUrl = "http://localhost:21000";
    private String username = "admin";
    private String password = "admin";

    @BeforeEach
    void setup() throws IOException {
        baseUrl = propOrEnv("edc.atlas.url", baseUrl);
        username = propOrEnv("atlas.username", username);
        password = propOrEnv("atlas.password", password);

        atlasApi = new AtlasApiImpl(baseUrl, username, password, new OkHttpClient.Builder().readTimeout(Duration.ofSeconds(120)).build(), new TypeManager());

        if (AtlasApiTest.typeDef == null) {
            AtlasApiTest.typeDef = createTypesAndRelations();
        }
    }

    @Test
    void createEntity() {
        var guid = atlasApi.createEntity("TestEntity", new HashMap<>() {{
            put("name", "TestEntity");
            put("displayName", "Sample Test Entity");
            put("qualifiedName", "This is just a Test Blob Entity");
            put("account", "TestAccount");
            put("someNumber", 42);
        }});

        assertThat(guid).isNotNull().isNotEmpty();
        AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo = atlasApi.getEntityById(guid);
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
        }})).isInstanceOf(EdcException.class).hasMessageContaining("Type ENTITY with name NotExist does not exist");
    }

    @Test
    void createEntity_missingRequiredProps() {
        assertThatThrownBy(() -> atlasApi.createEntity("TestEntity", new HashMap<>() {{
            put("name", "Test-Entity");
            put("displayName", "Sample Test Entity");
            put("account", "TestAccount");
            put("someNumber", 42);
        }})).isInstanceOf(EdcException.class).hasMessageContaining("TestEntity.qualifiedName: mandatory attribute value missing");
    }

    @Test
    void createEntity_alreadyExists_shouldUpdate() {
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
        AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo = atlasApi.getEntityById(guid);
        assertThat(entityWithExtInfo.getEntity().getAttribute("someNumber")).isEqualTo(69);
    }

    @Test
    void deleteEntity() {
        var ts = System.currentTimeMillis();
        var guid = atlasApi.createEntity("TestEntity", new HashMap<>() {{
            put("name", "ToDelete" + ts);
            put("displayName", "ToDelete Test Entity");
            put("qualifiedName", "somequalifiedname");
            put("account", "TestAccount");
            put("someNumber", 42);
        }});
        assertThat(atlasApi.getEntityById(guid).getEntity()).isNotNull();

        atlasApi.deleteEntities(Collections.singletonList(guid));

        AtlasEntity.AtlasEntityWithExtInfo entity = atlasApi.getEntityById(guid);
        assertThat(entity).isNotNull();
        assertThat(entity.getEntity().getStatus()).isEqualTo(AtlasEntity.Status.DELETED);
    }

    @Test
    void deleteEntity_notExist() {
        //does not throw
        atlasApi.deleteEntities(Collections.singletonList(UUID.randomUUID().toString()));
    }

    @Test
    void deleteEntity_alreadyDeleted() {
        var ts = System.currentTimeMillis();

        var guid = atlasApi.createEntity("TestEntity", new HashMap<>() {{
            put("name", "ToDelete" + ts);
            put("displayName", "ToDelete Test Entity");
            put("qualifiedName", "somequalifiedname");
            put("account", "TestAccount");
            put("someNumber", 42);
        }});
        atlasApi.deleteEntities(Collections.singletonList(guid));
        assertThat(atlasApi.getEntityById(guid).getEntity().getStatus()).isEqualTo(AtlasEntity.Status.DELETED);

    }

    @Test
    void findEntityById() {
        var guid = atlasApi.createEntity("TestEntity", new HashMap<>() {{
            put("name", "TestEntity");
            put("displayName", "Sample Test Entity");
            put("qualifiedName", "somequalifiedname");
            put("account", "TestAccount");
            put("someNumber", 42);
        }});
        assertThat(atlasApi.getEntityById(guid).getEntity()).isNotNull();

        AtlasEntity entityById = atlasApi.getEntityById(guid).getEntity();
        assertThat(entityById).isNotNull();
        assertThat(entityById.getAttribute("qualifiedName")).isEqualTo("somequalifiedname");
        assertThat(entityById.getAttribute("someNumber")).isEqualTo(42);
    }

    @Test
    void findEntity_notExist() {
        AtlasEntity.AtlasEntityWithExtInfo entityById = atlasApi.getEntityById(UUID.randomUUID().toString());
        assertThat(entityById).isNull();
    }

    @Test
    void findEntity_notAGuid() {
        AtlasEntity.AtlasEntityWithExtInfo entityById = atlasApi.getEntityById("not a valid id");
        assertThat(entityById).isNull();
    }

    @Test
    void createClassification() {
        var ts = System.currentTimeMillis();
        var typesDef = atlasApi.createClassifications("TestClassification1_" + ts, "TestClassification2_" + ts);
        assertThat(typesDef).isNotNull();
        assertThat(typesDef.getClassificationDefs()).hasSize(2);
        assertThat(typesDef.getClassificationDefs()).allSatisfy(tc -> assertThat(tc.getGuid()).isNotEmpty());

    }

    @Test
    void createClassification_alreadyExists() {
        var ts = System.currentTimeMillis();
        var name = "TestClass_" + ts;
        atlasApi.createClassifications(name);
        assertThatThrownBy(() -> atlasApi.createClassifications(name)).isInstanceOf(EdcException.class).hasMessageContaining("already exists");
        AtlasTypesDef allTypeDefs = atlasApi.getAllTypes(name);
        assertThat(allTypeDefs.getClassificationDefs()).hasSize(1);
    }

    @Test
    void deleteClassification() {
        var ts = System.currentTimeMillis();
        var name = "TestClass_" + ts;
        atlasApi.createClassifications(name);

        atlasApi.deleteClassification(name);

        AtlasTypesDef allTypeDefs = atlasApi.getAllTypes(name);
        assertThat(allTypeDefs.getClassificationDefs()).isEmpty();
    }

    @Test
    void deleteClassification_notExist() {

        assertThatThrownBy(() -> atlasApi.deleteClassification("I_Dont_Exist")).isInstanceOf(EdcException.class)
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
        assertThatThrownBy(() -> atlasApi.deleteClassification(name)).isInstanceOf(EdcException.class);
    }

    @Test
    void createCustomType() {
        var ts = System.currentTimeMillis();
        String typeName = "MyCustomType" + ts;
        AtlasTypesDef customTypes = atlasApi.createCustomTypes(typeName, Collections.singleton("DataSet"), new ArrayList<>() {{
            add(new AtlasCustomTypeAttribute("width", "float", true));
            add(new AtlasCustomTypeAttribute("height", "int", true));
        }});
        assertThat(customTypes).isNotNull();
        assertThat(customTypes.getEntityDefs()).hasSize(1);

        AtlasTypesDef allcustomTypes = atlasApi.getAllTypes(typeName);
        assertThat(allcustomTypes).isNotNull();
    }

    @Test
    void createCustomType_invalidAttributeType() {
        var ts = System.currentTimeMillis();
        ThrowableAssert.ThrowingCallable action = () -> atlasApi.createCustomTypes("MyCustomType" + ts, Collections.singleton("DataSet"), new ArrayList<>() {{
            add(new AtlasCustomTypeAttribute("width", "float", true));
            add(new AtlasCustomTypeAttribute("height", "foobar", true));
        }});

        assertThatThrownBy(action).isInstanceOf(EdcException.class).hasMessageContaining("Given typename foobar was invalid");
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
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("can not add mandatory attribute");
    }

    @Test
    void createCustomType_invalidTypeName() {
        String typeName = "My-Custom-Type";
        assertThatThrownBy(() -> atlasApi.createCustomTypes(typeName, Collections.singleton("DataSet"), new ArrayList<>() {{
            add(new AtlasCustomTypeAttribute("name", "float", true));
        }})).isInstanceOf(EdcException.class).hasMessageContaining("invalid");
    }

    @Test
    void deleteCustomType() {
        var ts = System.currentTimeMillis();
        String typeName = "MyCustomType" + ts;
        var typeDef = atlasApi.createCustomTypes(typeName, Collections.singleton("DataSet"), new ArrayList<>() {{
            add(new AtlasCustomTypeAttribute("name", "float", true));
        }});

        atlasApi.deleteCustomType(typeName);


        var typesDef = atlasApi.getAllTypes(typeName);
        assertThat(typesDef.getEntityDefs()).isEmpty();

    }

    @Test
    void deleteCustomType_notExist() {
        assertThatThrownBy(() -> atlasApi.deleteCustomType("I_DONT_EXIST")).isInstanceOf(EdcException.class)
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

        assertThatThrownBy(() -> atlasApi.deleteCustomType(typeName)).isInstanceOf(EdcException.class)
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

        var relation = atlasApi.createRelationship(entityId, policyId, AtlasApiTest.RELATION_TYPE_NAME);

        assertThat(relation).isNotNull();
        assertThat(relation.getEnd1().getTypeName()).isEqualTo("TestEntity");
        assertThat(relation.getEnd1().getGuid()).isEqualTo(entityId);
        assertThat(relation.getEnd2().getTypeName()).isEqualTo("Policy");
        assertThat(relation.getEnd2().getGuid()).isEqualTo(policyId);
    }

    @Test
    void queryEntities_byPolicy() {
        var uuid = UUID.randomUUID().toString();
        var entity1 = atlasApi.createEntity("TestEntity", new HashMap<>() {{
            put("name", "TestEntity1_" + uuid);
            put("displayName", "File1_" + uuid);
            put("qualifiedName", "This is just a Test Blob Entity");
            put("account", "TestAccount");
            put("someNumber", 42);
        }});
        assertThat(entity1).isNotNull().isNotEmpty();

        var entity2 = atlasApi.createEntity("TestEntity", new HashMap<>() {{
            put("name", "TestEntity2_" + uuid);
            put("displayName", "File2_" + uuid);
            put("qualifiedName", "This is just another Test Blob Entity");
            put("account", "TestAccount");
            put("someNumber", 69);
        }});

        var policyId = atlasApi.createEntity("Policy", new HashMap<>() {{
            put("name", "TestPolicy");
            put("expression", "foo-bar");
            put("qualifiedName", "This is a test policy");
        }});

        var rel1 = atlasApi.createRelationship(entity1, policyId, AtlasApiTest.RELATION_TYPE_NAME);
        var rel2 = atlasApi.createRelationship(entity2, policyId, AtlasApiTest.RELATION_TYPE_NAME);

        try {
            assertThat(entity1).isNotNull().isNotEmpty();
            assertThat(entity1).isNotNull().isNotEmpty();
            assertThat(rel1).isNotNull();
            assertThat(rel2).isNotNull();

            //query the policy, navigate to its associated entities
            var searchResult = atlasApi.dslSearchWithParams("from Policy", 100, 0);
            var policyHeader = searchResult.getEntities().stream().filter(header -> header.getDisplayText().equals("TestPolicy")).findFirst().orElseThrow();
            var policy = atlasApi.getEntityById(policyHeader.getGuid());
            assertThat(policy.getEntity().getRelationshipAttributes()).containsKey("EdcEntity");

            // get the list of all relations (i.e. relationsip attributes)
            var attributes = policy.getEntity().getRelationshipAttribute("EdcEntity");
            assertThat(attributes).isNotNull().isInstanceOf(List.class);

            var relationships = (List<Map<String, Object>>) attributes;
            assertThat(relationships).describedAs("Policy should have 2 associated entities").hasSize(2);
            assertThat(relationships).describedAs("The relations should both be active and point to TestEntities").allSatisfy(stringObjectMap -> {
                assertThat(stringObjectMap.get("relationshipType")).isEqualTo(AtlasApiTest.RELATION_TYPE_NAME);
                assertThat(stringObjectMap.get("relationshipStatus")).isEqualTo("ACTIVE");
                assertThat(stringObjectMap.get("typeName")).isEqualTo("TestEntity");
            });
        } finally {
            System.out.println("Cleaning up entities and policy");
            atlasApi.deleteEntities(Arrays.asList(entity1, entity2, policyId));
//            atlasApi.purgeEntities(Set.of(entity1, entity2, policyId));
        }
    }

    private List<AtlasTypesDef> createTypesAndRelations() throws IOException {
        List<AtlasTypesDef> result = new ArrayList<>();
        var mapper = new ObjectMapper();
        Map[] types = mapper.readValue(getClass().getClassLoader().getResourceAsStream("test-types.json"), Map[].class);
        var entityDefs = new ArrayList<AtlasEntityDef>();

        //create entity type defs from json file
        for (Map<?, ?> type : types) {
            AtlasEntityDef atlasEntityDef = createClassTypeDef((String) type.get("typeName"),
                    new HashSet<>((ArrayList<String>) type.get("superTypeNames")));

            List<AtlasStructDef.AtlasAttributeDef> atlasAttributes = new ArrayList<>();
            for (Map<String, Object> attribute : (ArrayList<Map<String, Object>>) type.get("attributes")) {
                String attributeType = (String) attribute.get("type");
                String attributeName = (String) attribute.get("name");

                if ((boolean) attribute.getOrDefault("required", false)) {
                    atlasAttributes.add(createRequiredAttrDef(attributeName, attributeType));
                } else {
                    atlasAttributes.add(createOptionalAttrDef(attributeName, attributeType));
                }
            }
            atlasEntityDef.setAttributeDefs(atlasAttributes);
            entityDefs.add(atlasEntityDef);

        }

        //create relation from code
        AtlasRelationshipDef relationshipDef;
        var entityEnd = new AtlasRelationshipEndDef("TestEntity", "EdcAccessPolicy", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE);
        var policyEnd = new AtlasRelationshipEndDef("Policy", "EdcEntity", AtlasStructDef.AtlasAttributeDef.Cardinality.SET);
        relationshipDef = createRelationshipTypeDef(AtlasApiTest.RELATION_TYPE_NAME, "Links an entity to a policy", "1.0", AtlasRelationshipDef.RelationshipCategory.ASSOCIATION, AtlasRelationshipDef.PropagateTags.NONE, entityEnd, policyEnd);


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

    private AtlasTypesDef createOrUpdate(AtlasTypesDef typesDef) {
        try {
            return ((AtlasApiImpl) atlasApi).createTypesDef(typesDef);
        } catch (EdcException e) {
            System.out.print("Error creating types, attempting update: " + e.getMessage());
            try {
                AtlasTypesDef atlasTypesDef = ((AtlasApiImpl) atlasApi).updateTypesDef(typesDef);
                System.out.println(" --> Update successful!");
                return atlasTypesDef;
            } catch (EdcException EdcException) {
                System.out.println("\nError updating types: " + e.getMessage());
                return null;
            }
        }
    }

}
