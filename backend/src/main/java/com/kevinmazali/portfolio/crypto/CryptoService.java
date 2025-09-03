package com.kevinmazali.portfolio.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Enkel AES-GCM tjeneste for å kryptere/dekryptere tekst-chunks.
 * Lagringsformat:
 *  - content  = base64(ciphertext) (inkl tag i slutten, som vanlig for GCM)
 *  - metadata = enc="aesgcm", enc_iv=base64(IV)
 */
public class CryptoService {

  private static final String AES = "AES";
  private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
  private static final int GCM_TAG_BITS = 128;  // 16 byte tag
  private static final int GCM_IV_BYTES = 12;   // 96-bit IV er best practice

  private final SecretKey key;
  private final SecureRandom random = new SecureRandom();

  public CryptoService(byte[] keyBytes) {
    if (keyBytes == null || keyBytes.length != 32) {
      throw new IllegalArgumentException("AES-256 key must be 32 bytes");
    }
    this.key = new SecretKeySpec(keyBytes, AES);
  }

  public EncResult encrypt(String plaintext) {
    try {
      byte[] iv = new byte[GCM_IV_BYTES];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
      GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
      cipher.init(Cipher.ENCRYPT_MODE, key, spec);
      byte[] ct = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return new EncResult(
          Base64.getEncoder().encodeToString(iv),
          Base64.getEncoder().encodeToString(ct)
      );
    } catch (Exception e) {
      throw new RuntimeException("Encrypt failed", e);
    }
  }

  public String decrypt(String ivBase64, String cipherBase64) {
    try {
      byte[] iv = Base64.getDecoder().decode(ivBase64);
      byte[] ct = Base64.getDecoder().decode(cipherBase64);
      Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
      GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
      cipher.init(Cipher.DECRYPT_MODE, key, spec);
      byte[] pt = cipher.doFinal(ct);
      return new String(pt, java.nio.charset.StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("Decrypt failed", e);
    }
  }

  public record EncResult(String ivBase64, String cipherBase64) {}
}
