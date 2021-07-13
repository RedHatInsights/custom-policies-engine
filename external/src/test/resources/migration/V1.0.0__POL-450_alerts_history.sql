-- POL-450
-- This project uses the `policies_ui_backend` database but its schema is actually managed by the policies-ui-backend project.
-- We need that script to run standalone policies-engine tests.

create table alerts_history (
    id uuid not null,
    tenant_id varchar(255) not null,
    policy_id varchar(255) not null,
    ctime bigint not null,
    host_id varchar(255),
    host_name varchar(255),
    constraint pk_alerts_history primary key (id)
);
