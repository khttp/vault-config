package com.configvault.crypto;

public record EncryptedValue(byte[] ciphertext, byte[] nonce, String keyId) {
}
