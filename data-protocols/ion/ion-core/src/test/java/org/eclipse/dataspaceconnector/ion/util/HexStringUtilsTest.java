package org.eclipse.dataspaceconnector.ion.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;


class HexStringUtilsTest {
    @Test
    void encodeToHexBytes() {

        final var input = "cd18bb83b12080fae7945d31688f62ad968a7e980feb22642729d104e3097fe2";
        var expectedOutput = new byte[]{
                (byte) 205,
                24,
                (byte) 187,
                (byte) 131,
                (byte) 177,
                32,
                (byte) 128,
                (byte) 250,
                (byte) 231,
                (byte) 148,
                93,
                49,
                104,
                (byte) 143,
                98,
                (byte) 173,
                (byte) 150,
                (byte) 138,
                (byte) 126,
                (byte) 152,
                15,
                (byte) 235,
                34,
                100,
                39,
                41,
                (byte) 209,
                4,
                (byte) 227,
                9,
                127,
                (byte) 226

        };

        var encoded = HexStringUtils.encodeToHexBytes(input);
        Assertions.assertThat(encoded).isEqualTo(expectedOutput);

    }

    @Test
    void encodeToHex() {
        var input = "643937366564313632323965646530326532633433343366373365613133633265363233633363303765376138663463623131653765356336356437323" +
                "0333930626631383831373639333935303430653865306338376339643538313964343133383835663034653563643034356434333463356362653831313335336" +
                "461666238613834383863393434376138353261396335376438323135333463626237343433316661373937343732363438653433646437663864393938323262373" +
                "732303734343431386430383835653263376331366634633862633165663638366239353331616261333035306565633561666436376163376262393238373631636" +
                "2353431323861646363643163303234356632613761313331363662646262303631363830633635383430393133303466366135313836643331313739396466316264336262656364633536343662313864333333";
        var expectedOutput = "d976ed16229ede02e2c4343f73ea13c2e623c3c07e7a8f4cb11e7e5c65d720390bf1881769395040e8e0c87c9d5819d413885f04e5cd045d434c" +
                "5cbe811353dafb8a8488c9447a852a9c57d821534cbb74431fa797472648e43dd7f8d99822b7720744418d0885e2c7c16f4c8bc1ef686b9531aba3050eec5afd67ac7" +
                "bb928761cb54128adccd1c0245f2a7a13166bdbb061680c6584091304f6a5186d311799df1bd3bbecdc5646b18d333";

        Assertions.assertThat(HexStringUtils.encodeToHex(input)).isEqualTo(expectedOutput);
    }
}
