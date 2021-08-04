/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.spi;

import org.eclipse.dataspaceconnector.spi.EdcSetting;

public interface IdsConfiguration {

    /**
     * The unique id of the current connector.
     */
    @EdcSetting
    String CONNECTOR_NAME = "dataspaceconnector.ids.connector.name";

}
