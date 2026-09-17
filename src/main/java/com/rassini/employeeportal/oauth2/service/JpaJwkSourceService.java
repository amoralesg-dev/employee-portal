package com.rassini.employeeportal.oauth2.service;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.rassini.employeeportal.oauth2.entity.OauthJwkKeyEntity;
import com.rassini.employeeportal.oauth2.repository.OauthJwkKeyRepository;
import com.rassini.employeeportal.service.MfaCryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JpaJwkSourceService implements JWKSource<SecurityContext> {

    private final OauthJwkKeyRepository keyRepository;
    private final MfaCryptoService cryptoService;

    @Override
    @Transactional
    public List<JWK> get(JWKSelector jwkSelector, SecurityContext context) {
        JWKSet jwkSet = loadOrCreateJwkSet();
        return jwkSelector.select(jwkSet);
    }

    @Transactional
    public synchronized JWKSet loadOrCreateJwkSet() {
        List<OauthJwkKeyEntity> activeKeys = keyRepository.findByStatusIn(
                List.of(OauthJwkKeyEntity.KeyStatus.ACTIVE, OauthJwkKeyEntity.KeyStatus.ROTATING)
        );

        if (activeKeys.isEmpty()) {
            log.info("No active RSA JWK key found in database. Generating a new 2048-bit RSA key pair...");
            OauthJwkKeyEntity newKey = generateAndPersistNewKey();
            activeKeys = List.of(newKey);
        }

        List<JWK> jwks = activeKeys.stream()
                .map(this::toRsaKey)
                .map(rsa -> (JWK) rsa)
                .toList();

        return new JWKSet(jwks);
    }

    private OauthJwkKeyEntity generateAndPersistNewKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

            String keyId = "rassini-key-" + UUID.randomUUID().toString().substring(0, 8);
            String publicKeyPem = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            String privateKeyPem = Base64.getEncoder().encodeToString(privateKey.getEncoded());

            String encryptedPrivateKey = cryptoService.encrypt(privateKeyPem);

            OauthJwkKeyEntity entity = OauthJwkKeyEntity.builder()
                    .keyId(keyId)
                    .keyType("RSA")
                    .algorithm("RS256")
                    .useType("sig")
                    .publicKeyPem(publicKeyPem)
                    .privateKeyEncrypted(encryptedPrivateKey)
                    .status(OauthJwkKeyEntity.KeyStatus.ACTIVE)
                    .build();

            return keyRepository.save(entity);
        } catch (Exception e) {
            log.error("Failed to generate RSA key pair", e);
            throw new IllegalStateException("Failed to generate RSA key pair", e);
        }
    }

    private RSAKey toRsaKey(OauthJwkKeyEntity entity) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            byte[] publicBytes = Base64.getDecoder().decode(entity.getPublicKeyPem());
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(publicBytes));

            String decryptedPrivatePem = cryptoService.decrypt(entity.getPrivateKeyEncrypted());
            byte[] privateBytes = Base64.getDecoder().decode(decryptedPrivatePem);
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateBytes));

            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(entity.getKeyId())
                    .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
                    .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse RSA key with ID: {}", entity.getKeyId(), e);
            throw new IllegalStateException("Failed to parse RSA key from database", e);
        }
    }
}
