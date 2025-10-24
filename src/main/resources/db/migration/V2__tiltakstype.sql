create table tiltakstype
(
    id          uuid primary key,
    navn        varchar                  not null,
    tiltakskode varchar                  not null,
    arenakode   varchar,
    opprettet   timestamp with time zone not null default current_timestamp,
    sist_endret timestamp with time zone not null default current_timestamp,
    unique (tiltakskode)
);
