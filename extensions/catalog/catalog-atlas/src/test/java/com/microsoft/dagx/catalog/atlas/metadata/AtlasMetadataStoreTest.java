/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.catalog.atlas.metadata;

import com.microsoft.dagx.catalog.atlas.dto.AtlasEntity;
import com.microsoft.dagx.catalog.atlas.dto.AtlasEntityHeader;
import com.microsoft.dagx.catalog.atlas.dto.AtlasErrorCode;
import com.microsoft.dagx.catalog.atlas.dto.AtlasSearchResult;
import com.microsoft.dagx.policy.model.Policy;
import com.microsoft.dagx.schema.SchemaRegistry;
import com.microsoft.dagx.schema.aws.S3BucketSchema;
import com.microsoft.dagx.schema.azure.AzureBlobStoreSchema;
import com.microsoft.dagx.schema.policy.PolicySchema;
import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import org.easymock.Capture;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.easymock.EasyMock.*;

class AtlasMetadataStoreTest {

    private static final String KEY_NAME = "test-keyname";
    private final AtlasApi atlasApiMock = strictMock(AtlasApi.class);
    private final Monitor monitorMock = niceMock(Monitor.class);
    private final String testEntityName = "test-entity-name";
    private AtlasMetadataStore atlasMetadataStore;

    @BeforeEach
    void setup() {
        SchemaRegistry schemaRegistryMock = mock(SchemaRegistry.class);
        expect(schemaRegistryMock.getSchemas()).andReturn(Arrays.asList(new AzureBlobStoreSchema(), new S3BucketSchema()));
        replay(schemaRegistryMock);
        atlasMetadataStore = new AtlasMetadataStore(atlasApiMock, monitorMock, schemaRegistryMock);
    }

    @Test
    void findForId() {

        final String entityId = UUID.randomUUID().toString();
        AtlasSearchResult searchResult = createSearchResult(entityId, AzureBlobStoreSchema.TYPE);

        final AtlasEntity entity = createAzureEntity(testEntityName);

        expect(atlasApiMock.getEntityById(testEntityName)).andReturn(null);
        expect(atlasApiMock.dslSearchWithParams("from DataSet where name = '" + testEntityName + "'", 100, 0)).andReturn(searchResult);
        expect(atlasApiMock.getEntityById(entityId)).andReturn(new AtlasEntity.AtlasEntityWithExtInfo(entity)).times(1);

        replay(atlasApiMock);
        replay(monitorMock);

        var entry = atlasMetadataStore.findForId(testEntityName);

        assertThat(entry).isNotNull();
        assertThat(entry.getId()).isEqualTo(testEntityName);
        assertThat(entry.getCatalogEntry()).isInstanceOf(AtlasDataCatalogEntry.class);
        assertThat(entry.getCatalogEntry().getAddress()).isNotNull();
        assertAzureEntry(entry);
        verify(atlasApiMock);
        verifyUnexpectedCalls(monitorMock);
    }


    @Test
    void findForId_multipleResults() {
        final String azureId = UUID.randomUUID().toString();
        final String s3Id = UUID.randomUUID().toString();
        AtlasSearchResult searchResult = new AtlasSearchResult();
        final AtlasEntityHeader header = new AtlasEntityHeader(AzureBlobStoreSchema.TYPE, azureId, new HashMap<>());
        header.setStatus(AtlasEntity.Status.ACTIVE);
        final AtlasEntityHeader header2 = new AtlasEntityHeader(S3BucketSchema.TYPE, s3Id, new HashMap<>());
        searchResult.addEntity(header);
        searchResult.addEntity(header2);

        final AtlasEntity azureEntity = createAzureEntity(testEntityName);
        final AtlasEntity s3Entity = createS3Entity(testEntityName);

        expect(atlasApiMock.getEntityById(testEntityName)).andReturn(null);
        expect(atlasApiMock.dslSearchWithParams("from DataSet where name = '" + testEntityName + "'", 100, 0)).andReturn(searchResult);
        expect(atlasApiMock.getEntityById(azureId)).andReturn(new AtlasEntity.AtlasEntityWithExtInfo(azureEntity)).times(1);
        expect(atlasApiMock.getEntityById(s3Id)).andReturn(new AtlasEntity.AtlasEntityWithExtInfo(s3Entity)).times(1);
        replay(atlasApiMock);

        Capture<String> messageCapture = newCapture();
        monitorMock.info(capture(messageCapture));
        expectLastCall().times(1);

        replay(monitorMock);

        var entry = atlasMetadataStore.findForId(testEntityName);
        assertThat(entry).isNotNull();
        assertThat(entry.getId()).isEqualTo(testEntityName);
        assertThat(entry.getCatalogEntry()).isInstanceOf(AtlasDataCatalogEntry.class);
        assertThat(entry.getCatalogEntry().getAddress()).isNotNull();
        assertAzureEntry(entry);
        assertThat(messageCapture.getValue()).contains("entities with ID " + testEntityName + " were found.");

        verify(atlasApiMock);
        verifyUnexpectedCalls(monitorMock);
    }

