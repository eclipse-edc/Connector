package org.eclipse.dataspaceconnector.contract.definition.store;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.CosmosScripts;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import com.azure.cosmos.models.CosmosStoredProcedureProperties;
import com.azure.cosmos.models.CosmosStoredProcedureResponse;
import com.azure.cosmos.models.PartitionKey;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.contract.definition.store.model.ContractNegotiationDocument;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApi;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApiImpl;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;
import static org.eclipse.dataspaceconnector.contract.definition.store.TestFunctions.generateDocument;
import static org.eclipse.dataspaceconnector.contract.definition.store.TestFunctions.generateNegotiation;

@IntegrationTest
public class CosmosContractNegotiationStoreIntegrationTest {
    public static final String REGION = "westeurope";
    private static final String TEST_ID = UUID.randomUUID().toString();
    private static final String ACCOUNT_NAME = "cosmos-itest";
    private static final String DATABASE_NAME = "connector-itest-" + TEST_ID;
    private static final String CONTAINER_PREFIX = "ContractNegotiationStore-";
    private static CosmosContainer container;
    private static CosmosDatabase database;
    private TypeManager typeManager;
    private CosmosContractNegotiationStore store;

    @BeforeAll
    static void prepareCosmosClient() {
        var key = propOrEnv("COSMOS_KEY", null);
        Objects.requireNonNull(key, "COSMOS_KEY cannot be null!");
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
        uploadStoredProcedure(container, "nextForState");
        uploadStoredProcedure(container, "lease");

        typeManager = new TypeManager();
        typeManager.registerTypes(ContractDefinition.class, ContractNegotiationDocument.class);
        CosmosDbApi cosmosDbApi = new CosmosDbApiImpl(container, true);
        store = new CosmosContractNegotiationStore(cosmosDbApi, typeManager, new RetryPolicy<>().withMaxRetries(3).withBackoff(1, 5, ChronoUnit.SECONDS));
    }

    @Test
    void findById() {
        var doc1 = generateDocument();
        var doc2 = generateDocument();

        container.createItem(doc1);
        container.createItem(doc2);

        var foundItem = store.find(doc1.getId());

        assertThat(foundItem).isNotNull().isEqualTo(doc1.getWrappedInstance());
    }

    @Test
    void findById_notExist() {
        var foundItem = store.find("not-exit");
        assertThat(foundItem).isNull();
    }

    @Test
    void findForCorrelationId() {
        var doc1 = generateDocument();
        var doc2 = generateDocument();

        container.createItem(doc1);
        container.createItem(doc2);

        var corrId = doc1.getWrappedInstance().getCorrelationId();
        var foundItem = store.findForCorrelationId(corrId);

        assertThat(foundItem).isNotNull().isEqualTo(doc1.getWrappedInstance());
    }

    @Test
    void findForCorrelationId_notFound() {
        var doc1 = generateDocument();
        var doc2 = generateDocument();

        container.createItem(doc1);
        container.createItem(doc2);

        var foundItem = store.findForCorrelationId("not-exit");

        assertThat(foundItem).isNull();
    }

    @Test
    void findContractAgreement() {
        var doc1 = generateDocument();
        var doc2 = generateDocument();

        container.createItem(doc1);
        container.createItem(doc2);

        var foundItem = store.findContractAgreement(doc1.getWrappedInstance().getContractAgreement().getId());

        assertThat(foundItem).isNotNull().isEqualTo(doc1.getWrappedInstance().getContractAgreement());
    }

    @Test
    void findContractAgreement_notFound() {
        var foundItem = store.findContractAgreement("not-exist");

        assertThat(foundItem).isNull();
    }

    @Test
    void save_notExists_shouldCreate() {
        var negotiation = generateNegotiation();
        store.save(negotiation);

        var allObjs = container.readAllItems(new PartitionKey(String.valueOf(negotiation.getState())), Object.class);

        assertThat(allObjs).hasSize(1);
        assertThat(allObjs).allSatisfy(o -> assertThat(toNegotiation(o)).isEqualTo(negotiation));
    }

    @Test
    void save_exists_shouldUpdate() {
        var negotiation = generateNegotiation();
        container.createItem(new ContractNegotiationDocument(negotiation));

        assertThat(container.readAllItems(new PartitionKey(String.valueOf(negotiation.getState())), Object.class)).hasSize(1);

        //add an offer, should modifu
        var newOffer = ContractOffer.Builder.newInstance().policy(Policy.Builder.newInstance().build()).id("new-offer-1").build();
        negotiation.getContractOffers().add(newOffer);
        store.save(negotiation);

        var allObjs = container.readAllItems(new PartitionKey(String.valueOf(negotiation.getState())), Object.class);

        assertThat(allObjs).hasSize(1);
        assertThat(allObjs).allSatisfy(o -> {
            var actual = toNegotiation(o);
            assertThat(actual.getContractOffers()).hasSize(1).containsExactlyInAnyOrder(newOffer);
        });
    }

    @Test
    void nextForState() {
    }

    @Test
    void nextForState_noResult() {
    }

    @Test
    void nextForState_leasedByAnother() {
    }

    @Test
    void nextForState_leasedBySelf() {
    }

    @Test
    void nextForState_leaseByAnotherExpired() {
    }


    private ContractNegotiation toNegotiation(Object object) {
        var json = typeManager.writeValueAsString(object);
        return typeManager.readValue(json, ContractNegotiationDocument.class).getWrappedInstance();
    }

    private void uploadStoredProcedure(CosmosContainer container, String name) {
        // upload stored procedure
        var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name + ".js");
        if (is == null) {
            throw new AssertionError("The input stream referring to the " + name + " file cannot be null!");
        }

        Scanner s = new Scanner(is).useDelimiter("\\A");
        String body = s.hasNext() ? s.next() : "";
        CosmosStoredProcedureProperties props = new CosmosStoredProcedureProperties(name, body);

        CosmosScripts scripts = container.getScripts();
        if (scripts.readAllStoredProcedures().stream().noneMatch(sp -> sp.getId().equals(name))) {
            CosmosStoredProcedureResponse storedProcedure = scripts.createStoredProcedure(props);
        }
    }
}
