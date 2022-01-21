create table if not exists inquiry_state
(
    id         serial primary key,
    inquiry_id varchar(255) not null,
    state      varchar(32)  not null
);
