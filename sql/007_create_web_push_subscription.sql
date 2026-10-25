BEGIN;

-- 子どもの安否回答に関する通知設定
ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS notification_enabled
        boolean NOT NULL DEFAULT false;

-- 保護者が通知を受信するブラウザーの登録情報
CREATE TABLE public.web_push_subscription (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    user_id bigint NOT NULL
        REFERENCES public.users(id) ON DELETE CASCADE,

    endpoint varchar(2048) NOT NULL UNIQUE,
    p256dh varchar(255) NOT NULL,
    auth varchar(255) NOT NULL,

    created_at timestamp with time zone
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at timestamp with time zone
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_push_endpoint
        CHECK (endpoint LIKE 'https://%'),

    CONSTRAINT ck_push_p256dh
        CHECK (length(btrim(p256dh)) > 0),

    CONSTRAINT ck_push_auth
        CHECK (length(btrim(auth)) > 0)
);

CREATE INDEX idx_web_push_subscription_user
    ON public.web_push_subscription(user_id);

-- ローカル環境のアプリ用DBユーザー
GRANT SELECT, INSERT, UPDATE, DELETE
    ON public.web_push_subscription
    TO school_safety_user;

GRANT USAGE, SELECT
    ON SEQUENCE public.web_push_subscription_id_seq
    TO school_safety_user;

GRANT SELECT (notification_enabled),
      UPDATE (notification_enabled)
    ON public.users
    TO school_safety_user;

COMMIT;