-- 利用者ごとの位置情報利用設定を追加する。
-- 初期状態はOFFとする。

BEGIN;

ALTER TABLE public.users
    ADD COLUMN location_enabled BOOLEAN NOT NULL DEFAULT FALSE;

COMMIT;