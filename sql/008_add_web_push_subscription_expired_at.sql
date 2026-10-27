BEGIN;

-- Pushサービスで無効と判定された日時を保持する
ALTER TABLE public.web_push_subscription
    ADD COLUMN IF NOT EXISTS expired_at
        timestamp with time zone;

COMMIT;