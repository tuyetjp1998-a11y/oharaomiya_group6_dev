-- 安否回答に位置情報の項目を追加する。
-- 対象データベース：school_safety_db

BEGIN;

ALTER TABLE public.safety_report
    ADD COLUMN latitude DOUBLE PRECISION,
    ADD COLUMN longitude DOUBLE PRECISION,
    ADD COLUMN location_accuracy DOUBLE PRECISION,
    ADD COLUMN location_acquired_at TIMESTAMPTZ;

ALTER TABLE public.safety_report
    ADD CONSTRAINT safety_report_location_check CHECK (
        (
            latitude IS NULL
            AND longitude IS NULL
            AND location_accuracy IS NULL
            AND location_acquired_at IS NULL
        )
        OR
        (
            latitude IS NOT NULL
            AND longitude IS NOT NULL
            AND location_accuracy IS NOT NULL
            AND location_acquired_at IS NOT NULL
            AND latitude BETWEEN -90 AND 90
            AND longitude BETWEEN -180 AND 180
            AND location_accuracy >= 0
            AND location_accuracy < 'Infinity'::DOUBLE PRECISION
        )
    );

COMMIT;