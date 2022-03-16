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
 *
 */

package org.eclipse.dataspaceconnector.sql.contractdefinition.store;

import org.eclipse.dataspaceconnector.spi.EdcSetting;

/**
 * Defines configuration keys used by the SqlContractDefinitionStoreServiceExtension.
 */
public interface ConfigurationKeys {

    /**
     * Name of the datasource to use for accessing contract definitions.
     */
    @EdcSetting(required = true)
    String DATASOURCE_SETTING_NAME = "edc.datasource.contractdefinition.name";

}