    @Test
    void findForId_atlasException() {
        expect(atlasApiMock.getEntityById(testEntityName)).andReturn(null);
        expect(atlasApiMock.dslSearchWithParams(anyString(), eq(100), eq(0)))
                .andThrow(new AtlasQueryException(new AtlasErrorCode("ATLAS-400-00-059", "some error message")))
                .times(2);
        replay(atlasApiMock);

        //verify that the exception is not swallowed or rethrown
        assertThatThrownBy(() -> atlasMetadataStore.findForId(testEntityName)).isInstanceOf(AtlasQueryException.class);

    }

    @Test
    void findForId_noEntityAttached() {

        String id = UUID.randomUUID().toString();
        expect(atlasApiMock.getEntityById(id)).andReturn(new AtlasEntity.AtlasEntityWithExtInfo());
        replay(atlasApiMock);
        monitorMock.info("AtlasMetadataStore: no DataEntry found for ID " + id);
        replay(monitorMock);

        assertThat(atlasMetadataStore.findForId(id)).isNull();
        verify(atlasApiMock);
        verify(monitorMock);
    }

    @Test
    void save() {
        monitorMock.severe("Save not yet implemented");
        expectLastCall().times(1);
        replay(monitorMock);
        atlasMetadataStore.save(DataEntry.Builder.newInstance().build());
        verify(monitorMock);
    }

    @Test
    void queryAll() {
        //arrange: create policy
        var policy = Policy.Builder.newInstance().id("test-policy").build();
        var policyEntity = new AtlasEntity(PolicySchema.TYPE);
        policyEntity.setAttribute("policyId", "test-policy");
        final ArrayList<Map<String, String>> relationshipEntityProps = new ArrayList<>();
        String entityGuid = UUID.randomUUID().toString();
        relationshipEntityProps.add(Map.of("guid", entityGuid));
        policyEntity.setRelationshipAttribute("itsEntity", relationshipEntityProps);

        // arrange: prepare searchresult
        var searchResult = new AtlasSearchResult();
        final AtlasEntityHeader header = new AtlasEntityHeader();
        String policyGuid = UUID.randomUUID().toString();
        header.setGuid(policyGuid);
        searchResult.addEntity(header);

        // arrange: create entity
        var azureEntity = createAzureEntity("test-azure-entity");


        expect(atlasApiMock.dslSearchWithParams("from AzureStorage where policyId = [\"test-policy\"]", 100, 0)).andReturn(searchResult);
        expect(atlasApiMock.getEntityById(policyGuid)).andReturn(new AtlasEntity.AtlasEntityWithExtInfo(azureEntity));
        expect(atlasApiMock.dslSearchWithParams("from AmazonS3 where policyId = [\"test-policy\"]", 100, 0)).andReturn(new AtlasSearchResult());
        replay(atlasApiMock);
        replay(monitorMock);

        final Collection<DataEntry> entries = atlasMetadataStore.queryAll(Collections.singleton(policy));
        assertThat(entries).isNotNull().isNotEmpty().doesNotContainNull();

        assertThat(entries).allSatisfy(this::assertAzureEntry);

        verify(atlasApiMock);
        verify(monitorMock);
    }

    @Test
    void queryAll_atlasException() {
        var policy = Policy.Builder.newInstance().id("test-policy").build();
        expect(atlasApiMock.dslSearchWithParams("from AzureStorage where policyId = [\"test-policy\"]", 100, 0))
                .andThrow(new AtlasQueryException(new AtlasErrorCode("ATLAS-400-00-059", "some error message")));
        monitorMock.severe(eq("Error during queryAll(): "), anyObject(DagxException.class));
        expectLastCall().times(1);
        replay(atlasApiMock);
        replay(monitorMock);

        assertThat(atlasMetadataStore.queryAll(Collections.singleton(policy))).isEmpty();

        verify(atlasApiMock);
        verify(monitorMock);

    }

    @Test
    void queryAll_emptyPolicies() {
        assertThat(atlasMetadataStore.queryAll(Collections.emptyList())).isEmpty();
    }

    @NotNull
    private AtlasSearchResult createSearchResult(String entityId, String type) {
        AtlasSearchResult searchResult = new AtlasSearchResult();
        final AtlasEntityHeader header = new AtlasEntityHeader(type, entityId, new HashMap<>());
        header.setStatus(AtlasEntity.Status.ACTIVE);
        searchResult.addEntity(header);
        return searchResult;
    }

    @NotNull
    private AtlasEntity createAzureEntity(String name) {
        return new AtlasEntity(AzureBlobStoreSchema.TYPE, Map.of("type", AzureBlobStoreSchema.TYPE,
                "keyName", KEY_NAME,
                "name", name,
                "container", "some-testcontainer",
                "account", "some-testaccount"));
    }

    @NotNull
    private AtlasEntity createS3Entity(String name) {
        return new AtlasEntity(S3BucketSchema.TYPE, Map.of("type", S3BucketSchema.TYPE,
                "keyName", KEY_NAME,
                "name", name,
                "bucket", "some-testbucket",
                "region", "neverland"));
    }

    private void assertAzureEntry(DataEntry entry) {
        assertThat(entry.getCatalogEntry().getAddress().getProperties()).isNotNull()
                .hasFieldOrPropertyWithValue("keyName", KEY_NAME)
                .hasFieldOrPropertyWithValue("type", AzureBlobStoreSchema.TYPE)
                .hasFieldOrPropertyWithValue("account", "some-testaccount")
                .hasFieldOrPropertyWithValue("container", "some-testcontainer");
    }
}