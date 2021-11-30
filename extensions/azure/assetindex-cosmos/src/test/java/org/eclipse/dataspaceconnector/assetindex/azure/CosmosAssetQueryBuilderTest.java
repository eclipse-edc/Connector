package org.eclipse.dataspaceconnector.assetindex.azure;

import com.azure.cosmos.models.SqlQuerySpec;
import org.eclipse.dataspaceconnector.spi.EdcException;
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
        SqlQuerySpec query = builder.from(AssetSelectorExpression.SELECT_ALL);

        assertThat(query.getQueryText()).isEqualTo("SELECT * FROM AssetDocument");
    }

    @Test
    void queryWithFilerOnProperty() {
        AssetSelectorExpression expression = AssetSelectorExpression.Builder.newInstance()
                .whenEquals("id", "'id-test'")
                .whenEquals("name", "'name-test'")
                .build();

        SqlQuerySpec query = builder.from(expression);

        assertThat(query.getQueryText()).isEqualTo("SELECT * FROM AssetDocument WHERE AssetDocument.wrappedInstance.id = 'id-test' AND AssetDocument.wrappedInstance.name = 'name-test'");
    }

    @Test
    void queryWithFilerOnPropertyWithIllegalArgs() {
        AssetSelectorExpression expression = AssetSelectorExpression.Builder.newInstance()
                .whenEquals("test:id", "'id-test'")
                .whenEquals("test:name", "'name-test'")
                .build();

        SqlQuerySpec query = builder.from(expression);

        assertThat(query.getQueryText()).isEqualTo("SELECT * FROM AssetDocument WHERE AssetDocument.wrappedInstance.test_id = 'id-test' AND AssetDocument.wrappedInstance.test_name = 'name-test'");
    }

    @Test
    void throwEdcExceptionIfCriterionOperationNotHandled() {
        AssetSelectorExpression expression = AssetSelectorExpression.Builder.newInstance()
                .whenEquals("id", "id-test")
                .constraint("name", "in", "name-test")
                .build();

        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> builder.from(expression))
                .withMessage("Cannot build SqlParameter for operator: in");
    }
}