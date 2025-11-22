/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.connector.controlplane.transfer.spi.types;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.edc.spi.types.domain.Polymorphic;

/**
 * A temporary token with write privileges to the data destination.
 *
 * @deprecated provisioning will be fully managed by the data-plane
 */
@Deprecated(since = "0.14.1")
@JsonTypeName("dataspaceconnector:secrettoken")
public interface SecretToken extends Polymorphic {

    long getExpiration();

}
