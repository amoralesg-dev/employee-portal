package com.rassini.employeeportal.service;

import com.rassini.employeeportal.exception.MfaCryptoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class MfaCryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128; // En bits
    private static final String VERSION_PREFIX = "v1:";

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public MfaCryptoService(@Value("${app.security.mfa.encryption-key}") String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException("La llave MFA_ENCRYPTION_KEY es requerida y no puede estar vacia.");
        }

        byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("MFA_ENCRYPTION_KEY no tiene un formato Base64 valido.");
        }

        if (decodedKey.length != 32) {
            throw new IllegalArgumentException(
                "MFA_ENCRYPTION_KEY debe ser de exactamente 32 bytes (256 bits). Actual: " + decodedKey.length + " bytes."
            );
        }

        this.secretKey = new SecretKeySpec(decodedKey, "AES");
        this.secureRandom = new SecureRandom(); // Reutiliza instancia
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        if (plainText.isBlank()) {
            throw new IllegalArgumentException("El secreto a cifrar no puede estar vacio.");
        }

        try {
            // 1. Generar IV Aleatorio
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // 2. Inicializar Cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // 3. Cifrar (En GCM, el metodo doFinal adjunta el MAC/Tag de 16 bytes al final automaticamente)
            byte[] cipherTextWithTag = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 4. Concatenar IV + (CipherText + Tag)
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherTextWithTag.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherTextWithTag);

            // 5. Codificar en Base64 y agregar versionado
            return VERSION_PREFIX + Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            // Catch global para no exponer metadatos
            throw new MfaCryptoException("Fallo interno al cifrar los datos de seguridad.", e);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null) {
            return null;
        }
        if (encryptedText.isBlank()) {
            throw new IllegalArgumentException("El texto cifrado no puede estar vacio.");
        }

        if (!encryptedText.startsWith(VERSION_PREFIX)) {
            throw new IllegalArgumentException("Formato cifrado invalido o version de cifrado desconocida.");
        }

        try {
            // 1. Remover prefijo y decodificar Base64
            String base64Data = encryptedText.substring(VERSION_PREFIX.length());
            byte[] decodedData = Base64.getDecoder().decode(base64Data);

            // 2. Validar tamano minimo: 12 bytes IV + 16 bytes Tag = 28 bytes minimo absoluto
            int minExpectedLength = GCM_IV_LENGTH + (GCM_TAG_LENGTH / 8);
            if (decodedData.length < minExpectedLength) {
                throw new IllegalArgumentException("El payload cifrado esta truncado o es demasiado corto.");
            }

            // 3. Extraer IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decodedData, 0, iv, 0, GCM_IV_LENGTH);

            // 4. Extraer CipherText + Tag
            int cipherTextLength = decodedData.length - GCM_IV_LENGTH;
            byte[] cipherTextWithTag = new byte[cipherTextLength];
            System.arraycopy(decodedData, GCM_IV_LENGTH, cipherTextWithTag, 0, cipherTextLength);

            // 5. Inicializar Cipher para descifrado
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // 6. Descifrar (Si el Tag o el IV fueron alterados un solo bit, arrojara AEADBadTagException)
            byte[] plainTextBytes = cipher.doFinal(cipherTextWithTag);
            return new String(plainTextBytes, StandardCharsets.UTF_8);

        } catch (IllegalArgumentException e) {
            throw e; // Relanzar validaciones propias (tamano, etc)
        } catch (Exception e) {
            // Captura AEADBadTagException ocultando los valores reales en memoria
            throw new MfaCryptoException("Violacion de integridad: Datos corruptos, manipulados o llave incorrecta.", e);
        }
    }
}
