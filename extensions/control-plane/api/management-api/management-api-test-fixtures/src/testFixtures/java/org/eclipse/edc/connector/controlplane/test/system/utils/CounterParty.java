/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.controlplane.test.system.utils;

/**
 * Represents a counter-party in a system test.
 *
 * @param participantId the participant identifier
 * @param protocolUrl   the protocol URL of the counter-party
 */
public record CounterParty(String participantId, String protocolUrl) {
}