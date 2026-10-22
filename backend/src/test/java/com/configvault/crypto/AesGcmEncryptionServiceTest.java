package com.configvault.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class AesGcmEncryptionServiceTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void encryptsAndDecryptsValues() {
        AesGcmEncryptionService encryption = new AesGcmEncryptionService(KEY, "test-v1");
        EncryptedValue encrypted = encryption.encrypt("secret value");

        assertEquals("test-v1", encrypted.keyId());
        assertNotEquals("secret value", new String(encrypted.ciphertext()));
        assertEquals(
                "secret value",
                encryption.decrypt(encrypted.ciphertext(), encrypted.nonce(), encrypted.keyId()));
    }

    @Test
    void usesDifferentNoncesForEachEncryption() {
        AesGcmEncryptionService encryption = new AesGcmEncryptionService(KEY, "test-v1");

        EncryptedValue first = encryption.encrypt("same value");
        EncryptedValue second = encryption.encrypt("same value");

        assertNotEquals(java.util.Arrays.toString(first.nonce()),
                java.util.Arrays.toString(second.nonce()));
        assertNotEquals(
                java.util.Arrays.toString(first.ciphertext()),
                java.util.Arrays.toString(second.ciphertext()));
    }

    @Test
    void rejectsTamperedCiphertext() {
        AesGcmEncryptionService encryption = new AesGcmEncryptionService(KEY, "test-v1");
        EncryptedValue encrypted = encryption.encrypt("secret value");
        byte[] tampered = encrypted.ciphertext().clone();
        tampered[0] ^= 1;

        assertThrows(
                IllegalStateException.class,
                () -> encryption.decrypt(tampered, encrypted.nonce(), encrypted.keyId()));
    }

    @Test
    void rejectsUnknownKeyId() {
        AesGcmEncryptionService encryption = new AesGcmEncryptionService(KEY, "test-v1");
        EncryptedValue encrypted = encryption.encrypt("secret value");

        assertThrows(
                IllegalStateException.class,
                () -> encryption.decrypt(encrypted.ciphertext(), encrypted.nonce(), "unknown"));
    }

    @Test
    void rejectsInvalidKeyLength() {
        String invalidKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThrows(
                IllegalStateException.class,
                () -> new AesGcmEncryptionService(invalidKey, "test-v1"));
    }
}
