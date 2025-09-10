package com.kevinmazali.portfolio.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Minimal AES-GCM helper for encrypting/decrypting text chunks.
 * Storage format:
 *  - content  = base64(ciphertext) (includes tag at the end, as in GCM)
 *  - metadata = enc="aesgcm", enc_iv=base64(IV)
 */
public class CryptoService {

  private static final String AES = "AES";
  private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
  private static final int GCM_TAG_BITS = 128;  // 16 byte tag
  private static final int GCM_IV_BYTES = 12;   // 96-bit IV is best practice

  private final SecretKey key;
  private final SecureRandom random = new SecureRandom();

  /**
   * Creates a new service instance with a 32-byte AES-256 key.
   *
   * @param keyBytes AES-256 key material (32 bytes)
   */
  public CryptoService(byte[] keyBytes) {
    if (keyBytes == null || keyBytes.length != 32) {
      throw new IllegalArgumentException("AES-256 key must be 32 bytes");
    }
    this.key = new SecretKeySpec(keyBytes, AES);
  }

  /**
   * Encrypts the provided UTF-8 text with AES/GCM/NoPadding and a random IV.
   *
   * @param plaintext input text
   * @return IV and ciphertext as Base64 strings
   */
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

  /**
   * Decrypts the given Base64-encoded IV and ciphertext.
   *
   * @param ivBase64 Base64 IV (12 bytes)
   * @param cipherBase64 Base64 ciphertext with GCM tag
   * @return decrypted UTF-8 text
   */
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

  /** Holds the Base64-encoded IV and ciphertext. */
  public record EncResult(String ivBase64, String cipherBase64) {}
}
