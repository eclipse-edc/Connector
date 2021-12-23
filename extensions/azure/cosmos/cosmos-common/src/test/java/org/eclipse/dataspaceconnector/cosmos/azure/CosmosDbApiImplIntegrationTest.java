package org.eclipse.dataspaceconnector.cosmos.azure;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;

@IntegrationTest
class CosmosDbApiImplIntegrationTest {

    public static final String REGION = "westeurope";
    public static final String PARTITION_KEY = "partitionKey";
    private static final String TEST_ID = UUID.randomUUID().toString();
    private static final String ACCOUNT_NAME = "cosmos-itest";
    private static final String DATABASE_NAME = "connector-itest-" + TEST_ID;
    private static final String CONTAINER_NAME = "CosmosAssetIndexTest-" + TEST_ID;
    private static CosmosContainer container;
    private static CosmosDatabase database;
    private CosmosDbApi cosmosDbApi;
    private ArrayList<TestCosmosDocument> record;

    @BeforeAll
    static void prepare() {
        var key = propOrEnv("COSMOS_KEY", "RYNecVDtJq2WKAcIoONBLzuTBys06kUcP8Rw9Yz5zOzsOQFVGaP8oGuI5qgF5ONQY4VukjkpQ4x7a2jwVvo7SQ==");
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
    static void cleanup() {
        if (database != null) {
            CosmosDatabaseResponse delete = database.delete();
            assertThat(delete.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(300);
        }
    }

    @BeforeEach
    void setup() {
        assertThat(database).describedAs("CosmosDB database is null - did something go wrong during initialization?").isNotNull();
        CosmosContainerResponse containerIfNotExists = database.createContainerIfNotExists(CONTAINER_NAME, "/partitionKey");
        container = database.getContainer(containerIfNotExists.getProperties().getId());
        cosmosDbApi = new CosmosDbApiImpl(container, true);

        record = new ArrayList<>();
    }

    @AfterEach
    void teardown() {
        record.forEach(td -> container.deleteItem(td, new CosmosItemRequestOptions()));
    }

    @Test
    void createItem() {
        var testItem = new TestCosmosDocument("payload", PARTITION_KEY);
        cosmosDbApi.saveItem(testItem);
        record.add(testItem);

        assertThat(container.readAllItems(new PartitionKey(PARTITION_KEY), Object.class)).hasSize(1);
    }

    @Test
    void queryItemById() {
        var testItem = new TestCosmosDocument("payload", PARTITION_KEY);
        container.createItem(testItem);
        record.add(testItem);

        var queryResult = cosmosDbApi.queryItemById(testItem.getId());
        assertThat(queryResult).isNotNull().isInstanceOf(LinkedHashMap.class);

        assertThat((LinkedHashMap) queryResult).containsEntry("id", testItem.getId())
                .containsEntry("partitionKey", PARTITION_KEY)
                .containsEntry("wrappedInstance", "payload");

    }

    @Test
    void queryAllItems() {
        var testItem = new TestCosmosDocument("payload", PARTITION_KEY);
        container.createItem(testItem);
        record.add(testItem);

        var testItem2 = new TestCosmosDocument("payload", PARTITION_KEY);
        container.createItem(testItem2);
        record.add(testItem2);

        assertThat(cosmosDbApi.queryAllItems()).hasSize(2)
                .allSatisfy(o -> {
                    assertThat(o).isInstanceOf(LinkedHashMap.class);
                    assertThat((LinkedHashMap) o).containsEntry("wrappedInstance", "payload");
                    assertThat(((LinkedHashMap) o).get("id")).isIn(testItem.getId(), testItem2.getId());
                });
    }

    @Test
    void queryItems() {
        // not picked up, wrong payload
        var testItem = new TestCosmosDocument("payload", PARTITION_KEY);
        container.createItem(testItem);
        record.add(testItem);

        var testItem2 = new TestCosmosDocument("payload-two", PARTITION_KEY);
        container.createItem(testItem2);
        record.add(testItem2);

        //should not be picked up - wrong case
        var testItem3 = new TestCosmosDocument("Payload-two", PARTITION_KEY);
        container.createItem(testItem3);
        record.add(testItem3);

        // should be picked up, despite the different partition key
        var testItem4 = new TestCosmosDocument("payload-two", "another-partkey");
        container.createItem(testItem4);
        record.add(testItem4);

        var query = "SELECT * FROM t WHERE t.wrappedInstance='payload-two'";
        var result = cosmosDbApi.queryItems(query);
        assertThat(result).hasSize(2);
    }

}