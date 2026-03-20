package org.eclipse.edc.verifiablecredentials.jwt;

public interface Constants {
    String VP_CLAIM = "vp";
    String VC_CLAIM = "vc";
    String VERIFIABLE_PRESENTATION_TYPE = "VerifiablePresentation";
    String ENVELOPED_VERIFIABLE_PRESENTATION_TYPE = "EnvelopedVerifiablePresentation";
    String TYPE = "type";
    String JWT_VC_TOKEN_CONTEXT = "dcp-vc";
    String JWT_VP_TOKEN_CONTEXT = "dcp-vp";
    String VERIFIABLE_CREDENTIAL_JSON_KEY = "verifiableCredential";
    String ENVELOPED_CREDENTIAL_TYPE = "EnvelopedVerifiableCredential";
    String ENVELOPED_CREDENTIAL_CONTENT_TYPE = "data:application/vc+jwt";
    String ENVELOPED_PRESENTATION_CONTENT_TYPE = "data:application/vp+jwt";
}
