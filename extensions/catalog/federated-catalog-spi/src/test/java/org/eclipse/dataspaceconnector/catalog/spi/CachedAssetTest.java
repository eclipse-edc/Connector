package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.policy.model.Policy;
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
                .id("testAsset1")
                .name("test-asset")
                .originator("test-originator")
                .build();

        assertThat(ca.getOriginator()).isEqualTo("test-originator");
    }

    @Test
    void getOriginator_notPresent() {
        var ca = CachedAsset.Builder.newInstance()
                .id("testAsset1")
                .name("test-asset")
                .build();

        assertThat(ca.getOriginator()).isNull();
    }

    @Test
    void getPolicy() {

        Policy policy = Policy.Builder.newInstance().build();
        var ca = CachedAsset.Builder.newInstance()
                .policy(policy)
                .id("testAsset1")
                .name("test-asset")
                .build();

        assertThat(ca.getPolicy()).isEqualTo(policy);
    }

    @Test
    void getPolicy_notPresent() {

        var ca = CachedAsset.Builder.newInstance()
                .id("testAsset1")
                .name("test-asset")
                .build();

        assertThat(ca.getPolicy()).isNull();
    }

}