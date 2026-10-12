package tool;

/**
 * ローカルテスト用のパスワードハッシュを生成する。
 */
public class GenerateTestPassword {

    public static void main(String[] args) {

        System.out.println(
                PasswordUtil.hash("TestPassword123!"));
    }
}