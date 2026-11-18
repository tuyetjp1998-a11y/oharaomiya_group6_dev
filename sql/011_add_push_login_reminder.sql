BEGIN;

-- 通知登録と、この端末のログイン維持用トークンを関連付ける
ALTER TABLE public.web_push_subscription
    ADD COLUMN login_token_id bigint;

-- ログイン期限の通知を最後に送信した日付（日本時間）
ALTER TABLE public.web_push_subscription
    ADD COLUMN last_login_reminder_on date;

-- トークン削除後も、既存の通知登録情報は保持する
ALTER TABLE public.web_push_subscription
    ADD CONSTRAINT fk_push_subscription_login_token
    FOREIGN KEY (login_token_id)
    REFERENCES public.login_token(id)
    ON DELETE SET NULL;

CREATE INDEX idx_push_subscription_login_token
    ON public.web_push_subscription(login_token_id);

COMMIT;