BEGIN;

-- 生徒のログイン状態を維持するためのトークン
CREATE TABLE public.student_login_token (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    user_id bigint NOT NULL
        REFERENCES public.users(id) ON DELETE CASCADE,

    -- トークン本体ではなくSHA-256のハッシュ値を保存する
    token_hash varchar(64) NOT NULL UNIQUE,

    created_at timestamp with time zone
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    expires_at timestamp with time zone NOT NULL,

    CONSTRAINT ck_student_login_token_hash
        CHECK (token_hash ~ '^[0-9a-f]{64}$'),

    CONSTRAINT ck_student_login_token_expiry
        CHECK (expires_at > created_at)
);

-- 利用者単位でトークンを管理する
CREATE INDEX idx_student_login_token_user
    ON public.student_login_token(user_id);

-- 有効期限切れのトークンを検索する
CREATE INDEX idx_student_login_token_expiry
    ON public.student_login_token(expires_at);

-- ローカル環境のアプリケーション用ユーザー
GRANT SELECT, INSERT, UPDATE, DELETE
    ON public.student_login_token
    TO school_safety_user;

GRANT USAGE, SELECT
    ON SEQUENCE public.student_login_token_id_seq
    TO school_safety_user;

COMMIT;