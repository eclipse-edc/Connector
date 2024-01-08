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

package org.eclipse.edc.iam.oauth2.spi;

import org.eclipse.edc.jwt.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

/**
 * Registry for Oauth2 validation rules.
 *
 * @deprecated This specialization will be removed issue https://github.com/eclipse-edc/Connector/issues/3744 and get replaced by a security context
 */
@Deprecated
@ExtensionPoint
public interface Oauth2ValidationRulesRegistry extends TokenValidationRulesRegistry {
}
