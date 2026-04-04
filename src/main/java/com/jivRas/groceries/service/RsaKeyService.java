package com.jivRas.groceries.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

/**
 * AES-256-GCM encryption service.
 *
 * A fixed 256-bit key is shared between this server and the frontend.
 * The frontend encrypts the login password with AES-GCM before sending;
 * this service decrypts it before passing to Spring's AuthenticationManager.
 *
 * Format expected: "<base64-iv>:<base64-ciphertext>"
 * (IV is 12 bytes, authentication tag is 128 bits — GCM defaults)
 */
@Service
public class RsaKeyService {

    // 256-bit AES key — hardcoded and shared with the frontend.
    // Change this value in both places if you rotate the key.
    private static final String HEX_KEY =
            "6a8f3b2e9c1d5f7a0e4b8c2d6f9a1e3b7d5c8f2a4e6b0d3f5a7c9e1b3d7f9ac4";

    private final SecretKeySpec secretKey;

    public RsaKeyService() {
        byte[] keyBytes = hexToBytes(HEX_KEY);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Decrypts a "<base64-iv>:<base64-ciphertext>" string produced by the frontend.
     */
    public String decrypt(String encryptedPayload) throws Exception {
        String[] parts = encryptedPayload.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid encrypted payload format");
        }
        byte[] iv         = Base64.getDecoder().decode(parts[0]);
        byte[] ciphertext = Base64.getDecoder().decode(parts[1]);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
        byte[] decrypted = cipher.doFinal(ciphertext);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
