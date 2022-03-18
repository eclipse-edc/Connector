package org.eclipse.dataspaceconnector.catalog.node.directory.azure;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDbApi;
import org.eclipse.dataspaceconnector.catalog.node.directory.azure.model.FederatedCacheNodeDocument;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


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
        api = mock(CosmosDbApi.class);
        directory = new CosmosFederatedCacheNodeDirectory(api, PARTITION_KEY, typeManager, retryPolicy);
    }

    @Test
    void insert() {
        FederatedCacheNode node = createNode();
        api.saveItem(any(FederatedCacheNodeDocument.class));

        List<FederatedCacheNodeDocument> documents = new ArrayList<>();
        doAnswer(i -> {
            FederatedCacheNodeDocument passedNode = i.getArgument(0);
            documents.add(passedNode);
            return null;
        }).when(api).saveItem(any(FederatedCacheNodeDocument.class));

        directory.insert(node);

        assertThat(documents)
                .hasSize(1)
                .allSatisfy(doc -> {
                    assertThat(doc.getPartitionKey()).isEqualTo(PARTITION_KEY);
                    assertNodesAreEqual(doc.getWrappedInstance(), node);
                });
        verify(api).saveItem(any(FederatedCacheNodeDocument.class));
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

        when(api.queryAllItems(anyString())).thenReturn(documents);

        List<FederatedCacheNode> result = directory.getAll();

        assertThat(result).hasSize(nbNodes);
        nodes.forEach(expected -> assertThat(result).anySatisfy(node -> assertNodesAreEqual(expected, node)));
        verify(api).queryAllItems(anyString());
    }
}