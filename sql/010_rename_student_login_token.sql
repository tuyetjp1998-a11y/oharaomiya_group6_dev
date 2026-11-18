BEGIN;

-- 生徒・保護者共通のテーブル名に変更する
ALTER TABLE public.student_login_token
    RENAME TO login_token;

COMMIT;