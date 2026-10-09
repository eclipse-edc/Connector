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

package org.eclipse.edc.connector.controlplane.contract.spi.types.command;

import org.eclipse.edc.spi.command.EntityCommand;

/**
 * Rejects a provider contract negotiation that has been held for manual approval.
 */
public class RejectNegotiationCommand extends EntityCommand {

    public RejectNegotiationCommand(String negotiationId) {
        super(negotiationId);
    }
}
