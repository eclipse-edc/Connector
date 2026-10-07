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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - added test
 *
 */

package org.eclipse.edc.policy.model;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class DutyTest {

    @Test
    void serializeDeserialize() throws JacksonException {
        var mapper = new ObjectMapper();
        var serialized = mapper.writeValueAsString(Duty.Builder.newInstance().action(Action.Builder.newInstance().type("use").build()).build());
        assertThat(mapper.readValue(serialized, Duty.class).getAction()).isNotNull();
    }

}
