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

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

/**
 * A function that describes the default behaviour for extracting a participant's id from a
 * corresponding ClaimToken.
 */
@FunctionalInterface
@ExtensionPoint
public interface DefaultParticipantIdExtractionFunction extends ParticipantIdExtractionFunction {
}
