package org.eclipse.dataspaceconnector.assetindex.azure;

import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CosmosAssetQueryBuilderTest {

    private CosmosAssetQueryBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new CosmosAssetQueryBuilder();
    }

    @Test
    void queryAll() {
        SqlQuerySpec query = builder.from(AssetSelectorExpression.SELECT_ALL.getCriteria());

        assertThat(query.getQueryText()).isEqualTo("SELECT * FROM AssetDocument");
    }

    @Test
    void queryWithFilerOnProperty() {
        AssetSelectorExpression expression = AssetSelectorExpression.Builder.newInstance()
                .whenEquals("id", "'id-test'")
                .whenEquals("name", "'name-test'")
                .build();

        SqlQuerySpec query = builder.from(expression.getCriteria());

        assertThat(query.getQueryText()).isEqualTo("SELECT * FROM AssetDocument WHERE AssetDocument.wrappedInstance.id = @id AND AssetDocument.wrappedInstance.name = @name");
        assertThat(query.getParameters()).hasSize(2).extracting(SqlParameter::getName).containsExactlyInAnyOrder("@id", "@name");
    }

    @Test
    void queryWithFilerOnPropertyWithIllegalArgs() {
        AssetSelectorExpression expression = AssetSelectorExpression.Builder.newInstance()
                .whenEquals("test:id", "'id-test'")
                .whenEquals("test:name", "'name-test'")
                .build();

        SqlQuerySpec query = builder.from(expression.getCriteria());

        assertThat(query.getQueryText()).isEqualTo("SELECT * FROM AssetDocument WHERE AssetDocument.wrappedInstance.test_id = @test_id AND AssetDocument.wrappedInstance.test_name = @test_name");
        assertThat(query.getParameters()).hasSize(2).extracting(SqlParameter::getName).containsExactlyInAnyOrder("@test_id", "@test_name");

    }

    @Test
    void throwEdcExceptionIfCriterionOperationNotHandled() {
        AssetSelectorExpression expression = AssetSelectorExpression.Builder.newInstance()
                .whenEquals("id", "id-test")
                .constraint("name", "in", "name-test")
                .build();

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> builder.from(expression.getCriteria()))
                .withMessage("Cannot build SqlParameter for operator: in");
    }
}