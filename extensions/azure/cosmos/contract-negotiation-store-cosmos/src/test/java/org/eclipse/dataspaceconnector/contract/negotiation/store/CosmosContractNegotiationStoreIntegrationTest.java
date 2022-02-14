package org.eclipse.dataspaceconnector.contract.negotiation.store;

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
import org.eclipse.dataspaceconnector.contract.negotiation.store.model.ContractNegotiationDocument;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApi;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApiImpl;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;
import static org.eclipse.dataspaceconnector.contract.negotiation.store.TestFunctions.generateDocument;
import static org.eclipse.dataspaceconnector.contract.negotiation.store.TestFunctions.generateNegotiation;

@IntegrationTest
class CosmosContractNegotiationStoreIntegrationTest {
    public static final String REGION = "westeurope";
    public static final String CONNECTOR_ID = "test-connector";
    private static final String TEST_ID = UUID.randomUUID().toString();
    private static final String ACCOUNT_NAME = "cosmos-itest";
    private static final String DATABASE_NAME = "connector-itest-" + TEST_ID;
    private static final String CONTAINER_PREFIX = "ContractNegotiationStore-";
    private static CosmosContainer container;
    private static CosmosDatabase database;
    private final String partitionKey = CONNECTOR_ID;
    private TypeManager typeManager;
    private CosmosContractNegotiationStore store;

    @BeforeAll
    static void prepareCosmosClient() {
        var key = propOrEnv("COSMOS_KEY", null);
        Objects.requireNonNull(key, "COSMOS_KEY cannot be null!");
        var client = new CosmosClientBuilder()
                .key(key)
                .preferredRegions(Collections.singletonList(REGION))
                .consistencyLevel(ConsistencyLevel.SESSION)
                .endpoint("https://" + ACCOUNT_NAME + ".documents.azure.com:443/")
                .buildClient();

        CosmosDatabaseResponse response = client.createDatabaseIfNotExists(DATABASE_NAME);
        database = client.getDatabase(response.getProperties().getId());
    }

