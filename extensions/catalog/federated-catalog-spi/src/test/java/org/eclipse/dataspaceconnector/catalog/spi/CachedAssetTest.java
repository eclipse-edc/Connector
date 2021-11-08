package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CachedAssetTest {


    @BeforeEach
    void setUp() {
    }

    @Test
    void getOriginator() {
        var ca = CachedAsset.Builder.newInstance()
                .asset(createAsset())
                .originator("test-originator")
                .build();

        assertThat(ca.getOriginator()).isEqualTo("test-originator");
    }

    @Test
    void getOriginator_notPresent() {
        var ca = CachedAsset.Builder.newInstance()
                .asset(createAsset())
                .build();

        assertThat(ca.getOriginator()).isNull();
    }

    @Test
    void getPolicy() {

        Policy policy = Policy.Builder.newInstance().build();
        var ca = CachedAsset.Builder.newInstance()
                .asset(createAsset())
                .policy(policy)
                .build();

        assertThat(ca.getPolicy()).isEqualTo(policy);
    }

    @Test
    void getPolicy_notPresent() {

        var ca = CachedAsset.Builder.newInstance()
                .asset(createAsset())
                .build();

        assertThat(ca.getPolicy()).isNull();
    }

    @NotNull
    private Asset createAsset() {
        return Asset.Builder.newInstance().id("asset1").name("test-asset").build();
    }
}