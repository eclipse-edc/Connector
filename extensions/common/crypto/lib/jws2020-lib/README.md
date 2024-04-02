# JsonWebSignature2020

This module extends the [iron-verifiable-credentials library](https://github.com/filip26/iron-verifiable-credentials),
which we use in conjunction with [titanium-ld](https://github.com/filip26/titanium-json-ld/) with an implementation for
the [JsonWebSignature2020](https://www.w3.org/community/reports/credentials/CG-FINAL-lds-jws2020-20220721) crypto suite.

## Technical aspects

This implementation is actually mostly glue code between the `iron-verifiable-credentials` lib and the
well-known [Nimbus JOSE lib](https://connect2id.com/products/nimbus-jose-jwt), as all cryptographic primitives are taken
from Nimbus.

VerifiableCredentials and VerifiablePresentations are processed as JSON(-LD) objects, so some familiarity with JSON-LD
is required.
The entrypoint into the cryptographic suite is the `Vc` class, which allows signing/issuing and verifying JSON-LD
structures. The following samples use explicit types for clarity. These are just some illustrative examples, please
check the `IssuerTests` and the `VerifierTests` for more comprehensive explanations.

### Sign a VC

```java
JwsSignature2020Suite suite=new JwsSignature2020Suite(JacksonJsonLd.createObjectMapper());
JsonObject vc=createVcAsJsonLd();
JWK keyPair=createKeyPairAsJwk();
JwkMethod signKeys=new JwkMethod(id,type,controller,keyPair);

var options=suite.createOptions()
        .created(Instant.now())
        .verificationMethod(signKeys) // embeds the proof
        .purpose(URI.create("https://w3id.org/security#assertionMethod"));

Issuer signedVc=Vc.sign(vc,signKeys,options);

JsonObject compacted=IssuerCompat.compact(signedVc);
```

### Verify a VC

```java
JwsSignature2020Suite suite=new JwsSignature2020Suite(JacksonJsonLd.createObjectMapper());
JsonObject vc=readSignedVc();
Verifier result=Vc.verify(vc,suite);

try{
    result.isValid();
}catch(VerificationError error){
    //handle    
}
```

## Limitations & Known Issues

Java 17 [dropped support](https://connect2id.com/products/nimbus-jose-jwt/examples/jwt-with-es256k-signature) for
the `secp256k1` curve. Alternatively, the BouncyCastle JCA provider could be used.
For this implementation, we chose to forego this at the benefit of a smaller library footprint. There is plenty of other
curves to choose from.

On a similar note, support for Octet Keypairs (`"OKP"`) has not yet been added to the standard Java JCA, thus an
additional dependency `tink` is needed,
check [here](https://connect2id.com/products/nimbus-jose-jwt/examples/jwk-generation#okp) for details. If that is not
acceptable to you, please add a dependency exclusion to your build script.

`iron-verifiable-credentials` is not 100% agnostic toward its crypto suites, for example there is
a [hard-coded context](https://github.com/filip26/iron-verifiable-credentials/blob/82d13326c5f64a0f38c75d417ffc263febfd970d/src/main/java/com/apicatalog/vc/processor/Issuer.java#L122)
added to the compacted JSON-LD, which is incorrect. It doesn't negatively impact the resulting JSON-LD, other than
possibly affecting processing times, but unfortunately it also makes it impossible to add more contexts, such
as https://w3id.org/security/suites/jws-2020/v1. We mitigated this with
the [`IssuerCompatibilty.java`](./src/main/java/org/eclipse/edc/security/signature/jws2020/IssuerCompatibility.java),
which should be
used
for compaction.
