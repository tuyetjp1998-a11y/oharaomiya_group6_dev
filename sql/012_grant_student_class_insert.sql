-- 担当クラスへの生徒追加に必要な登録権限
GRANT INSERT (student_user_id, school_year, class_id)
ON public.student_class TO school_safety_user;
