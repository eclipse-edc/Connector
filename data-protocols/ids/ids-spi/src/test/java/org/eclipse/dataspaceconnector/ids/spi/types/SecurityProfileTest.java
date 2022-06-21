/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.spi.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityProfileTest {

    @Test
    void fromValue() {
        assertThrows(IllegalArgumentException.class, () -> SecurityProfile.fromValue("test"));
        assertEquals(SecurityProfile.BASE_SECURITY_PROFILE, SecurityProfile.fromValue("base"));
        assertEquals(SecurityProfile.TRUST_SECURITY_PROFILE, SecurityProfile.fromValue("trust"));
        assertEquals(SecurityProfile.TRUST_PLUS_SECURITY_PROFILE, SecurityProfile.fromValue("trust-plus"));
    }
}
