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
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.handler;

import de.fraunhofer.iais.eis.NotificationMessage;

/**
 * {@link NotificationMessage} has several implementations. This registry is used to hold the {{@link Handler}
 * associated with each implementation.
 */
public class NotificationMessageHandlerRegistry extends HandlerRegistry<NotificationMessage> {
}
