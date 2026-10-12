package tool;

/**
 * ローカルでの動作確認用。
 */
public class PasswordUtilTest {

    public static void main(String[] args) {

        String password = "TestPassword123!";
        String hash = PasswordUtil.hash(password);

        System.out.println(
                "正しいパスワード: "
                + PasswordUtil.verify(password, hash));

        System.out.println(
                "間違ったパスワード: "
                + PasswordUtil.verify("WrongPassword", hash));

        System.out.println(
                "不正なハッシュ: "
                + PasswordUtil.verify(password, "invalid"));
    }
}