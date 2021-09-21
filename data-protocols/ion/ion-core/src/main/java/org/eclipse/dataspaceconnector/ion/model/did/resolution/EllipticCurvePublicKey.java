package org.eclipse.dataspaceconnector.ion.model.did.resolution;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Public part of an Elliptic Curve key
 */
public class EllipticCurvePublicKey {
    String crv;
    String kty;
    String curvePointX;
    String curvePointY;

    public EllipticCurvePublicKey() {
    }

    public EllipticCurvePublicKey(String crv, String kty, String x, String y) {
        this.crv = crv;
        this.kty = kty;
        this.curvePointX = x;
        this.curvePointY = y;
    }

    @JsonProperty("crv")
    public String getCrv() {
        return crv;
    }

    public void setCrv(String crv) {
        this.crv = crv;
    }

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
        this.curvePointX = x;
    }

    @JsonProperty("y")
    public String getY() {
        return curvePointY;
    }

    public void setY(String y) {
        this.curvePointY = y;
    }
}
