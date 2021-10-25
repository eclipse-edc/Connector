package org.eclipse.dataspaceconnector.metadata.memory;

import org.easymock.EasyMockExtension;
import org.easymock.Mock;
import org.easymock.MockType;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndexLoader;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

@ExtendWith(EasyMockExtension.class)
class InMemoryAssetIndexLoaderExtensionTest {
    private InMemoryAssetIndexLoaderExtension extension;
    @Mock()
    private ServiceExtensionContext contextMock;
    @Mock(MockType.NICE)
    private Monitor monitorMock;

    @BeforeEach
    void setup() {
        expect(contextMock.getMonitor()).andReturn(monitorMock);
    }

    @Test
    @DisplayName("Should register an InMemoryAssetLoader")
    void testGetTyped() {
        expect(contextMock.getService(AssetIndex.class, true)).andReturn(new InMemoryAssetIndex(monitorMock, new CriterionToPredicateConverter()));
        expect(contextMock.getService(DataAddressResolver.class, true)).andReturn(new InMemoryDataAddressResolver());
        contextMock.registerService(eq(AssetIndexLoader.class), isA(InMemoryAssetIndexLoader.class));
        expectLastCall();
        replay(contextMock);

        extension = new InMemoryAssetIndexLoaderExtension();
        extension.initialize(contextMock);
        verify(contextMock);
    }

    @Test
    @DisplayName("Shoule raise an exception when an AssetIndex other then the In-memory one was registered")
    void testGetTyped_wrongAssetIndexType() {
        expect(contextMock.getService(AssetIndex.class, true)).andReturn(new DummyAssetIndex());
        replay(contextMock);

        extension = new InMemoryAssetIndexLoaderExtension();
        assertThatThrownBy(() -> extension.initialize(contextMock)).isInstanceOf(ClassCastException.class);

        verify(contextMock);
    }

    @Test
    @DisplayName("Shoule raise an exception when a DataAddressResolver other then the In-memory one was registered")
    void testGetTyped_wrongDataAddressResolverType() {
        expect(contextMock.getService(AssetIndex.class, true)).andReturn(new InMemoryAssetIndex(monitorMock, new CriterionToPredicateConverter()));
        expect(contextMock.getService(DataAddressResolver.class, true)).andReturn(niceMock(DataAddressResolver.class));
        replay(contextMock);

        extension = new InMemoryAssetIndexLoaderExtension();
        assertThatThrownBy(() -> extension.initialize(contextMock)).isInstanceOf(ClassCastException.class);

        verify(contextMock);
    }

    private static class DummyAssetIndex implements AssetIndex {
        @Override
        public Stream<Asset> queryAssets(AssetSelectorExpression expression) {
            return null;
        }

        @Override
        public Asset findById(String assetId) {
            return null;
        }
    }
}
