/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Microsoft Inc.   - Refactoring
 *
 */

package org.eclipse.edc.connector.store.sql.assetindex;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;

/**
 * Defines configuration keys used by the SqlAssetIndexServiceExtension.
 */
public interface ConfigurationKeys {

    /**
     * Name of the datasource to use for accessing assets.
     */
    @Setting(required = true, value = "The Datasource used to by SqlAssetIndexService extension ")
    String DATASOURCE_SETTING_NAME = "edc.datasource.asset.name";

}
