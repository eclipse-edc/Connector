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

package org.eclipse.edc.connector.store.azure.cosmos.assetindex;

import com.azure.cosmos.models.SqlParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.edc.spi.query.Criterion.criterion;

class CosmosAssetQueryBuilderTest {

    private CosmosAssetQueryBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new CosmosAssetQueryBuilder();
    }

    @Test
    void queryAll() {
        var query = builder.from(Collections.emptyList());

        assertThat(query.getQueryText()).isEqualTo("SELECT * FROM AssetDocument");
    }

    @Test
    void queryWithFilerOnProperty() {
        var criteria = List.of(
                criterion("id", "=", "'id-test'"),
                criterion("name", "=", "'name-test'")
        );

        var query = builder.from(criteria);

        assertThat(query.getQueryText()).isEqualTo("SELECT * FROM AssetDocument WHERE AssetDocument.wrappedInstance.id = @id AND AssetDocument.wrappedInstance.name = @name");
        assertThat(query.getParameters()).hasSize(2).extracting(SqlParameter::getName).containsExactlyInAnyOrder("@id", "@name");
    }

    @Test
    void queryWithFilerOnPropertyWithIllegalArgs() {
        var criteria = List.of(
                criterion("test:id", "=", "'id-test'"),
                criterion("test:name", "=", "'name-test'")
        );

        var query = builder.from(criteria);

        assertThat(query.getQueryText()).isEqualTo("SELECT * FROM AssetDocument WHERE AssetDocument.wrappedInstance.test_id = @test_id AND AssetDocument.wrappedInstance.test_name = @test_name");
        assertThat(query.getParameters()).hasSize(2).extracting(SqlParameter::getName).containsExactlyInAnyOrder("@test_id", "@test_name");

    }

    @Test
    void throwEdcExceptionIfCriterionOperationNotHandled() {
        var criteria = List.of(
                criterion("id", "=", "'id-test'"),
                criterion("name", "in", "name-test")
        );

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> builder.from(criteria))
                .withMessage("Cannot build WHERE clause, reason: The \"in\" operator requires the right-hand operand to be of type interface java.lang.Iterable but was actually class java.lang.String");
    }
}
