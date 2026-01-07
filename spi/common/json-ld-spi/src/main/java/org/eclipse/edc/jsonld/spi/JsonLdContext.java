/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.jsonld.spi;

import java.net.URI;

/**
 * Represent a json-ld context file in resources, associated to a context url.
 *
 * @param resource the resource uri where the cached
 * @param url the context url.
 */
public record JsonLdContext(URI resource, String url) {
}
