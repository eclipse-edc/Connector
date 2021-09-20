/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.security;

/**
 * PKS8 private keys used for testing.
 */
interface PrivateTestKeys {

    String ENCODED_PRIVATE_KEY_NOHEADER = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAOBwfl+mohb44Cxtqm3jEXqsVBK+ftjKkdapcBKc62D19tJ9g925s7u44Ck2222B6EQZplzLuAHxMl4DJ2oky5jhj6wKqIgIQwoL/uTLPBTi/dkPUgfvC9DkbC4R2nhT3hxp4VkcYkwUall+R4EeHQ3qmWaY2vqsoaduqGrOjoyRAgMBAAECgYAXMsl9LYYXhcX2EafKD+xgl6tg/Juz4MxOOdlBs0KJFSNcAmk849L2FlflKqxnl0Pgth4B/XSZjsq7+Ot8By2cADavChhIQZ8Qqez6GNYmyzrhqnxtVEtB/0M0+RiZWVN/5vBXsKxHbDJDBPU1U2VI0Z3fSHZqCn4u4hi9FZcUoQJBAPG/f8g6AcazKGShLoBeqKCQ0u3GdG+dg7ecfh6aSDzHr19Vyl/KIr9fSCVKaA1ADuVc/tvVgj9XMcSxpQ42PLUCQQDtq8UifFfGsIXIaNJqmbbiJS+marRZY++CBgXNGPGCnDy6HY90QzPR/nmbMNyTmVdX0nqyc4AaJBZEeFpV15XtAkEAzkuyybTmix+b2rPJMPaWQ98SgKIX/p+WJXvADHFwL53fxCU7u56NJG407M2gGZ6Ol6BSRFrg+Rh5efZ2ebhC+QJAY0PWtiyQzJ91gNqm53cD9zfoyuzOHneq1PeG/L5qQE7Y3jLyc3CN+Cr8x2CE//CPllKGhHnRCxn/YuGReUDtkQJAXD3ALwp3pdqrD58Nxla9ipepS7JoNg0jdetARPTPa9l+r4e4tJZCGqZpKeWnyXpoeGDVIeEov7W7MQ5s3H7fFQ==";

    String ENCODED_PRIVATE_KEY_HEADER = "-----BEGIN PRIVATE KEY-----\n" +
            "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAOBwfl+mohb44Cxt\n" +
            "qm3jEXqsVBK+ftjKkdapcBKc62D19tJ9g925s7u44Ck2222B6EQZplzLuAHxMl4D\n" +
            "J2oky5jhj6wKqIgIQwoL/uTLPBTi/dkPUgfvC9DkbC4R2nhT3hxp4VkcYkwUall+\n" +
            "R4EeHQ3qmWaY2vqsoaduqGrOjoyRAgMBAAECgYAXMsl9LYYXhcX2EafKD+xgl6tg\n" +
            "/Juz4MxOOdlBs0KJFSNcAmk849L2FlflKqxnl0Pgth4B/XSZjsq7+Ot8By2cADav\n" +
            "ChhIQZ8Qqez6GNYmyzrhqnxtVEtB/0M0+RiZWVN/5vBXsKxHbDJDBPU1U2VI0Z3f\n" +
            "SHZqCn4u4hi9FZcUoQJBAPG/f8g6AcazKGShLoBeqKCQ0u3GdG+dg7ecfh6aSDzH\n" +
            "r19Vyl/KIr9fSCVKaA1ADuVc/tvVgj9XMcSxpQ42PLUCQQDtq8UifFfGsIXIaNJq\n" +
            "mbbiJS+marRZY++CBgXNGPGCnDy6HY90QzPR/nmbMNyTmVdX0nqyc4AaJBZEeFpV\n" +
            "15XtAkEAzkuyybTmix+b2rPJMPaWQ98SgKIX/p+WJXvADHFwL53fxCU7u56NJG40\n" +
            "7M2gGZ6Ol6BSRFrg+Rh5efZ2ebhC+QJAY0PWtiyQzJ91gNqm53cD9zfoyuzOHneq\n" +
            "1PeG/L5qQE7Y3jLyc3CN+Cr8x2CE//CPllKGhHnRCxn/YuGReUDtkQJAXD3ALwp3\n" +
            "pdqrD58Nxla9ipepS7JoNg0jdetARPTPa9l+r4e4tJZCGqZpKeWnyXpoeGDVIeEo\n" +
            "v7W7MQ5s3H7fFQ==\n" +
            "-----END PRIVATE KEY-----";

    String ENCODED_PRIVATE_KEY_NOPEM = "JBAPG/f8g6AcazKGShLoBeqKCQ0u3GdG+dg7ecfh6aSDzH\n" +
            "19Vyl/KIr9fSCVKaA1ADuVc/tvVgj9XMcSxpQ42PLUCQQDtq8UifFfGsIXIaNJq\n" +
            "mbbiJS+marRZY++CBgXNGPGCnDy6HY90QzPR/nmbMNyTmVdX0nqyc4AaJBZEeFpV\n" +
            "15XtAkEAzkuyybTmix+b2rPJMPaWQ98SgKIX/p+WJXvADHFwL53fxCU7u56NJG40\n" +
            "7M2gGZ6Ol6BSRFrg+Rh5efZ2ebhC+QJAY0PWtiyQzJ91gNqm";
}
