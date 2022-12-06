/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - add more values
 *
 */

package org.eclipse.edc.protocol.ids.spi.domain;

/**
 * Common IDS values.
 */
public final class IdsConstants {

    /**
     * Context information for de/serialization.
     */
    public static final String CONTEXT = "https://w3id.org/idsa/contexts/context.jsonld";

    /**
     * Webhook for IDS multipart messages.
     */
    public static final String IDS_WEBHOOK_ADDRESS_PROPERTY = "idsWebhookAddress";

    /**
     * Version of the used IDS information model library.
     */
    public static final String INFORMATION_MODEL_VERSION = "4.1.3";
}
