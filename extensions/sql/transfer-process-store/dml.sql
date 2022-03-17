-- find by id
select t.*
from transfer_process t
where t.id='test-id1';

-- find by drq id
select t.* from transfer_process t
where t.id=
      (select d.transfer_process_id
        from data_request d
        where d.process_id = 'test-pid2');

-- create
insert into transfer_process (id, state, state_time_stamp, trace_context, error_detail, resource_manifest,
                              provisioned_resource_set)
values ('test-id2', 400, now(), null, null, null, null);


insert into data_request (id, process_id, connector_address, connector_id, asset_id, contract_id, data_destination,
                          properties, transfer_type, transfer_process_id)
values ('test-drq-2', 'test-pid2', 'http://anotherconnector.com', 'anotherconnector', 'asset2', 'contract2', '{}', null,
        default, 'test-id2');

-- delete by id
delete
from transfer_process
where id = 'test-id2';


-- update
update transfer_process t
set state = 800
where id = 'test-id2';


-- next for state: select and write lease
-- select TPs with no or expired lease
select * from transfer_process where lease_id is null or lease_id in (select lease_id from lease where (NOW > (leased_at + lease.lease_duration));

-- create lease
insert into lease (lease_id, leased_by, leased_at, lease_duration) values ('lease-1', 'yomama', NOW), default);

-- update previously selected TPs
update transfer_process t
set lease_id='lease-1'
where id in ('test-id2');