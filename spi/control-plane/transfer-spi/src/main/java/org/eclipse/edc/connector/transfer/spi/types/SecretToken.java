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

package org.eclipse.edc.connector.transfer.spi.types;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.edc.spi.types.domain.Polymorphic;

/**
 * A temporary token with write privileges to the data destination.
 */
@JsonTypeName("dataspaceconnector:secrettoken")
public interface SecretToken extends Polymorphic {

    long getExpiration();

}
