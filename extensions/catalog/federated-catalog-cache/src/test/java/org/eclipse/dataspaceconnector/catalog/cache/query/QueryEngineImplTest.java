package org.eclipse.dataspaceconnector.catalog.cache.query;

import org.eclipse.dataspaceconnector.catalog.spi.CacheQueryAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.spi.QueryEngine;
import org.eclipse.dataspaceconnector.catalog.spi.QueryResponse;
import org.eclipse.dataspaceconnector.catalog.spi.model.CacheQuery;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.verify;

class QueryEngineImplTest {

    private static final Asset ASSET_ABC = Asset.Builder.newInstance().id("ABC").build();
    private static final Asset ASSET_DEF = Asset.Builder.newInstance().id("DEF").build();
    private static final Asset ASSET_XYZ = Asset.Builder.newInstance().id("XYZ").build();

    @Test
    void getCatalog() {
        CacheQueryAdapterRegistry registry = strictMock(CacheQueryAdapterRegistry.class);

        expect(registry.executeQuery(anyObject())).andReturn(QueryResponse.ok(Stream.of(ASSET_ABC, ASSET_DEF, ASSET_XYZ)));

        replay(registry);

        QueryEngine queryEngine = new QueryEngineImpl(registry);

        QueryResponse catalog = queryEngine.getCatalog(CacheQuery.Builder.newInstance().build());
        assertThat(catalog.getStatus()).isEqualTo(QueryResponse.Status.ACCEPTED);
        assertThat(catalog.getErrors()).isEmpty();
        assertThat(catalog.getAssets()).containsExactlyInAnyOrder(ASSET_ABC, ASSET_DEF, ASSET_XYZ);

        verify(registry);
    }

    @Test
    void getCatalog_withErrors() {
        CacheQueryAdapterRegistry registry = strictMock(CacheQueryAdapterRegistry.class);

        expect(registry.executeQuery(anyObject())).andReturn(QueryResponse.Builder.newInstance()
                .assets(Stream.of(ASSET_ABC, ASSET_DEF, ASSET_XYZ))
                .error("some error")
                .build());

        replay(registry);

        QueryEngine queryEngine = new QueryEngineImpl(registry);

        QueryResponse catalog = queryEngine.getCatalog(CacheQuery.Builder.newInstance().build());
        assertThat(catalog.getStatus()).isEqualTo(QueryResponse.Status.ACCEPTED);
        assertThat(catalog.getErrors()).hasSize(1).containsExactly("some error");
        assertThat(catalog.getAssets()).containsExactlyInAnyOrder(ASSET_ABC, ASSET_DEF, ASSET_XYZ);

        verify(registry);
    }

    @Test
    void getCatalog_notAccepted() {
        CacheQueryAdapterRegistry registry = strictMock(CacheQueryAdapterRegistry.class);

        expect(registry.executeQuery(anyObject())).andReturn(QueryResponse.Builder.newInstance()
                .status(QueryResponse.Status.NO_ADAPTER_FOUND)
                .error("no adapter was found for that query")
                .build());

        replay(registry);

        QueryEngine queryEngine = new QueryEngineImpl(registry);

        QueryResponse catalog = queryEngine.getCatalog(CacheQuery.Builder.newInstance().build());
        assertThat(catalog.getStatus()).isEqualTo(QueryResponse.Status.NO_ADAPTER_FOUND);
        assertThat(catalog.getErrors()).hasSize(1);
        assertThat(catalog.getAssets()).isEmpty();

        verify(registry);
    }
}