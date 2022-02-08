package org.eclipse.dataspaceconnector.contract.definition.store;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import com.azure.cosmos.models.PartitionKey;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.contract.definition.store.model.ContractDefinitionDocument;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApi;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApiImpl;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;
import static org.eclipse.dataspaceconnector.contract.definition.store.TestFunctions.generateDefinition;
import static org.eclipse.dataspaceconnector.contract.definition.store.TestFunctions.generateDocument;

@IntegrationTest
public class CosmosContractDefinitionStoreIntegrationTest {
    public static final String REGION = "westeurope";
    private static final String TEST_ID = UUID.randomUUID().toString();
    private static final String ACCOUNT_NAME = "cosmos-itest";
    private static final String DATABASE_NAME = "connector-itest-" + TEST_ID;
    private static final String CONTAINER_PREFIX = "ContractDefinitionStore-";
    private static CosmosContainer container;
    private static CosmosDatabase database;
    private TypeManager typeManager;
    private CosmosContractDefinitionStore store;

    @BeforeAll
    static void prepareCosmosClient() {
        var key = propOrEnv("COSMOS_KEY", null);
        if (key != null) {
            var client = new CosmosClientBuilder()
                    .key(key)
                    .preferredRegions(Collections.singletonList(REGION))
                    .consistencyLevel(ConsistencyLevel.SESSION)
                    .endpoint("https://" + ACCOUNT_NAME + ".documents.azure.com:443/")
                    .buildClient();

            CosmosDatabaseResponse response = client.createDatabaseIfNotExists(DATABASE_NAME);
            database = client.getDatabase(response.getProperties().getId());
        }
    }

    @AfterAll
    static void deleteDatabase() {
        if (database != null) {
            CosmosDatabaseResponse delete = database.delete();
            assertThat(delete.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(300);
        }
    }

    @BeforeEach
    void setUp(TestInfo testInfo) {
        var containerName = CONTAINER_PREFIX + testInfo.getDisplayName();
        assertThat(database).describedAs("CosmosDB database is null - did something go wrong during initialization?").isNotNull();
        CosmosContainerResponse containerIfNotExists = database.createContainerIfNotExists(containerName, "/partitionKey");
        container = database.getContainer(containerIfNotExists.getProperties().getId());
        typeManager = new TypeManager();
        typeManager.registerTypes(ContractDefinition.class, ContractDefinitionDocument.class);
        CosmosDbApi cosmosDbApi = new CosmosDbApiImpl(container, true);
        store = new CosmosContractDefinitionStore(cosmosDbApi, typeManager, new RetryPolicy<>().withMaxRetries(3).withBackoff(1, 5, ChronoUnit.SECONDS));
    }

    @Test
    void findAll() {
        var doc1 = generateDocument();
        var doc2 = generateDocument();
        container.createItem(doc1);
        container.createItem(doc2);

        store.reload();
        assertThat(store.findAll()).hasSize(2).containsExactlyInAnyOrder(doc1.getWrappedInstance(), doc2.getWrappedInstance());
    }

    @Test
    void findAll_noReload() {
        var doc1 = generateDocument();
        var doc2 = generateDocument();
        container.createItem(doc1);
        container.createItem(doc2);

        assertThat(store.findAll()).hasSize(2);
    }

    @Test
    void findAll_emptyResult() {
        assertThat(store.findAll()).isNotNull().isEmpty();
    }

    @Test
    void save() {
        ContractDefinition def = generateDefinition();
        store.save(def);

        var actual = container.readAllItems(new PartitionKey(def.getAccessPolicy().getUid()), Object.class);
        assertThat(actual).hasSize(1);
        var doc = actual.stream().findFirst().get();
        assertThat(convert(doc)).isEqualTo(def);
    }

    @Test
    void save_exists_shouldUpdate() {
        var doc1 = generateDocument();
        container.createItem(doc1);

        var defToAdd = doc1.getWrappedInstance();

        //modify a single field
        defToAdd.getSelectorExpression().getCriteria().add(new Criterion("anotherkey", "isGreaterThan", "anotherValue"));

        store.save(defToAdd);
        var actual = container.readAllItems(new PartitionKey(doc1.getPartitionKey()), Object.class);
        assertThat(actual).hasSize(1);
        var first = convert(actual.stream().findFirst().get());
        assertThat(first.getSelectorExpression().getCriteria()).hasSize(2).anySatisfy(criterion -> {
            assertThat(criterion.getOperandLeft()).isNotEqualTo("anotherkey");
            assertThat(criterion.getOperator()).isNotEqualTo("isGreaterThan");
            assertThat(criterion.getOperandLeft()).isNotEqualTo("anotherValue");
        }); //we modified that earlier

    }

    @Test
    void saveAll() {
        var def1 = generateDefinition();
        var def2 = generateDefinition();
        var def3 = generateDefinition();
        var pk = def1.getAccessPolicy().getUid();

        store.save(List.of(def1, def2, def3));

        var allItems = container.readAllItems(new PartitionKey(pk), Object.class);
        assertThat(allItems).hasSize(3);
        var allDefs = allItems.stream().map(this::convert);
        assertThat(allDefs).containsExactlyInAnyOrder(def1, def2, def3);
    }

    @Test
    void update() {
        var doc1 = generateDocument();
        container.createItem(doc1);

        var definition = doc1.getWrappedInstance();
        //modify the object
        definition.getSelectorExpression().getCriteria().add(new Criterion("anotherKey", "NOT EQUAL", "anotherVal"));
        store.update(definition);

        var updatedDefinition = convert(container.readItem(doc1.getId(), new PartitionKey(doc1.getPartitionKey()), Object.class).getItem());
        assertThat(updatedDefinition.getId()).isEqualTo(definition.getId());
        assertThat(updatedDefinition.getSelectorExpression().getCriteria()).hasSize(2)
                .anySatisfy(criterion -> {
                    assertThat(criterion.getOperandLeft()).isNotEqualTo("anotherKey");
                    assertThat(criterion.getOperator()).isNotEqualTo("NOT EQUAL");
                    assertThat(criterion.getOperandLeft()).isNotEqualTo("anotherValue");
                }); //we modified that earlier
    }

    @Test
    void update_notExists() {
        var document = generateDocument();
        var definition = document.getWrappedInstance();
        //modify the object - should insert
        store.update(definition);

        var updatedDefinition = convert(container.readItem(document.getId(), new PartitionKey(document.getPartitionKey()), Object.class).getItem());
        assertThat(updatedDefinition.getId()).isEqualTo(definition.getId());
        assertThat(updatedDefinition.getSelectorExpression().getCriteria()).hasSize(1);
    }

    @Test
    void delete() {
        var doc1 = generateDocument();
        container.createItem(doc1);

        store.delete(doc1.getId());

        assertThat(container.readAllItems(new PartitionKey(doc1.getPartitionKey()), Object.class)).isEmpty();
    }

    @Test
    void delete_notExist() {
        assertThatThrownBy(() -> store.delete("not-exist-id"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("An object with the ID not-exist-id could not be found!");
    }

    @Test
    void reload() {

    }

    private ContractDefinition convert(Object object) {
        var json = typeManager.writeValueAsString(object);
        return typeManager.readValue(json, ContractDefinitionDocument.class).getWrappedInstance();
    }
}
