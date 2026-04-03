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

package org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model;

import java.util.List;

/**
 * DTO representation of a QuerySpec.
 */
public final class QuerySpectDto extends Typed {
    private final List<CriterionDto> filterExpression;

    public QuerySpectDto(List<CriterionDto> filterExpression) {
        super("QuerySpec");
        this.filterExpression = filterExpression;
    }


    public List<CriterionDto> filterExpression() {
        return filterExpression;
    }

}
