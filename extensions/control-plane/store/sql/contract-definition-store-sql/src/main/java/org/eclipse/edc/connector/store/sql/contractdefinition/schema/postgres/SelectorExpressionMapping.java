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

package org.eclipse.edc.connector.store.sql.contractdefinition.schema.postgres;

import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.sql.translation.TranslationMapping;

/**
 * Maps fields of a {@link AssetSelectorExpression} onto the corresponding SQL
 * schema (= column names)
 */
class SelectorExpressionMapping extends TranslationMapping {

    SelectorExpressionMapping() {
        add("criteria", new CriterionMapping());
    }


}
