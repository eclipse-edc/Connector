/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.sql.lease;

public interface StatefulEntityStatements {

    String getIdColumn();

    default String getStateColumn() {
        return "state";
    }

    default String getStateTimestampColumn() {
        return "state_time_stamp";
    }

    default String getStateCountColumn() {
        return "state_count";
    }

    default String getTraceContextColumn() {
        return "trace_context";
    }

    default String getErrorDetailColumn() {
        return "error_detail";
    }

    default String getCreatedAtColumn() {
        return "created_at";
    }

    default String getUpdatedAtColumn() {
        return "updated_at";
    }

    default String getLeasedByColumn() {
        return "leased_by";
    }

    default String getLeasedAtColumn() {
        return "leased_at";
    }

    default String getLeaseDurationColumn() {
        return "lease_duration";
    }

    /**
     * Filter matching the entities that are not leased or whose lease is expired. Parameters: now.
     *
     * @param table the entity table.
     * @return the filter.
     */
    default String getNotLeasedFilter(String table) {
        return "(%1$s.%2$s IS NULL OR (%1$s.%2$s + %1$s.%3$s) < ?)".formatted(table, getLeasedAtColumn(), getLeaseDurationColumn());
    }

    /**
     * Filter matching the entities that are not leased, are leased by the given holder or whose lease is expired.
     * Parameters: leased by, now.
     *
     * @param table the entity table.
     * @return the filter.
     */
    default String getLeaseOwnedOrFreeFilter(String table) {
        return "(%1$s.%2$s IS NULL OR %1$s.%2$s = ? OR (%1$s.%3$s + %1$s.%4$s) < ?)"
                .formatted(table, getLeasedByColumn(), getLeasedAtColumn(), getLeaseDurationColumn());
    }

    /**
     * Template that counts the entities with the given id that are leased by another holder and whose lease is not
     * expired. Parameters: entity id, leased by, now.
     *
     * @param table the entity table.
     * @return the template.
     */
    default String getCountLeasedByOtherTemplate(String table) {
        return "SELECT COUNT(*) FROM %1$s WHERE %1$s.%2$s = ? AND %1$s.%3$s IS NOT NULL AND %1$s.%3$s <> ? AND (%1$s.%4$s + %1$s.%5$s) >= ?"
                .formatted(table, getIdColumn(), getLeasedByColumn(), getLeasedAtColumn(), getLeaseDurationColumn());
    }

    /**
     * Template for acquiring the lease of an entity, it succeeds only if the entity is not leased or the lease is expired.
     * Parameters: leased by, leased at, lease duration, entity id, now.
     *
     * @param table the entity table.
     * @return the template.
     */
    default String getAcquireLeaseTemplate(String table) {
        return "UPDATE %s SET %s = ?, %s = ?, %s = ? WHERE %s = ? AND %s".formatted(table, getLeasedByColumn(), getLeasedAtColumn(),
                getLeaseDurationColumn(), getIdColumn(), getNotLeasedFilter(table));
    }

    /**
     * Template for breaking the lease of an entity, it succeeds only if the entity is not leased, is leased by the given
     * holder or the lease is expired. Parameters: entity id, leased by, now.
     *
     * @param table the entity table.
     * @return the template.
     */
    default String getBreakLeaseTemplate(String table) {
        return "UPDATE %s SET %s = NULL, %s = NULL, %s = NULL WHERE %s = ? AND %s".formatted(table, getLeasedByColumn(), getLeasedAtColumn(),
                getLeaseDurationColumn(), getIdColumn(), getLeaseOwnedOrFreeFilter(table));
    }
}
