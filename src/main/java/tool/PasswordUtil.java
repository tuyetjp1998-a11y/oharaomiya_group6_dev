package tool;

import java.nio.charset.StandardCharsets;

import org.mindrot.jbcrypt.BCrypt;

/**
 * パスワードのハッシュ化と照合を行う。
 */
public final class PasswordUtil {

    // BCryptの計算コスト
    private static final int WORK_FACTOR = 12;

    // パスワードの最大バイト数
    private static final int MAX_PASSWORD_BYTES = 72;

    private PasswordUtil() {
        // インスタンス化しない
    }

    /**
     * 新規登録時にパスワードをハッシュ化する。
     */
    public static String hash(String password) {

        if (!isValidLength(password)) {
            throw new IllegalArgumentException(
                    "パスワードはUTF-8で1～72バイトにしてください。");
        }

        return BCrypt.hashpw(
                password,
                BCrypt.gensalt(WORK_FACTOR));
    }

    /**
     * 入力されたパスワードと保存済みハッシュを照合する。
     */
    public static boolean verify(
            String password, String passwordHash) {

        if (!isValidLength(password)
                || passwordHash == null
                || passwordHash.isBlank()) {
            return false;
        }

        try {
            return BCrypt.checkpw(password, passwordHash);

        } catch (IllegalArgumentException e) {
            // ハッシュの形式が不正な場合
            return false;
        }
    }

    /**
     * BCryptで扱うパスワードの長さを確認する。
     */
    private static boolean isValidLength(String password) {

        if (password == null || password.isEmpty()) {
            return false;
        }

        int byteLength =
                password.getBytes(StandardCharsets.UTF_8).length;

        return byteLength <= MAX_PASSWORD_BYTES;
    }
}