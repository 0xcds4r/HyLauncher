package com.linghy.launcher;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

// TODO: online mode
public final class UUIDGen
{
    private static final String TYPE = "GGJJWT";
    private static final String SALT = "4HVUJD)611";

    // .gopclntab:00000000017841A0 byte_17841A0
    private static final byte[] SEED = hexStringToByteArray(
            "c0621b00073b03005000000000000000deeb08001aec080038ec080004000000"
    );

    public static String generateUUID(String nick)
    {
        try {
            String input = "Player:" + nick.trim();
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

            hash[6] &= 0x0f;
            hash[6] |= 0x30;
            hash[8] &= 0x3f;
            hash[8] |= 0x80;

            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) msb = (msb << 8) | (hash[i] & 0xff);
            for (int i = 8; i < 16; i++) lsb = (lsb << 8) | (hash[i] & 0xff);

            return new UUID(msb, lsb).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String generateIdentityToken(String nick) throws Exception
    {
        String headerJson = "{\"alg\":\"Ed25519\",\"typ\":\"JWT\"}";

        String payloadJson = "{\"aud\":\"https://sessions.hytale.com/\", \"sub\":\"" + nick + "\",\"scope\":[\"hytale:client\"]}";

        String headerBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes());
        String payloadBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes());

        String message = headerBase64 + "." + payloadBase64;

        Ed25519PrivateKeyParameters privateKey = new Ed25519PrivateKeyParameters(SEED, 0);
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, privateKey);
        signer.update(message.getBytes(), 0, message.getBytes().length);
        byte[] signature = signer.generateSignature();

        String signatureBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);

        return headerBase64 + "." + payloadBase64 + "." + signatureBase64;
    }

    private static byte[] xorPayload(byte[] data, String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            out[i] = (byte) (data[i] ^ keyBytes[i % keyBytes.length]);
        }
        return out;
    }

    private static byte[] hexStringToByteArray(String s)
    {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String generateSessionToken() {
        return UUID.randomUUID().toString();
    }
}

