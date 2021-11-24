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

package org.eclipse.dataspaceconnector.ids.core.message;

import static org.eclipse.dataspaceconnector.ids.spi.Protocols.IDS_REST;

/**
 * Binds and sends remote messages using the IDS REST protocol by dispatching to {@link IdsMessageSender}s.
 */
public class IdsRestRemoteMessageDispatcher extends IdsRemoteMessageDispatcher {

    @Override
    public String protocol() {
        return IDS_REST;
    }

}
