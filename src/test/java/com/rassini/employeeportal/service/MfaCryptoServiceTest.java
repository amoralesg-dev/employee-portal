package com.rassini.employeeportal.service;

import com.rassini.employeeportal.exception.MfaCryptoException;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MfaCryptoServiceTest {

    // Llave de ceros unicamente para pruebas unitarias
    private static final String VALID_KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String SECRET_TOTP = "JBSWY3DPEHPK3PXP";

    @Test
    void roundTripCorrecto() {
        MfaCryptoService service = new MfaCryptoService(VALID_KEY);
        String encrypted = service.encrypt(SECRET_TOTP);

        assertThat(encrypted).startsWith("v1:");
        String decrypted = service.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(SECRET_TOTP);
    }

    @Test
    void mismoSecretoGeneraCiphertextDistinto() {
        MfaCryptoService service = new MfaCryptoService(VALID_KEY);
        String enc1 = service.encrypt(SECRET_TOTP);
        String enc2 = service.encrypt(SECRET_TOTP);

        assertThat(enc1).isNotEqualTo(enc2);
    }

    @Test
    void instanciacionFallaSiLlaveAusente() {
        assertThatThrownBy(() -> new MfaCryptoService(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new MfaCryptoService(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void instanciacionFallaSiBase64Invalido() {
        assertThatThrownBy(() -> new MfaCryptoService("NoSoyUnBase64!@#"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void instanciacionFallaSiLlaveDistintaDe32Bytes() {
        String invalidLengthKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new MfaCryptoService(invalidLengthKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void desencriptarRechazaVersionDesconocida() {
        MfaCryptoService service = new MfaCryptoService(VALID_KEY);
        String fakeData = "v2:SomeBase64String";

        assertThatThrownBy(() -> service.decrypt(fakeData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version");
    }

    @Test
    void desencriptarRechazaCiphertextAlteradoXor() {
        MfaCryptoService service = new MfaCryptoService(VALID_KEY);
        String encrypted = service.encrypt(SECRET_TOTP);

        String base64Data = encrypted.substring(3);
        byte[] decoded = Base64.getDecoder().decode(base64Data);
        
        // Alterar byte posterior al IV (byte 13) usando XOR
        decoded[13] = (byte) (decoded[13] ^ 0xFF);
        
        String alteredBase64 = Base64.getEncoder().encodeToString(decoded);
        String finalEncrypted = "v1:" + alteredBase64;

        assertThatThrownBy(() -> service.decrypt(finalEncrypted))
                .isInstanceOf(MfaCryptoException.class)
                .hasMessageContaining("Violacion de integridad");
    }

    @Test
    void desencriptarRechazaTagAlteradoXor() {
        MfaCryptoService service = new MfaCryptoService(VALID_KEY);
        String encrypted = service.encrypt(SECRET_TOTP);

        String base64Data = encrypted.substring(3);
        byte[] decoded = Base64.getDecoder().decode(base64Data);
        
        // Alterar ultimo byte (parte del tag)
        decoded[decoded.length - 1] = (byte) (decoded[decoded.length - 1] ^ 0xFF);
        
        String alteredBase64 = Base64.getEncoder().encodeToString(decoded);
        String finalEncrypted = "v1:" + alteredBase64;

        assertThatThrownBy(() -> service.decrypt(finalEncrypted))
                .isInstanceOf(MfaCryptoException.class)
                .hasMessageContaining("Violacion de integridad");
    }

    @Test
    void encriptarYDesencriptarValorNull() {
        MfaCryptoService service = new MfaCryptoService(VALID_KEY);
        assertThat(service.encrypt(null)).isNull();
        assertThat(service.decrypt(null)).isNull();
    }

    @Test
    void encriptarYDesencriptarValorVacio() {
        MfaCryptoService service = new MfaCryptoService(VALID_KEY);
        assertThatThrownBy(() -> service.encrypt(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.encrypt("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.decrypt(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.decrypt("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void desencriptarRechazaDatoDemasiadoCorto() {
        MfaCryptoService service = new MfaCryptoService(VALID_KEY);
        // Base64 simulando menos de 28 bytes de largo
        String tooShort = "v1:" + Base64.getEncoder().encodeToString(new byte[20]);

        assertThatThrownBy(() -> service.decrypt(tooShort))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("demasiado corto");
    }
}
