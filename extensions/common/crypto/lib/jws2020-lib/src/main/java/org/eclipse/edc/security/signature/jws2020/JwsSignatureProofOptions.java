/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.security.signature.jws2020;

import com.apicatalog.vc.integrity.DataIntegrityProofOptions;

/**
 * Proof options for Jws2020
 */
public class JwsSignatureProofOptions extends DataIntegrityProofOptions {
    /**
     * Create a new proof options instance
     *
     * @param suite The {@link JwsSignature2020Suite} for which the options are created.
     */
    public JwsSignatureProofOptions(JwsSignature2020Suite suite) {
        super(suite);
    }
}
