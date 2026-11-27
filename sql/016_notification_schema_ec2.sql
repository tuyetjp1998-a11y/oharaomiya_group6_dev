-- AnpiLink: notification schema migration for EC2

-- Run as postgres using psql -v ON_ERROR_STOP=1.

-- Existing records are preserved.

BEGIN;

SET LOCAL lock_timeout = '10s';

SET LOCAL search_path = public, pg_catalog;

CREATE TABLE public.safety_dispatch (
    id bigint GENERATED ALWAYS AS IDENTITY,
    sender_user_id bigint NOT NULL,
    target_type varchar(20) NOT NULL,
    title varchar(200) NOT NULL,
    message text NOT NULL,
    deadline_at timestamptz,
    dispatched_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.safety_dispatch_recipient (
    dispatch_id bigint NOT NULL,
    user_id bigint NOT NULL,
    read_at timestamptz
);

CREATE TABLE public.safety_dispatch_reminder (
    id bigint GENERATED ALWAYS AS IDENTITY,
    dispatch_id bigint NOT NULL,
    student_user_id bigint NOT NULL,
    family_user_id bigint NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at timestamptz,
    read_at timestamptz
);

ALTER TABLE public.safety_dispatch ADD CONSTRAINT safety_dispatch_pkey PRIMARY KEY (id);

ALTER TABLE public.safety_dispatch ADD CONSTRAINT ck_dispatch_deadline CHECK (((deadline_at IS NULL) OR (deadline_at > dispatched_at)));

ALTER TABLE public.safety_dispatch ADD CONSTRAINT ck_dispatch_message CHECK ((btrim(message) <> ''::text));

ALTER TABLE public.safety_dispatch ADD CONSTRAINT ck_dispatch_target CHECK (((target_type)::text = ANY ((ARRAY['STUDENT'::character varying, 'FAMILY'::character varying, 'TEACHER'::character varying, 'ALL'::character varying])::text[])));

ALTER TABLE public.safety_dispatch ADD CONSTRAINT ck_dispatch_title CHECK ((btrim((title)::text) <> ''::text));

ALTER TABLE public.safety_dispatch ADD CONSTRAINT safety_dispatch_sender_user_id_fkey FOREIGN KEY (sender_user_id) REFERENCES users(id);

ALTER TABLE public.safety_dispatch_recipient ADD CONSTRAINT safety_dispatch_recipient_pkey PRIMARY KEY (dispatch_id, user_id);

ALTER TABLE public.safety_dispatch_recipient ADD CONSTRAINT safety_dispatch_recipient_dispatch_id_fkey FOREIGN KEY (dispatch_id) REFERENCES safety_dispatch(id) ON DELETE CASCADE;

ALTER TABLE public.safety_dispatch_recipient ADD CONSTRAINT safety_dispatch_recipient_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE public.safety_dispatch_reminder ADD CONSTRAINT safety_dispatch_reminder_pkey PRIMARY KEY (id);

ALTER TABLE public.safety_dispatch_reminder ADD CONSTRAINT fk_reminder_recipient FOREIGN KEY (dispatch_id, student_user_id) REFERENCES safety_dispatch_recipient(dispatch_id, user_id);

ALTER TABLE public.safety_dispatch_reminder ADD CONSTRAINT safety_dispatch_reminder_family_user_id_fkey FOREIGN KEY (family_user_id) REFERENCES users(id);

ALTER TABLE public.safety_dispatch_reminder ADD CONSTRAINT uq_dispatch_reminder UNIQUE (dispatch_id, student_user_id, family_user_id);

ALTER TABLE public.safety_report ADD COLUMN dispatch_id bigint;

ALTER TABLE public.safety_report ALTER COLUMN danger_type TYPE varchar(50);

ALTER TABLE public.safety_report DROP CONSTRAINT ck_safety_report_danger;

ALTER TABLE public.safety_report ADD CONSTRAINT ck_safety_report_danger CHECK (((((status)::text = 'SAFE'::text) AND (danger_type IS NULL)) OR (((status)::text = 'DANGER'::text) AND ((danger_type IS NULL) OR ((danger_type)::text = ANY ((ARRAY['ACCIDENT'::character varying, 'EARTHQUAKE'::character varying, 'FIRE'::character varying, 'LANDSLIDE'::character varying, 'STALKING'::character varying])::text[])))) OR (((status)::text = 'WORRIED'::text) AND ((danger_type IS NULL) OR ((char_length(TRIM(BOTH FROM danger_type)) >= 1) AND (char_length(TRIM(BOTH FROM danger_type)) <= 50))))));

ALTER TABLE public.safety_report ADD CONSTRAINT fk_report_dispatch_recipient FOREIGN KEY (dispatch_id, user_id) REFERENCES safety_dispatch_recipient(dispatch_id, user_id);

CREATE INDEX ix_safety_dispatch_date ON public.safety_dispatch USING btree (dispatched_at DESC);

CREATE INDEX ix_dispatch_recipient_user ON public.safety_dispatch_recipient USING btree (user_id, dispatch_id);

CREATE INDEX ix_dispatch_reminder_family ON public.safety_dispatch_reminder USING btree (family_user_id, created_at DESC);

CREATE INDEX ix_report_dispatch_user ON public.safety_report USING btree (dispatch_id, user_id);

GRANT SELECT, INSERT ON public.safety_dispatch TO school_safety;

GRANT SELECT, INSERT ON public.safety_dispatch_recipient TO school_safety;

GRANT UPDATE (read_at) ON public.safety_dispatch_recipient TO school_safety;

GRANT SELECT, INSERT ON public.safety_dispatch_reminder TO school_safety;

GRANT UPDATE (sent_at, read_at) ON public.safety_dispatch_reminder TO school_safety;

GRANT USAGE, SELECT ON SEQUENCE public.safety_dispatch_id_seq, public.safety_dispatch_reminder_id_seq TO school_safety;

COMMIT;
