package org.eclipse.dataspaceconnector.iam.ion.dto.did;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PublicKeyJwk {
    String crv;
    String kty;
    String x;
    String y;

    public PublicKeyJwk() {
    }

    public PublicKeyJwk(String crv, String kty, String x, String y) {
        this.crv = crv;
        this.kty = kty;
        this.x = x;
        this.y = y;
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
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    @JsonProperty("y")
    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }
}
