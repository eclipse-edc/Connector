package com.microsoft.dagx.catalog.atlas.metadata;

import com.microsoft.dagx.spi.DagxException;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.model.SearchFilter;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.type.AtlasTypeUtil;
import org.assertj.core.api.ThrowableAssert;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.util.*;

import static com.microsoft.dagx.spi.util.ConfigurationFunctions.propOrEnv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
public class AtlasApiTest {

    private AtlasApi atlasApi;
    private AtlasClientV2 atlasClient;
    // these are the default atlas connection properties!!
    private String baseUrl = "http://localhost:21000";
    private String username = "admin";
    private String password = "admin";
    private static List<AtlasTypesDef> typeDef;

    @BeforeEach
    void setup() throws IOException {
        baseUrl = propOrEnv("atlas.rest.address", baseUrl);
        username = propOrEnv("atlas.account.username", username);
        password = propOrEnv("atlas.account.password", password);

        atlasClient = new AtlasClientV2(new String[]{baseUrl}, new String[]{username, password});
        atlasApi = new AtlasApiImpl(atlasClient);

        if (typeDef == null)
            typeDef = createTypDefs();
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
            add(new TypeAttribute("width", "float", true));
            add(new TypeAttribute("height", "int", true));
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
            add(new TypeAttribute("width", "float", true));
            add(new TypeAttribute("height", "foobar", true));
        }});

        assertThatThrownBy(action).isInstanceOf(DagxException.class).hasMessageContaining("Given typename foobar was invalid");
    }

    @Test
    void createCustomType_alreadyExists() {
        var ts = System.currentTimeMillis();
        String typeName = "MyCustomType" + ts;
        atlasApi.createCustomTypes(typeName, Collections.singleton("DataSet"), new ArrayList<>() {{
            add(new TypeAttribute("name", "float", true));
        }});

        assertThatThrownBy(() -> atlasApi.createCustomTypes(typeName, Collections.singleton("DataSet"), new ArrayList<>() {{
            add(new TypeAttribute("name", "float", true));
        }})).isInstanceOf(DagxException.class).hasMessageContaining("already exists");

    }

    @Test
    void createCustomType_invalidTypeName() {
        String typeName = "My-Custom-Type";
        assertThatThrownBy(() -> atlasApi.createCustomTypes(typeName, Collections.singleton("DataSet"), new ArrayList<>() {{
            add(new TypeAttribute("name", "float", true));
        }})).isInstanceOf(DagxException.class).hasMessageContaining("invalid");
    }

    @Test
    void deleteCustomType() throws AtlasServiceException {
        var ts = System.currentTimeMillis();
        String typeName = "MyCustomType" + ts;
        var typeDef = atlasApi.createCustomTypes(typeName, Collections.singleton("DataSet"), new ArrayList<>() {{
            add(new TypeAttribute("name", "float", true));
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
            add(new TypeAttribute("name", "string", true));
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<AtlasTypesDef> createTypDefs() throws IOException {
        List<AtlasTypesDef> atlasTypesDefs = new ArrayList<>();

        var mapper = new ObjectMapper();
        Map[] entities = mapper.readValue(this.getClass().getClassLoader().getResourceAsStream("test-types.json"), Map[].class);

        for (Map<?, ?> entity : entities) {
            AtlasEntityDef atlasEntityDef = AtlasTypeUtil.createClassTypeDef((String) entity.get("typeName"),
                    new HashSet<>((ArrayList<String>) entity.get("superTypeNames")));

            List<AtlasStructDef.AtlasAttributeDef> atlasAttributes = new ArrayList<AtlasStructDef.AtlasAttributeDef>();

            for (Map<String, Object> attribute : (ArrayList<Map<String, Object>>) entity.get("attributes")) {
                String attributeType = (String) attribute.get("type");
                String attributeName = (String) attribute.get("name");

                if ((boolean) attribute.getOrDefault("required", false)) {
                    atlasAttributes.add(AtlasTypeUtil.createRequiredAttrDef(attributeName, attributeType));
                } else {
                    atlasAttributes.add(AtlasTypeUtil.createOptionalAttrDef(attributeName, attributeType));
                }
            }

            atlasEntityDef.setAttributeDefs(atlasAttributes);

            AtlasTypesDef typesDef = new AtlasTypesDef();
            typesDef.setEntityDefs(Collections.singletonList(atlasEntityDef));

            try {
                var atlasTypesDef = atlasClient.createAtlasTypeDefs(typesDef);

                atlasTypesDefs.add(atlasTypesDef);
            } catch (Exception e) {
                System.out.println("Error creating types: " + e.getMessage());
            }
        }
        return atlasTypesDefs;
    }

}
