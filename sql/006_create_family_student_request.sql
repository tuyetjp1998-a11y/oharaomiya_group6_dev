BEGIN;

-- 保護者と生徒の関連付け申請
CREATE TABLE public.family_student_request (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    family_user_id bigint NOT NULL,
    student_user_id bigint NOT NULL,

    -- PENDING：承認待ち、APPROVED：承認済み、REJECTED：却下
    status varchar(20) NOT NULL DEFAULT 'PENDING',

    requested_at timestamp with time zone
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    reviewed_by bigint,
    reviewed_at timestamp with time zone,

    CONSTRAINT fk_family_request_family
        FOREIGN KEY (family_user_id)
        REFERENCES public.users (id),

    CONSTRAINT fk_family_request_student
        FOREIGN KEY (student_user_id)
        REFERENCES public.users (id),

    CONSTRAINT fk_family_request_reviewer
        FOREIGN KEY (reviewed_by)
        REFERENCES public.users (id),

    CONSTRAINT ck_family_request_different_users
        CHECK (family_user_id <> student_user_id),

    CONSTRAINT ck_family_request_status
        CHECK (status IN (
            'PENDING',
            'APPROVED',
            'REJECTED'
        )),

    CONSTRAINT ck_family_request_review
        CHECK (
            (
                status = 'PENDING'
                AND reviewed_by IS NULL
                AND reviewed_at IS NULL
            )
            OR
            (
                status IN ('APPROVED', 'REJECTED')
                AND reviewed_by IS NOT NULL
                AND reviewed_at IS NOT NULL
                AND reviewed_at >= requested_at
            )
        )
);

-- 同じ親子の承認待ち申請を重複させない
CREATE UNIQUE INDEX uq_family_request_pending
    ON public.family_student_request (
        family_user_id,
        student_user_id
    )
    WHERE status = 'PENDING';

-- 保護者の申請履歴を取得する
CREATE INDEX idx_family_request_family_time
    ON public.family_student_request (
        family_user_id,
        requested_at DESC,
        id DESC
    );

-- 対象生徒の承認待ち申請を取得する
CREATE INDEX idx_family_request_pending_student
    ON public.family_student_request (student_user_id)
    WHERE status = 'PENDING';

-- ローカル環境のアプリケーション用ユーザーに権限を付与
GRANT SELECT, INSERT, UPDATE
    ON TABLE public.family_student_request
    TO school_safety_user;

GRANT USAGE, SELECT
    ON SEQUENCE public.family_student_request_id_seq
    TO school_safety_user;

-- 承認時に親子の関連付けを登録する
GRANT INSERT
    ON TABLE public.family_student
    TO school_safety_user;

COMMIT;