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

package org.eclipse.edc.identitytrust.model;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class represents a proof/signature object defined in the <a href="https://www.w3.org/TR/vc-data-model/#proofs-signatures">W3 Verifiable Credential Datamodel</a>
 * <p>
 * The following logic applies:
 * <ul>
 * <li>When the {@link Proof#verificationMethod} is a {@link URI} (external proof), then the {@link Proof#proofContents} <br/>
 * must contain a key-value pair representing the proof value, for example:
 *   <pre>
 *    "proof": {
 *       "type": "JsonWebSignature2020",
 *       "created": "2022-12-31T23:00:00Z",
 *       "proofPurpose": "assertionMethod",
 *       "verificationMethod": "https://org.eclipse.edc/issuers/42#key-7",
 *       "jws": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFUzM4NCJ9..dbi6LFkdeBeCz3sHaxRRFVJC2_rF8Z_oYqaoNOpYtzQh61WP78pK7nKT53WsE-7uiBUMamLA8vEGJpFQ3h4MXDi2OKh1YDpphS_pwyDkqYbsguMs2KYqPxe8t1OC2G1o"
 *     }
 *   </pre>
 * </li>
 * <li>When the {@link Proof#verificationMethod} is an object (embedded proof), then it must contain a {@code type} field indicating
 *   the cryptographic primitives used. For example:
 *   <pre>
 *    "proof": {
 *     "type": "JsonWebSignature2020",
 *     "created": "2022-12-31T23:00:00Z",
 *     "proofPurpose": "assertionMethod",
 *     "verificationMethod": {
 *       "type": "JsonWebKey2020",
 *       "publicKeyJwk": {
 *         "kty": "EC",
 *         "crv": "P-384",
 *         "x": "eQbMauiHc9HuiqXT894gW5XTCrOpeY8cjLXAckfRtdVBLzVHKaiXAAxBFeVrSB75",
 *         "y": "YOjxhMkdH9QnNmGCGuGXJrjAtk8CQ1kTmEEi9cg2R9ge-zh8SFT1Xu6awoUjK5Bv"
 *       },
 *       "id": "https://org.eclipse.edc/keys/68c7189c-b849-4f85-b27d-c796c7cf29ed"
 *     },
 *     "jws": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFUzM4NCJ9..0ueANOomarONwEL2Y0QnCFjgOdgPjI8kL2Wk4QWh8SJjvVTR80ASVh7bi8HlQp6dUigP3r509oMQkXB6TEddi0D8oQc2Lv0uWxl7yxPInBcfIsWmQrFBTb4mCSU_MJwE"
 *   }
 *   </pre>
 *   </li>
 * </ul>
 * The verification method <strong>cannot</strong> be anything other than URI or object.
 * Going by the aforementioned examples, {@code jws} and {@code proofValue} would be contained in {@link Proof#proofContents}
 */
public class Proof {
    private Map<String, Object> proofContents = new HashMap<>();
    private String type;
    private Date created;
    private String proofPurpose;
    private Object verificationMethod;

    private Proof() {
    }

    public Map<String, Object> getProofContents() {
        return proofContents;
    }

    public String getType() {
        return type;
    }

    public Date getCreated() {
        return created;
    }

    public String getProofPurpose() {
        return proofPurpose;
    }

    public Object getVerificationMethod() {
        return verificationMethod;
    }

    public static final class Builder {
        private final Proof instance;

        private Builder() {
            instance = new Proof();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder proofContents(Map<String, Object> proofContent) {
            this.instance.proofContents = proofContent;
            return this;
        }

        public Builder proofContent(String key, Object value) {
            this.instance.proofContents.put(key, value);
            return this;
        }

        public Builder type(String type) {
            this.instance.type = type;
            return this;
        }

        public Builder created(Date created) {
            this.instance.created = created;
            return this;
        }

        public Builder proofPurpose(String proofPurpose) {
            this.instance.proofPurpose = proofPurpose;
            return this;
        }

        public Builder verificationMethod(Object verificationMethod) {
            this.instance.verificationMethod = verificationMethod;
            return this;
        }

        public Proof build() {
            Objects.requireNonNull(instance.type, "Proof must contain a 'type' property.");
            Objects.requireNonNull(instance.created, "Proof must contain a 'created' property.");
            Objects.requireNonNull(instance.verificationMethod, "Proof must contain `verificationMethod` property.");
            Objects.requireNonNull(instance.proofPurpose, "Proof must contain `proofPurpose` property.");

            if (!(instance.verificationMethod instanceof Map) && !(instance.verificationMethod instanceof URI)) {
                throw new IllegalArgumentException("Proof must have a `verificationMethod` that is either a URI (LD-proof) or a Map (embedded proof).");
            }

            Objects.requireNonNull(instance.proofContents, "External proofs require additional proof content");
            if (instance.proofContents.isEmpty()) {
                throw new IllegalArgumentException("External proofs require additional proof content");
            }
            return instance;
        }
    }
}
