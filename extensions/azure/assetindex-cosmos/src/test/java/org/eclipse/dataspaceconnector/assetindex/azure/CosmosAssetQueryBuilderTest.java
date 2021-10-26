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
    void queryWithConditions() {
        AssetSelectorExpression expression = AssetSelectorExpression.Builder.newInstance()
                .whenEquals("id", "id-test")
                .whenEquals("name", "name-test")
                .build();

        SqlQuerySpec query = builder.from(expression);

        assertThat(query.getQueryText()).isEqualTo("SELECT * FROM AssetDocument WHERE AssetDocument.wrappedInstance.properties.id = id-test AND AssetDocument.wrappedInstance.properties.name = name-test");
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