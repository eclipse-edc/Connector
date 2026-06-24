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

package org.eclipse.edc.policy.cel.function.context;

/**
 * Represents a claim to be used in CEL context mapping, consisting of a name and a value.
 *
 * @param name  the name of the claim
 * @param value the value of the claim
 */
public record CelClaim(String name, Object value) {

}
