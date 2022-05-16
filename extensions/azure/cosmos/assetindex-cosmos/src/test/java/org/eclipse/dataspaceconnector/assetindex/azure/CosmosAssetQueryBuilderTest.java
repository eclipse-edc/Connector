/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

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
                .withMessage("Cannot build WHERE clause, reason: The \"in\" operator requires the right-hand operand to be of type interface java.lang.Iterable but was actually class java.lang.String");
    }
}