    @AfterAll
    static void deleteDatabase() {
        if (database != null) {
            CosmosDatabaseResponse delete = database.delete();
            assertThat(delete.getStatusCode()).isBetween(200, 300);
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
        store = new CosmosContractNegotiationStore(cosmosDbApi, typeManager, new RetryPolicy<>().withMaxRetries(3).withBackoff(1, 5, ChronoUnit.SECONDS), CONNECTOR_ID);
    }

    @Test
    void findById() {
        var doc1 = generateDocument();
        var doc2 = generateDocument();

        container.createItem(doc1);
        container.createItem(doc2);

        var foundItem = store.find(doc1.getId());

        assertThat(foundItem).isNotNull().usingRecursiveComparison().isEqualTo(doc1.getWrappedInstance());
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

        assertThat(foundItem).isNotNull().usingRecursiveComparison().isEqualTo(doc1.getWrappedInstance());
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

        assertThat(foundItem).isNotNull().usingRecursiveComparison().isEqualTo(doc1.getWrappedInstance().getContractAgreement());
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

        var allObjs = container.readAllItems(new PartitionKey(partitionKey), Object.class);

        assertThat(allObjs).hasSize(1)
                .allSatisfy(o -> assertThat(toNegotiation(o)).usingRecursiveComparison().isEqualTo(negotiation));
    }

    @Test
    void save_exists_shouldUpdate() {
        var negotiation = generateNegotiation();
        container.createItem(new ContractNegotiationDocument(negotiation, partitionKey));

        assertThat(container.readAllItems(new PartitionKey(partitionKey), Object.class)).hasSize(1);

        //add an offer, should modifu
        var newOffer = ContractOffer.Builder.newInstance().policy(Policy.Builder.newInstance().build()).asset(Asset.Builder.newInstance().build()).id("new-offer-1").build();
        negotiation.getContractOffers().add(newOffer);
        store.save(negotiation);

        var allObjs = container.readAllItems(new PartitionKey(partitionKey), Object.class);

        assertThat(allObjs).hasSize(1)
                .allSatisfy(o -> {
                    var actual = toNegotiation(o);
                    assertThat(actual.getContractOffers()).hasSize(1).extracting(ContractOffer::getId).containsExactlyInAnyOrder(newOffer.getId());
                });
    }

    @Test
    void nextForState() {
        var state = ContractNegotiationStates.CONFIRMED;
        var n = generateNegotiation(state);
        container.createItem(new ContractNegotiationDocument(n, partitionKey));

        var result = store.nextForState(state.code(), 10);
        assertThat(result).hasSize(1).allSatisfy(neg -> assertThat(neg).usingRecursiveComparison().isEqualTo(n));
    }

    @Test
    void nextForState_exceedsLimit() {
        var state = ContractNegotiationStates.CONFIRMED;
        var numElements = 10;

        var preparedNegotiations = IntStream.range(0, numElements)
                .mapToObj(i -> generateNegotiation(state))
                .peek(n -> container.createItem(new ContractNegotiationDocument(n, partitionKey)))
                .collect(Collectors.toList());

        var result = store.nextForState(state.code(), 4);
        assertThat(result).hasSize(4)
                .allSatisfy(r -> assertThat(preparedNegotiations).extracting(ContractNegotiation::getId).contains(r.getId()));
    }


    @Test
    void nextForState_noResult() {
        var state = ContractNegotiationStates.CONFIRMED;
        var n = generateNegotiation(state);
        container.createItem(new ContractNegotiationDocument(n, partitionKey));

        var result = store.nextForState(ContractNegotiationStates.PROVIDER_OFFERING.code(), 10);
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void nextForState_onlyReturnsFreeItems() {
        var state = ContractNegotiationStates.CONFIRMED;
        var n1 = generateNegotiation(state);
        var doc1 = new ContractNegotiationDocument(n1, partitionKey);
        container.createItem(doc1);

        var n2 = generateNegotiation(state);
        var doc2 = new ContractNegotiationDocument(n2, partitionKey);
        container.createItem(doc2);

        var n3 = generateNegotiation(state);
        var doc3 = new ContractNegotiationDocument(n3, partitionKey);
        doc3.acquireLease("another-connector");
        container.createItem(doc3);

        var result = store.nextForState(state.code(), 10);
        assertThat(result)
                .hasSize(2)
                .extracting(ContractNegotiation::getId)
                .containsExactlyInAnyOrder(n1.getId(), n2.getId());
    }

    @Test
    void nextForState_leasedByAnother() {
        var state = ContractNegotiationStates.CONFIRMED;
        var n = generateNegotiation(state);
        var doc = new ContractNegotiationDocument(n, partitionKey);
        doc.acquireLease("another-connector");
        container.createItem(doc);

        var result = store.nextForState(state.code(), 10);
        assertThat(result).isEmpty();
    }

    @Test
    void nextForState_leasedBySelf() {
        var state = ContractNegotiationStates.CONFIRMED;
        var n = generateNegotiation(state);
        var doc = new ContractNegotiationDocument(n, partitionKey);
        container.createItem(doc);

        // let's verify that the first invocation correctly sets the lease
        var result = store.nextForState(state.code(), 10);
        assertThat(result).hasSize(1); //should contain the lease already
        var object = container.readAllItems(new PartitionKey(partitionKey), Object.class).stream().findFirst().get();
        var storedNegotiation = toDocument(object);
        assertThat(storedNegotiation.getLease()).isNotNull().hasFieldOrPropertyWithValue("leasedBy", CONNECTOR_ID);

        // now lets verify that the second invocation gives us the same result, since we're the leaser
        result = store.nextForState(state.code(), 10);

        assertThat(result).hasSize(1);
    }

    @Test
    void nextForState_leaseByAnotherExpired() throws InterruptedException {
        var state = ContractNegotiationStates.CONFIRMED;
        var n = generateNegotiation(state);
        var doc = new ContractNegotiationDocument(n, partitionKey);
        doc.acquireLease("another-connector", Duration.ofMillis(10));
        container.createItem(doc);

        Thread.sleep(20); //give the lease time to expire

        var result = store.nextForState(state.code(), 10);
        assertThat(result).hasSize(1).allSatisfy(neg -> assertThat(neg).usingRecursiveComparison().isEqualTo(n));
    }


    @Test
    void findAll_noQuerySpec() {
        var doc1 = generateDocument();
        var doc2 = generateDocument();

        container.createItem(doc1);
        container.createItem(doc2);

        assertThat(store.queryNegotiations(QuerySpec.none())).hasSize(2).extracting(ContractNegotiation::getId).containsExactlyInAnyOrder(doc1.getId(), doc2.getId());
    }

    @Test
    void findAll_verifyPaging() {

        var all = IntStream.range(0, 10).mapToObj(i -> generateDocument())
                .peek(d -> container.createItem(d))
                .map(ContractNegotiationDocument::getId)
                .collect(Collectors.toList());

        // page size fits
        assertThat(store.queryNegotiations(QuerySpec.Builder.newInstance().offset(3).limit(4).build())).hasSize(4)
                .extracting(ContractNegotiation::getId).isSubsetOf(all);

    }

    @Test
    void findAll_verifyPaging_pageSizeLargerThanCollection() {

        var all = IntStream.range(0, 10).mapToObj(i -> generateDocument())
                .peek(d -> container.createItem(d))
                .map(ContractNegotiationDocument::getId)
                .collect(Collectors.toList());

        // page size fits
        assertThat(store.queryNegotiations(QuerySpec.Builder.newInstance().offset(3).limit(40).build())).hasSize(7)
                .extracting(ContractNegotiation::getId).isSubsetOf(all);
    }

    @Test
    void findAll_verifyFiltering() {
        var documents = IntStream.range(0, 10).mapToObj(i -> generateDocument())
                .peek(d -> container.createItem(d))
                .collect(Collectors.toList());

        var expectedId = documents.get(3).getId();

        var query = QuerySpec.Builder.newInstance().filter("id=" + expectedId).build();
        assertThat(store.queryNegotiations(query)).extracting(ContractNegotiation::getId).containsOnly(expectedId);
    }

    @Test
    void findAll_verifyFiltering_invalidFilterExpression() {
        IntStream.range(0, 10).mapToObj(i -> generateDocument())
                .forEach(d -> container.createItem(d));

        var query = QuerySpec.Builder.newInstance().filter("something contains other").build();

        assertThatThrownBy(() -> store.queryNegotiations(query)).isInstanceOfAny(IllegalArgumentException.class).hasMessage("Cannot build SqlParameter for operator: contains");
    }

    @Test
    void findAll_verifyFiltering_unsuccessfulFilterExpression() {
        IntStream.range(0, 10).mapToObj(i -> generateDocument())
                .forEach(d -> container.createItem(d));

        var query = QuerySpec.Builder.newInstance().filter("something = other").build();

        assertThat(store.queryNegotiations(query)).isEmpty();
    }

    @Test
    void findAll_verifySorting() {

        IntStream.range(0, 10).mapToObj(i -> generateDocument())
                .forEach(d -> container.createItem(d));

        var ascendingQuery = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build();
        assertThat(store.queryNegotiations(ascendingQuery)).hasSize(10).isSortedAccordingTo(Comparator.comparing(ContractNegotiation::getId));
        var descendingQuery = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build();
        assertThat(store.queryNegotiations(descendingQuery)).hasSize(10).isSortedAccordingTo((c1, c2) -> c2.getId().compareTo(c1.getId()));
    }

    @Test
    void findAll_sorting_nonExistentProperty() {

        var allIds = IntStream.range(0, 10).mapToObj(i -> generateDocument())
                .peek(d -> container.createItem(d))
                .map(ContractNegotiationDocument::getId)
                .collect(Collectors.toList());


        var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();

        var all = store.queryNegotiations(query).collect(Collectors.toList());
        assertThat(all).isEmpty();
    }

    private ContractNegotiationDocument toDocument(Object object) {
        var json = typeManager.writeValueAsString(object);
        return typeManager.readValue(json, ContractNegotiationDocument.class);
    }

    private ContractNegotiation toNegotiation(Object object) {
        return toDocument(object).getWrappedInstance();
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
