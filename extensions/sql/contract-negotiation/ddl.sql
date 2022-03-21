create table if not exists contract_agreement
(
    id                varchar not null
        constraint contract_agreement_pk
            primary key,
    provider_agent_id varchar,
    consumer_agent_id varchar,
    signing_date      bigint,
    start_date        bigint,
    end_date          integer,
    asset_id          varchar not null,
    policy_id         varchar
);

alter table contract_agreement
    owner to "user";

create table if not exists contract_negotiation
(
    id                    varchar                                            not null
        constraint contract_negotiation_pk
            primary key,
    correlation_id        varchar                                            not null,
    counterparty_id       varchar                                            not null,
    counterparty_address  varchar                                            not null,
    protocol              varchar default 'ids-multipart'::character varying not null,
    type                  integer default 0                                  not null,
    state                 integer default 0                                  not null,
    state_count           integer default 0,
    state_timestamp       bigint,
    error_detail          varchar,
    contract_agreement_id varchar
        constraint contract_negotiation_contract_agreement_id_fk
            references contract_agreement,
    contract_offers       varchar,
    trace_context         varchar,
    lease_id              varchar
        constraint contract_negotiation_lease_lease_id_fk
            references lease
            on delete set null
);

comment on column contract_negotiation.contract_agreement_id is 'ContractAgreement serialized as JSON';

comment on column contract_negotiation.contract_offers is 'List<ContractOffer> serialized as JSON';

comment on column contract_negotiation.trace_context is 'Map<String,String> serialized as JSON';

alter table contract_negotiation
    owner to "user";

create index if not exists contract_negotiation_correlationid_index
    on contract_negotiation (correlation_id);

create unique index if not exists contract_negotiation_id_uindex
    on contract_negotiation (id);

create unique index if not exists contract_agreement_id_uindex
    on contract_agreement (id);

