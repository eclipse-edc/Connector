/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.asset.spi.index;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AssetIdCompatibilityTest {

    private static final String LEGACY_ID = EDC_NAMESPACE + "id";

    private final Monitor monitor = mock();
    private final AssetIdCompatibility compatibility = new AssetIdCompatibility(monitor);

    @ParameterizedTest
    @ValueSource(strings = { LEGACY_ID, "'" + LEGACY_ID + "'", "'" + LEGACY_ID })
    void isLegacyIdOperand_shouldReturnTrue(String operand) {
        assertThat(AssetIdCompatibility.isLegacyIdOperand(operand)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "id", EDC_NAMESPACE + "name", "properties.'" + LEGACY_ID + "'", "'" })
    void isLegacyIdOperand_shouldReturnFalse(String operand) {
        assertThat(AssetIdCompatibility.isLegacyIdOperand(operand)).isFalse();
    }

    @Test
    void translateCriteria_shouldReplaceLegacyOperand() {
        var other = new Criterion("name", "=", "foo");

        var result = compatibility.translate(List.of(new Criterion(LEGACY_ID, "=", "asset-id"), other));

        assertThat(result).containsExactly(new Criterion("id", "=", "asset-id"), other);
    }

    @Test
    void translateCriteria_shouldReturnSameList_whenNoLegacyOperand() {
        var criteria = List.of(new Criterion("id", "=", "asset-id"));

        assertThat(compatibility.translate(criteria)).isSameAs(criteria);
        verifyNoInteractions(monitor);
    }

    @Test
    void translateQuerySpec_shouldReplaceFilterAndSortField() {
        var querySpec = QuerySpec.Builder.newInstance()
                .offset(5)
                .limit(10)
                .sortField(LEGACY_ID)
                .sortOrder(SortOrder.DESC)
                .filter(new Criterion(LEGACY_ID, "in", List.of("a", "b")))
                .build();

        var result = compatibility.translate(querySpec);

        assertThat(result.getOffset()).isEqualTo(5);
        assertThat(result.getLimit()).isEqualTo(10);
        assertThat(result.getSortField()).isEqualTo("id");
        assertThat(result.getSortOrder()).isEqualTo(SortOrder.DESC);
        assertThat(result.getFilterExpression()).containsExactly(new Criterion("id", "in", List.of("a", "b")));
    }

    @Test
    void translateQuerySpec_shouldReturnSameQuerySpec_whenNoLegacyUsage() {
        var querySpec = QuerySpec.Builder.newInstance().sortField("id").filter(new Criterion("id", "=", "x")).build();

        assertThat(compatibility.translate(querySpec)).isSameAs(querySpec);
        verifyNoInteractions(monitor);
    }

    @Test
    void shouldWarnOnlyOnce() {
        compatibility.translate(List.of(new Criterion(LEGACY_ID, "=", "a")));
        compatibility.translate(QuerySpec.Builder.newInstance().sortField(LEGACY_ID).build());
        compatibility.warnOnce();

        verify(monitor, times(1)).warning(anyString());
    }
}
