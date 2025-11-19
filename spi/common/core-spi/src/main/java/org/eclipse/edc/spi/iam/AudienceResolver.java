/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.spi.iam;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.message.ProtocolRemoteMessage;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

/**
 * An interface for resolving the aud claim from a {@link RemoteMessage}. The resolver is used on protocol layer
 * before calling the {@link IdentityService} for obtaining the token. Implementors of {@link IdentityService} might
 * use different value for the aud claim, and they should provide a default resolution strategy which specific use case
 * can override.
 */
@FunctionalInterface
@ExtensionPoint
public interface AudienceResolver {

    Result<String> resolve(ProtocolRemoteMessage remoteMessage);

}
