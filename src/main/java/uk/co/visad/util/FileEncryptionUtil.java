package uk.co.visad.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * AES-256-GCM file encryption utility.
 *
 * On-disk format:
 *   [8 bytes magic "VISADENC"][12 bytes IV][ciphertext + 16 bytes GCM tag]
 *
 * The magic header allows backward-compatible detection: files without the
 * header are treated as plaintext and served as-is.
 */
public class FileEncryptionUtil {

    private static final byte[] MAGIC = "VISADENC".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private static final int MAGIC_LEN = 8;
    private static final int IV_LEN = 12;
    private static final int GCM_TAG_BITS = 128;

    private final SecretKeySpec keySpec;
    private final SecureRandom random = new SecureRandom();

    public FileEncryptionUtil(byte[] keyBytes) {
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("Encryption key must be exactly 32 bytes (256 bits)");
        }
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Returns true if the byte array starts with the VISADENC magic header.
     */
    public boolean isEncrypted(byte[] data) {
        if (data == null || data.length < MAGIC_LEN) return false;
        return Arrays.equals(Arrays.copyOf(data, MAGIC_LEN), MAGIC);
    }

    /**
     * Encrypts plaintext bytes.
     * Returns: [8B magic][12B IV][ciphertext + 16B GCM tag]
     */
    public byte[] encrypt(byte[] plaintext) throws GeneralSecurityException {
        byte[] iv = new byte[IV_LEN];
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] ciphertext = cipher.doFinal(plaintext);

        byte[] result = new byte[MAGIC_LEN + IV_LEN + ciphertext.length];
        System.arraycopy(MAGIC, 0, result, 0, MAGIC_LEN);
        System.arraycopy(iv, 0, result, MAGIC_LEN, IV_LEN);
        System.arraycopy(ciphertext, 0, result, MAGIC_LEN + IV_LEN, ciphertext.length);
        return result;
    }

    /**
     * Decrypts bytes previously encrypted by {@link #encrypt(byte[])}.
     */
    public byte[] decrypt(byte[] data) throws GeneralSecurityException {
        if (!isEncrypted(data)) {
            throw new IllegalArgumentException("Data does not have the VISADENC header");
        }
        byte[] iv = Arrays.copyOfRange(data, MAGIC_LEN, MAGIC_LEN + IV_LEN);
        byte[] ciphertext = Arrays.copyOfRange(data, MAGIC_LEN + IV_LEN, data.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
        return cipher.doFinal(ciphertext);
    }
}
