package common.network.encryption;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * 암호화를 위한 유틸리티 클래스다.
 */
public class NetworkEncryptionUtils {
    /**
     * AES 알고리즘을 통해 비밀 키를 생성한다. 이러한 비밀 키는 암호화에 사용된다.
     * @return 비밀 키
     */
    public static SecretKey generateSecretKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * RSA 알고리즘을 통해 encoded 된 PublicKey를 복호화 한다.
     * @param key 인코딩된 PublicKey ByteArray
     * @return Public Key
     */
    public static PublicKey decodeEncodedRsaPublicKey(byte[] key) {
        try {
            EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(key);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(encodedKeySpec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * SecretKey를 통해 Cipher를 생성한다. opMode는 {@link Cipher#ENCRYPT_MODE} {@link Cipher#DECRYPT_MODE} 등을 참고하라.
     * @param opMode Cipher의 역할. 해당 프로젝트에서는 {@link Cipher#ENCRYPT_MODE}, {@link Cipher#DECRYPT_MODE} 만을 사용한다.
     * @param key Cipher에 사용할 키
     * @return 특정 작업에 사용 될 {@link Cipher}
     */
    public static Cipher cipherFromKey(int opMode, Key key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
            cipher.init(opMode, key, new IvParameterSpec(key.getEncoded()));
            return cipher;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Key를 이용하여 암호화 한다. 이 프로젝트에서는 nonce와 Secretkey를 Publickey로 암호화 할 때 쓰인다.
     * @param key 암호화에 사용할 key
     * @param data 암호화할 byte array
     * @return 암호화된 byte array
     */
    public static byte[] encrypt(Key key, byte[] data) {
        return crypt(Cipher.ENCRYPT_MODE, key, data);
    }

    /**
     * Key를 통하여 복호화 한다. 이 프로젝트에서는 nonce와 SecretKey를 PrivateKey로 복호화 할 때 주로 쓰인다.
     * @param key 복호화에 사용할 Key
     * @param data 암호화된 byte Array
     * @return 복호화 된 byte Array
     */
    public static byte[] decrypt(Key key, byte[] data) {
        return crypt(Cipher.DECRYPT_MODE, key, data);
    }

    private static byte[] crypt(int opMode, Key key, byte[] data) {
        try {
            return createCipher(opMode, key.getAlgorithm(), key).doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Cipher createCipher(int opMode, String algorithm, Key key) throws Exception {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(opMode, key);
        return cipher;
    }

    public static SecretKey decryptSecretKey(PrivateKey privateKey, byte[] data) {
        byte[] bytes = decrypt(privateKey, data);

        try {
            return new SecretKeySpec(bytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
