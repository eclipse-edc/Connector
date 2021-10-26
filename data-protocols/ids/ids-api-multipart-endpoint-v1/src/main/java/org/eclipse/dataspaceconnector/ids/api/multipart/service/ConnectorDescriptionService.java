/*
 *  Copyright (c) 2021 Daimler TSS GmbH
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

package org.eclipse.dataspaceconnector.ids.api.multipart.service;

import de.fraunhofer.iais.eis.Connector;
import org.jetbrains.annotations.NotNull;

/**
 * The IDS service is able to create IDS compliant descriptions of resources.
 * These descriptions may be used to create a self-description or answer a Description Request Message.
 */
public interface ConnectorDescriptionService {

    /**
     * Provides the connector object, which may be used by the IDS self-description of the connector.
     *
     * @return connector description
     */
    @NotNull
    Connector createSelfDescription();
}
