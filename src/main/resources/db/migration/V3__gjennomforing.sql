create table gjennomforing
(
    id             uuid primary key,
    tiltakstype_id uuid                     not null references tiltakstype (id),
    deltidsprosent decimal,
    opprettet      timestamp with time zone not null default current_timestamp,
    sist_endret    timestamp with time zone not null default current_timestamp
);
