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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.catalog;

import org.eclipse.edc.api.model.CriterionDto;

public class TestFunctions {
    public static CriterionDto createCriterionDto(String left, String op, Object right) {
        return CriterionDto.Builder.newInstance().operandLeft(left).operator(op).operandRight(right).build();
    }

}
