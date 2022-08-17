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

package org.eclipse.dataspaceconnector.iam.did.spi.document;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Public part of an Elliptic Curve key
 */
public class EllipticCurvePublicKey implements JwkPublicKey {
    private String crv;
    private String kty;
    private String curvePointX;
    private String curvePointY;

    public EllipticCurvePublicKey() {
        // needed for JSON Deserialization
    }

    public EllipticCurvePublicKey(String crv, String kty, String x, String y) {
        this.crv = crv;
        this.kty = kty;
        curvePointX = x;
        curvePointY = y;
    }

    @JsonProperty("crv")
    public String getCrv() {
        return crv;
    }

    public void setCrv(String crv) {
        this.crv = crv;
    }

    @Override
    @JsonProperty("kty")
    public String getKty() {
        return kty;
    }

    public void setKty(String kty) {
        this.kty = kty;
    }

    @JsonProperty("x")
    public String getX() {
        return curvePointX;
    }

    public void setX(String x) {
        curvePointX = x;
    }

    @JsonProperty("y")
    public String getY() {
        return curvePointY;
    }

    public void setY(String y) {
        curvePointY = y;
    }
}
