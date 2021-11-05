package org.eclipse.dataspaceconnector.metadata.memory;

import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CriterionToPredicateConverterTest {

    private CriterionToPredicateConverter converter;

    @BeforeEach
    void setUp() {
        converter = new CriterionToPredicateConverter();
    }

    @Test
    void convert() {
        var criterion = new Criterion("name", "=", "test-asset");
        var asset = createAsset("test-asset");
        var predicate = converter.convert(criterion);

        assertThat(predicate).isNotNull();
        assertThat(predicate.test(asset)).isTrue();
    }

    @Test
    void convert_selectAll() {
        var criterion = new Criterion("*", "=", "*");
        var asset = createAsset("test-asset");
        var predicate = converter.convert(criterion);

        assertThat(predicate).isNotNull();
        assertThat(predicate.test(asset)).isTrue();
    }

    @Test
    void convert_invalidOperator() {
        var criterion = new Criterion("name", "in", "(bob, alice)");
        assertThatThrownBy(() -> converter.convert(criterion)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Operator [in] is not supported by this converter!");

    }

    private Asset createAsset(String name) {
        return Asset.Builder.newInstance()
                .name(name)
                .id(UUID.randomUUID().toString())
                .build();
    }
}