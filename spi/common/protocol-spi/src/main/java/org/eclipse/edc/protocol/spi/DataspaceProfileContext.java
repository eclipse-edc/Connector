/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.spi;

/**
 * Represent a Dataspace Profile Context
 *
 * @param name the name.
 * @param protocolVersion the protocol version associated.
 * @param webhook the protocol endpoint url.
 */
public record DataspaceProfileContext(String name, ProtocolVersion protocolVersion, ProtocolWebhook webhook, String participantId) {

}
