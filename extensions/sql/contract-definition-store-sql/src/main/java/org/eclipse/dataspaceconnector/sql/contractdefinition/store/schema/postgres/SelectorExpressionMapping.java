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

package org.eclipse.dataspaceconnector.sql.contractdefinition.store.schema.postgres;

import org.eclipse.dataspaceconnector.sql.translation.TranslationMapping;

/**
 * Maps fields of a {@link org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression} onto the corresponding SQL
 * schema (= column names)
 */
class SelectorExpressionMapping extends TranslationMapping {

    SelectorExpressionMapping() {
        add("criteria", new CriterionMapping());
    }


}
