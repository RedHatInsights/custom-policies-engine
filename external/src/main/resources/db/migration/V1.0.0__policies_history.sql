-- temp, TBC
SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;
SET default_tablespace = '';

-- ^ this is useless?

CREATE TABLE event (
    discriminator CHAR(5) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    event_id VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP,
    event_type CHAR(5) NOT NULL,
    ctime BIGINT,
    datasource VARCHAR(255),
    data_id VARCHAR(255),
    category VARCHAR(255),
    text VARCHAR(50011),
    context VARCHAR(50010),
    tags VARCHAR(50009),
    trigger VARCHAR(50008),
    dampening VARCHAR(50007),
    eval_sets TEXT,
    facts TEXT,
    severity VARCHAR(50004),
    status VARCHAR(50003),
    lifecycle VARCHAR(50002),
    resolved_eval_sets VARCHAR(50001),
    CONSTRAINT pk_event PRIMARY KEY (tenant_id, event_id)
);

-- add indexes on everything?
