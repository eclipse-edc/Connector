/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.edc.participant.spi;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Implementations register with the {@link ParticipantAgentService} and will be invoked when instantiating a {@link ParticipantAgent} so that they can add attributes to the agent.
 *
 * For example, custom logic may be invoked to determine if a participant (based on its claim token) is a special category by calling an external system. This category may then
 * be added as an attribute.
 */
public interface ParticipantAgentServiceExtension {

    /**
     * Returns attributes that should be added to the {@link ParticipantAgent} or an empty map.
     */
    @NotNull
    Map<String, String> attributesFor(ClaimToken token);

}
