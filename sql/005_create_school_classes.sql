BEGIN;

-- クラス情報
CREATE TABLE public.school_class (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_year integer NOT NULL,
    class_name varchar(100) NOT NULL,

    CONSTRAINT uq_school_class_year_name
        UNIQUE (school_year, class_name),

    -- 生徒所属テーブルから年度も含めて参照する
    CONSTRAINT uq_school_class_id_year
        UNIQUE (id, school_year),

    CONSTRAINT ck_school_class_year
        CHECK (school_year BETWEEN 2000 AND 9999),

    CONSTRAINT ck_school_class_name
        CHECK (btrim(class_name) <> '')
);

-- 生徒の所属クラス
CREATE TABLE public.student_class (
    student_user_id bigint NOT NULL,
    school_year integer NOT NULL,
    class_id bigint NOT NULL,

    -- 生徒は同じ年度に1つのクラスに所属する
    CONSTRAINT student_class_pkey
        PRIMARY KEY (student_user_id, school_year),

    CONSTRAINT fk_student_class_student
        FOREIGN KEY (student_user_id)
        REFERENCES public.users (id),

    CONSTRAINT fk_student_class_class
        FOREIGN KEY (class_id, school_year)
        REFERENCES public.school_class (id, school_year)
);

CREATE INDEX idx_student_class_class
    ON public.student_class (class_id);

-- クラスを担当する教員
CREATE TABLE public.teacher_class (
    teacher_user_id bigint NOT NULL,
    class_id bigint NOT NULL,

    CONSTRAINT teacher_class_pkey
        PRIMARY KEY (teacher_user_id, class_id),

    CONSTRAINT fk_teacher_class_teacher
        FOREIGN KEY (teacher_user_id)
        REFERENCES public.users (id),

    CONSTRAINT fk_teacher_class_class
        FOREIGN KEY (class_id)
        REFERENCES public.school_class (id)
);

CREATE INDEX idx_teacher_class_class
    ON public.teacher_class (class_id);

-- ローカル環境のアプリケーション用ユーザーに参照権限を付与
GRANT SELECT ON TABLE
    public.school_class,
    public.student_class,
    public.teacher_class
TO school_safety_user;

COMMIT;