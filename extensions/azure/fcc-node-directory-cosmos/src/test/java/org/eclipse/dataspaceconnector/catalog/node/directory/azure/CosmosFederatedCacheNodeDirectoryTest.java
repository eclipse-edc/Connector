package org.eclipse.dataspaceconnector.catalog.node.directory.azure;

import net.jodah.failsafe.RetryPolicy;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.catalog.node.directory.azure.model.FederatedCacheNodeDocument;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApi;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.verify;

class CosmosFederatedCacheNodeDirectoryTest {

    private static final String PARTITION_KEY = "partition-test";

    private CosmosDbApi api;
    private CosmosFederatedCacheNodeDirectory directory;

    private static void assertNodesAreEqual(FederatedCacheNode node1, FederatedCacheNode node2) {
        assertThat(node1.getName()).isEqualTo(node2.getName());
        assertThat(node1.getTargetUrl()).isEqualTo(node2.getTargetUrl());
        assertThat(node1.getSupportedProtocols()).isEqualTo(node2.getSupportedProtocols());
    }

    private static FederatedCacheNodeDocument createDocument(FederatedCacheNode node) {
        return new FederatedCacheNodeDocument(node, PARTITION_KEY);
    }

    private static FederatedCacheNode createNode() {
        return new FederatedCacheNode(UUID.randomUUID().toString(), UUID.randomUUID().toString(), Collections.singletonList(UUID.randomUUID().toString()));
    }

    @BeforeEach
    public void setUp() {
        TypeManager typeManager = new TypeManager();
        typeManager.registerTypes(FederatedCacheNodeDocument.class, FederatedCacheNode.class);
        RetryPolicy<Object> retryPolicy = new RetryPolicy<>().withMaxRetries(1);
        api = strictMock(CosmosDbApi.class);
        directory = new CosmosFederatedCacheNodeDirectory(api, PARTITION_KEY, typeManager, retryPolicy);
    }

    @AfterEach
    public void tearDown() {
        reset(api);
    }

    @Test
    void insert() {
        FederatedCacheNode node = createNode();
        api.saveItem(anyObject(FederatedCacheNodeDocument.class));

        List<FederatedCacheNodeDocument> documents = new ArrayList<>();
        expectLastCall().andAnswer(() -> {
            FederatedCacheNodeDocument passedNode = (FederatedCacheNodeDocument) EasyMock.getCurrentArguments()[0];
            documents.add(passedNode);
            return null;
        });

        replay(api);

        directory.insert(node);

        assertThat(documents)
                .hasSize(1)
                .allSatisfy(doc -> {
                    assertThat(doc.getPartitionKey()).isEqualTo(PARTITION_KEY);
                    assertNodesAreEqual(doc.getWrappedInstance(), node);
                });

        verify(api);
    }

    @Test
    void queryAll() {
        int nbNodes = 2;
        List<FederatedCacheNode> nodes = new ArrayList<>();
        for (int i = 0; i < nbNodes; i++) {
            nodes.add(createNode());
        }
        List<Object> documents = nodes.stream()
                .map(CosmosFederatedCacheNodeDirectoryTest::createDocument)
                .collect(Collectors.toList());

        expect(api.queryAllItems(anyString())).andReturn(documents);

        replay(api);

        List<FederatedCacheNode> result = directory.getAll();
        assertThat(result).hasSize(nbNodes);
        nodes.forEach(expected -> assertThat(result).anySatisfy(node -> assertNodesAreEqual(expected, node)));

        verify(api);
    }
}