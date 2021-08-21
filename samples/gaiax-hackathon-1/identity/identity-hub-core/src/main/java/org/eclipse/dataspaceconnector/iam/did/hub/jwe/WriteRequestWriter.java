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
 *
 */
package org.eclipse.dataspaceconnector.iam.did.hub.jwe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.RSASSASigner;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.message.Commit;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.message.CommitHeader;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.message.InterfaceType;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.message.JsonCommitObject;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.message.WriteRequest;
import org.eclipse.dataspaceconnector.spi.EdcException;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.nimbusds.jose.EncryptionMethod.A256GCM;
import static com.nimbusds.jose.JWEAlgorithm.RSA_OAEP_256;

/**
 * Creates a JWE contining a commit {@link WriteRequest}.
 */
public class WriteRequestWriter extends AbstractJweWriter<WriteRequestWriter> {

    private String kid;
    private String sub;
    private InterfaceType interfaze = InterfaceType.Collections;
    private String context;
    private String type;
    private Commit.Operation operation = Commit.Operation.create;
    private Object commitObject;

    public String buildJwe() {
        Objects.requireNonNull(context);
        Objects.requireNonNull(type);
        Objects.requireNonNull(kid);
        Objects.requireNonNull(sub);
        Objects.requireNonNull(commitObject);
        try {
            JWSHeader jwsCommitHeader = createHeader();

            var commitBody = objectMapper.writeValueAsString(commitObject);
            var jwsCommit = new JWSObject(jwsCommitHeader, new Payload(commitBody));
            jwsCommit.sign(new RSASSASigner(privateKey));

            var serializedCommit = jwsCommit.serialize();

            // convert to JWT JSON Serialization format - .cf https://tools.ietf.org/html/rfc7515#section-3.1
            var commitObject = new JsonCommitObject(serializedCommit, new CommitHeader(kid)); // use the kid as the issuer

            var writeRequest = WriteRequest.Builder.newInstance().commit(commitObject).aud(sub).iss(kid).sub(sub).build();

            var payload = new Payload(objectMapper.writeValueAsString(writeRequest));

            var jweHeader = new JWEHeader.Builder(RSA_OAEP_256, A256GCM).keyID(kid).build();
            var jweObject = new JWEObject(jweHeader, payload);
            jweObject.encrypt(new RSAEncrypter(publicKey));
            return jweObject.serialize();
        } catch (JOSEException | JsonProcessingException e) {
            throw new EdcException(e);
        }
    }

    public WriteRequestWriter interfaceType(InterfaceType interfaze) {
        this.interfaze = interfaze;
        return this;
    }

    public WriteRequestWriter context(String context) {
        this.context = context;
        return this;
    }

    public WriteRequestWriter type(String type) {
        this.type = type;
        return this;
    }

    public WriteRequestWriter kid(String kid) {
        this.kid = kid;
        return this;
    }

    public WriteRequestWriter sub(String sub) {
        this.sub = sub;
        return this;
    }

    public WriteRequestWriter commitObject(Object commitObject) {
        this.commitObject = commitObject;
        return this;
    }

    private JWSHeader createHeader() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("context", context);
        parameters.put("type", type);
        parameters.put("interface", interfaze);
        parameters.put("operation", operation.toString());
        parameters.put("committed_at", ZonedDateTime.now(ZoneOffset.UTC).toString());
        parameters.put("commit_strategy", "basic");
        parameters.put("sub", "sub");

        return new JWSHeader.Builder(JWSAlgorithm.RS256).customParams(parameters).build();
    }

}
