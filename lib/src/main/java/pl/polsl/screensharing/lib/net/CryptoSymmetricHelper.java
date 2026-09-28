package pl.polsl.screensharing.lib.net;

import lombok.Getter;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

import static pl.polsl.screensharing.lib.SharedConstants.IV_SIZE;

@Getter
public class CryptoSymmetricHelper {
    private Cipher cipher;
    private SecretKeySpec secretKeySpec;
    private final SecureRandom secureRandom;

    public CryptoSymmetricHelper() {
        secureRandom = new SecureRandom();
    }

    public byte[] encrypt(byte[] rawData) throws Exception {
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, secureRandom);
        final byte[] iv = cipher.getIV();
        final byte[] encrypted = cipher.doFinal(rawData);
        final byte[] encryptedWithIv = new byte[encrypted.length + iv.length];
        System.arraycopy(encrypted, 0, encryptedWithIv, 0, encrypted.length);
        System.arraycopy(iv, 0, encryptedWithIv, encrypted.length, iv.length);
        return encryptedWithIv;
    }

    public byte[] decrypt(byte[] data, int length) throws Exception {
        final byte[] iv = new byte[IV_SIZE];
        final byte[] encryptedWithoutIv = new byte[length - iv.length];
        System.arraycopy(data, length - iv.length, iv, 0, iv.length);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
        cipher.doFinal(data, 0, length - iv.length, encryptedWithoutIv);
        return encryptedWithoutIv;
    }

    public void init(byte[] secretKey) throws Exception {
        secretKeySpec = new SecretKeySpec(secretKey, "AES");
        cipher = Cipher.getInstance("AES/CTR/NoPadding");
    }
}
