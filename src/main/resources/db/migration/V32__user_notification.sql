create table user_notification (
    id          bigserial primary key,
    created_at  timestamp not null,
    updated_at  timestamp not null,
    user_id     bigint not null,
    title       varchar(255) not null,
    message     varchar(2000) not null,
    is_read     boolean not null default false
);

create index idx_user_notification_user_id on user_notification(user_id);
