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

package org.eclipse.edc.controlplane.tasks;


import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Represents the payload of a {@link Task}. Implementations of this interface should be immutable and serializable to JSON.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
public interface TaskPayload {
    String name();

    String group();
}
