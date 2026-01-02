/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.selector.spi.manager;

import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.spi.response.StatusResult;

/**
 * Check data plane availability
 */
public interface DataPlaneAvailabilityChecker {

    /**
     * Check availability of the passed data plane instance
     *
     * @param dataPlane the data plane
     * @return success if data plane is available, failure otherwise
     */
    StatusResult<Void> checkAvailability(DataPlaneInstance dataPlane);

}
