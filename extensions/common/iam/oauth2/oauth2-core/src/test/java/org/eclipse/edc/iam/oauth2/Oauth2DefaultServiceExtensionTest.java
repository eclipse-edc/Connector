/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       sovity GmbH - added issuedAt leeway
 *
 */

package org.eclipse.edc.iam.oauth2;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class Oauth2DefaultServiceExtensionTest {


    @Test
    void defaultAudienceResolver(Oauth2ServiceDefaultServicesExtension extension) {
        var address = "http://address";
        var remoteMessage = mock(RemoteMessage.class);
        when(remoteMessage.getCounterPartyAddress()).thenReturn(address);
        assertThat(extension.defaultAudienceResolver().resolve(remoteMessage)).isEqualTo(address);
    }

}
