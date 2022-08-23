/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.core.jwt;

import org.eclipse.dataspaceconnector.core.jwt.JwtDecoratorRegistryImpl;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.Oauth2JwtDecoratorRegistry;

/**
 * Registry for Oauth2 JWT decorators.
 */
public class Oauth2JwtDecoratorRegistryRegistryImpl extends JwtDecoratorRegistryImpl implements Oauth2JwtDecoratorRegistry {
}
