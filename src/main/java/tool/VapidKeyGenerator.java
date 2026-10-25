package tool;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

/**
 * Web Push用のVAPID鍵を生成する。
 */
public class VapidKeyGenerator {

    public static void main(String[] args) throws Exception {

        KeyPairGenerator generator =
                KeyPairGenerator.getInstance("EC");

        generator.initialize(
                new ECGenParameterSpec("secp256r1"));

        KeyPair keyPair = generator.generateKeyPair();

        ECPublicKey publicKey =
                (ECPublicKey) keyPair.getPublic();

        ECPrivateKey privateKey =
                (ECPrivateKey) keyPair.getPrivate();

        // 公開鍵：非圧縮形式（04 + X座標 + Y座標）
        byte[] publicBytes = new byte[65];
        publicBytes[0] = 0x04;

        System.arraycopy(
                to32Bytes(publicKey.getW().getAffineX()),
                0, publicBytes, 1, 32);

        System.arraycopy(
                to32Bytes(publicKey.getW().getAffineY()),
                0, publicBytes, 33, 32);

        byte[] privateBytes =
                to32Bytes(privateKey.getS());

        Base64.Encoder encoder =
                Base64.getUrlEncoder().withoutPadding();

        System.out.println(
                "VAPID_PUBLIC_KEY="
                + encoder.encodeToString(publicBytes));

        System.out.println(
                "VAPID_PRIVATE_KEY="
                + encoder.encodeToString(privateBytes));
    }

    private static byte[] to32Bytes(BigInteger value) {
        byte[] source = value.toByteArray();
        byte[] result = new byte[32];

        int length = Math.min(source.length, result.length);

        System.arraycopy(
                source,
                source.length - length,
                result,
                result.length - length,
                length);

        return result;
    }
}