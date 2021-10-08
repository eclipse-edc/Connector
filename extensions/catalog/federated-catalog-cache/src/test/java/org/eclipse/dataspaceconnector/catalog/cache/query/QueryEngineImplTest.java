package org.eclipse.dataspaceconnector.catalog.cache.query;

import org.eclipse.dataspaceconnector.catalog.spi.QueryAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.QueryAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.spi.QueryEngine;
import org.eclipse.dataspaceconnector.catalog.spi.model.CacheQuery;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
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
        QueryAdapterRegistry registry = strictMock(QueryAdapterRegistry.class);

        QueryAdapter adapter1 = strictMock(QueryAdapter.class);
        QueryAdapter adapter2 = strictMock(QueryAdapter.class);

        expect(adapter1.executeQuery(anyObject())).andReturn(Stream.of(ASSET_ABC));
        expect(adapter2.executeQuery(anyObject())).andReturn(Stream.of(ASSET_DEF, ASSET_XYZ));
        expect(registry.getAllAdapters()).andReturn(Arrays.asList(adapter1, adapter2));

        replay(adapter1, adapter2, registry);

        QueryEngine queryEngine = new QueryEngineImpl(registry);

        assertThat(queryEngine.getCatalog(new CacheQuery())).isEqualTo(Arrays.asList(ASSET_ABC, ASSET_DEF, ASSET_XYZ));

        verify(adapter1, adapter2, registry);
    }
}