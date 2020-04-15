CREATE USER daily_log_server PASSWORD 'change meeeee';

CREATE DATABASE daily_log;

\c daily_log

CREATE TYPE activity_type AS ENUM ('int', 'bool');

CREATE TABLE users (
       id SERIAL PRIMARY KEY,
       name VARCHAR(100) CHECK (name = '' IS FALSE) UNIQUE
);

CREATE TABLE activities (
       id SERIAL PRIMARY KEY,
       user_id INTEGER REFERENCES users(id),
       name VARCHAR(50) CHECK (name = '' IS FALSE),
       type activity_type,
       UNIQUE (user_id, name)
);

CREATE TABLE logs (
       activity_id INTEGER REFERENCES activities(id),
       date DATE NOT NULL,
       value INTEGER NOT NULL,
       PRIMARY KEY (activity_id, date)
);

GRANT SELECT, UPDATE, INSERT ON ALL TABLES IN SCHEMA public TO daily_log_server;
GRANT UPDATE ON ALL SEQUENCES IN SCHEMA public TO daily_log_server;
