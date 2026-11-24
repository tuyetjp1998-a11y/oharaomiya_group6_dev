BEGIN;

-- 却下理由を保護者の申請履歴に表示するために保存する。
ALTER TABLE public.family_student_request
    ADD COLUMN IF NOT EXISTS rejection_reason varchar(300);

GRANT SELECT, UPDATE
    ON TABLE public.family_student_request
    TO school_safety_user;

COMMIT;
