CREATE TABLE users (
    u_id TEXT PRIMARY KEY, -- 245256
    u_name TEXT NOT NULL, -- prayerslayer
    u_email TEXT NOT NULL,
    u_display_name TEXT NOT NULL, -- Nikolaus Piccolotto
    u_plan TEXT DEFAULT 'free',
    u_plan_expires TIMESTAMPTZ,
    u_created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);