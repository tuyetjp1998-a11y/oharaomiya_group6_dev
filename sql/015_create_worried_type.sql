BEGIN;

-- 生徒が「困っている」で選択する全校共通の項目。
CREATE TABLE IF NOT EXISTS public.worried_type (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code varchar(50) NOT NULL,
    name varchar(30) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_by bigint,
    deleted_at timestamp with time zone,

    CONSTRAINT uq_worried_type_code UNIQUE (code),
    CONSTRAINT fk_worried_type_created_by
        FOREIGN KEY (created_by) REFERENCES public.users (id),
    CONSTRAINT fk_worried_type_deleted_by
        FOREIGN KEY (deleted_by) REFERENCES public.users (id),
    CONSTRAINT ck_worried_type_name
        CHECK (char_length(btrim(name)) BETWEEN 1 AND 30),
    CONSTRAINT ck_worried_type_deleted
        CHECK (
            (active = true AND deleted_by IS NULL AND deleted_at IS NULL)
            OR
            (active = false AND deleted_by IS NOT NULL AND deleted_at IS NOT NULL)
        )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_worried_type_active_name
    ON public.worried_type (lower(btrim(name)))
    WHERE active = true;

-- 現在のアプリで使っている初期項目。
INSERT INTO public.worried_type (code, name)
VALUES
    ('ACCIDENT', '事故'),
    ('EARTHQUAKE', '地震'),
    ('FIRE', '火災'),
    ('LANDSLIDE', '土砂崩れ'),
    ('HEATSTROKE', '熱中症')
ON CONFLICT (code) DO NOTHING;

GRANT SELECT, INSERT, UPDATE
    ON TABLE public.worried_type
    TO school_safety_user;

GRANT USAGE, SELECT
    ON SEQUENCE public.worried_type_id_seq
    TO school_safety_user;

COMMIT;
