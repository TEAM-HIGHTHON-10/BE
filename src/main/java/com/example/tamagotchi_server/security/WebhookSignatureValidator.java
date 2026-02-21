package com.example.tamagotchi_server.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Validates GitHub webhook signatures using HMAC-SHA256.
 * Compares X-Hub-Signature-256 header against the computed signature.
 */
@Component
public class WebhookSignatureValidator {

    private final String webhookSecret;

    public WebhookSignatureValidator(@Value("${github.webhook.secret}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public boolean isValid(String signatureHeader, byte[] payload) {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }

        String expected = signatureHeader.substring("sha256=".length());

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload);
            String computed = HexFormat.of().formatHex(hash);

            // Constant-time comparison to prevent timing attacks
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    computed.getBytes(StandardCharsets.UTF_8));

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return false;
        }
    }
}
