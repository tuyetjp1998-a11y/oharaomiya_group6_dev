-- 保護者と生徒の関連付け
CREATE TABLE IF NOT EXISTS public.family_student (
    family_user_id bigint NOT NULL,
    student_user_id bigint NOT NULL,

    CONSTRAINT family_student_pkey
        PRIMARY KEY (family_user_id, student_user_id),

    CONSTRAINT fk_family_student_family
        FOREIGN KEY (family_user_id)
        REFERENCES public.users (id),

    CONSTRAINT fk_family_student_student
        FOREIGN KEY (student_user_id)
        REFERENCES public.users (id),

    CONSTRAINT ck_family_student_different_users
        CHECK (family_user_id <> student_user_id)
);

-- 生徒から保護者を検索するためのインデックス
CREATE INDEX IF NOT EXISTS idx_family_student_student
    ON public.family_student (student_user_id);
    
-- アプリケーション用ユーザーに参照権限を付与する
GRANT SELECT ON TABLE public.family_student
TO school_safety_user;