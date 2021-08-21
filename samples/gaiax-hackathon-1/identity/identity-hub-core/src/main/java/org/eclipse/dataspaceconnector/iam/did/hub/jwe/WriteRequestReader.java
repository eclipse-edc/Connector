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
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.message.Commit;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.message.CommitStrategy;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.message.InterfaceType;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.message.WriteRequest;
import org.eclipse.dataspaceconnector.spi.EdcException;

import java.text.ParseException;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Reads a {@link Commit} passed as part of a {@link WriteRequest}.
 */
public class WriteRequestReader extends AbstractJweReader<WriteRequestReader> {
    private Predicate<JWSObject> verifier;

    public Commit readCommit() {
        Objects.requireNonNull(jwe);
        Objects.requireNonNull(privateKey);
        Objects.requireNonNull(mapper);
        Objects.requireNonNull(verifier);
        try {
            var parsedJwe = JWEObject.parse(jwe);
            parsedJwe.decrypt(new RSADecrypter(privateKey));

            var writeRequest = mapper.readValue(parsedJwe.getPayload().toString(), WriteRequest.class);


            var jsonCommitObject = writeRequest.getCommit();
            var parsedJwsCommit = JWSObject.parse(jsonCommitObject.getProtectedHeader() + "." + jsonCommitObject.getPayload() + "." + jsonCommitObject.getSignature());

            var header = parsedJwsCommit.getHeader();

            return Commit.Builder.newInstance()
                    .commitStrategy(CommitStrategy.valueOf((String) header.getCustomParam("commit_strategy")))
                    .iss(writeRequest.getIss())
                    .sub((String) header.getCustomParam("sub"))
                    .interrfaceType(InterfaceType.valueOf((String) header.getCustomParam("interface")))
                    .operation(Commit.Operation.valueOf((String) header.getCustomParam("operation")))
                    .type((String) header.getCustomParam("type"))
                    .context((String) header.getCustomParam("context"))
                    .objectId(jsonCommitObject.getHeader().getRev())     // object id is the rev header (stated in the spec)
                    .payload(parsedJwsCommit.getPayload().toJSONObject())
                    .build();
        } catch (ParseException | JOSEException | JsonProcessingException e) {
            throw new EdcException(e);
        }
    }

    public WriteRequestReader verifier(Predicate<JWSObject> verifier) {
        this.verifier = verifier;
        return this;
    }

}
