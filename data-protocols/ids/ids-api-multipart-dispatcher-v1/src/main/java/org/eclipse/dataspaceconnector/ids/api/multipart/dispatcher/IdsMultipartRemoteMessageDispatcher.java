/*
 *  Copyright (c) 2020, 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher;

import org.eclipse.dataspaceconnector.ids.core.message.IdsRemoteMessageDispatcher;

import static org.eclipse.dataspaceconnector.ids.spi.Protocols.IDS_MULTIPART;

/**
 * IdsRemoteMessageDisptacher implementation for IDS multipart.
 */
public class IdsMultipartRemoteMessageDispatcher extends IdsRemoteMessageDispatcher {

    @Override
    public String protocol() {
        return IDS_MULTIPART;
    }

}
