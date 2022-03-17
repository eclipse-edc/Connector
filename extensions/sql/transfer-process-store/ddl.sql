create table lease
(
    leased_by      varchar               not null,
    leased_at      bigint,
    lease_duration integer default 60000 not null,
    lease_id       varchar               not null
        constraint lease_pk
            primary key
);

comment on column lease.leased_at is 'posix timestamp of lease';

comment on column lease.lease_duration is 'duration of lease in milliseconds';

alter table lease
    owner to "user";

create table transfer_process
(
    id                       varchar           not null
        constraint transfer_process_pk
            primary key,
    type                     integer default 0 not null,
    state                    integer           not null,
    state_count              integer default 0 not null,
    state_time_stamp         timestamp,
    trace_context            varchar,
    error_detail             varchar,
    resource_manifest        varchar,
    provisioned_resource_set varchar,
    lease_id                 varchar
        constraint transfer_process_lease_lease_id_fk
            references lease
            on delete set null
);

comment on column transfer_process.trace_context is 'Java Map serialized as JSON';

comment on column transfer_process.resource_manifest is 'java ResourceManifest serialized as JSON';

comment on column transfer_process.provisioned_resource_set is 'ProvisionedResourceSet serialized as JSON';

alter table transfer_process
    owner to "user";

create unique index transfer_process_id_uindex
    on transfer_process (id);

create table data_request
(
    id                  varchar                                            not null
        constraint data_request_pk
            primary key,
    process_id          varchar                                            not null,
    connector_address   varchar                                            not null,
    protocol            varchar default 'ids-multipart'::character varying not null,
    connector_id        varchar,
    asset_id            varchar                                            not null,
    contract_id         varchar                                            not null,
    data_destination    varchar                                            not null,
    managed_resources   boolean default true,
    properties          varchar,
    transfer_type       varchar,
    transfer_process_id varchar                                            not null
        constraint data_request_transfer_process_id_fk
            references transfer_process
            on update restrict on delete cascade
);

comment on column data_request.data_destination is 'DataAddress serialized as JSON';

comment on column data_request.properties is 'java Map serialized as JSON';

comment on column data_request.transfer_type is 'TransferType serialized as JSON';

alter table data_request
    owner to "user";

create unique index data_request_id_uindex
    on data_request (id);

create unique index lease_lease_id_uindex
    on lease (lease_id);

