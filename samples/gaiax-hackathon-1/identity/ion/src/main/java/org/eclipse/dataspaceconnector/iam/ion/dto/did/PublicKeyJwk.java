package org.eclipse.dataspaceconnector.iam.ion.dto.did;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PublicKeyJwk {
    String crv;
    String kty;
    String x;
    String y;

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
