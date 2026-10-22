package com.configvault.crypto;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AesGcmEncryptionService {

    private static final int NONCE_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int AES_KEY_LENGTH_BYTES = 32;

    private final SecretKeySpec key;
    private final String keyId;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmEncryptionService(
            @Value("${configvault.encryption.key}") String encodedKey,
            @Value("${configvault.encryption.key-id:local-v1}") String keyId) {
        byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "CONFIGVAULT_ENCRYPTION_KEY must be a base64-encoded 32-byte AES key",
                    exception);
        }
        if (decodedKey.length != AES_KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "CONFIGVAULT_ENCRYPTION_KEY must be a base64-encoded 32-byte AES key");
        }
        if (keyId.isBlank()) {
            throw new IllegalStateException("CONFIGVAULT_ENCRYPTION_KEY_ID must not be blank");
        }

        this.key = new SecretKeySpec(decodedKey, "AES");
        this.keyId = keyId;
    }

    public EncryptedValue encrypt(String plaintext) {
        byte[] nonce = new byte[NONCE_LENGTH_BYTES];
        secureRandom.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            return new EncryptedValue(
                    cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                    nonce,
                    keyId);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt configuration value", exception);
        }
    }

    public String decrypt(byte[] ciphertext, byte[] nonce, String storedKeyId) {
        if (!keyId.equals(storedKeyId)) {
            throw new IllegalStateException(
                    "No encryption key configured for key id: " + storedKeyId);
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            return new String(
                    cipher.doFinal(ciphertext),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Unable to decrypt configuration value; verify the encryption key and stored data",
                    exception);
        }
    }
}
