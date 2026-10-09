/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.services.spi.callback;

import org.eclipse.edc.controlplane.CallbackAddress;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

import java.util.List;

/**
 * Resolves the callbacks configured by a participant context, which are invoked for every event that carries the id of
 * that participant context.
 */
@ExtensionPoint
@FunctionalInterface
public interface ParticipantCallbackResolver {

    /**
     * Returns the callbacks of a participant context associated with the input event.
     *
     * @param participantContextId the participant context id.
     * @param eventName the event name.
     * @return the list of callbacks, empty if none.
     */
    List<CallbackAddress> resolve(String participantContextId, String eventName);
}
