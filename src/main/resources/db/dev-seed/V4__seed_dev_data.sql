-- Dev/local seed data so the app is usable immediately after `docker compose up` + `mvnw spring-boot:run`.
-- All seeded users share the password: Passw0rd!

insert into app_user (created_at, updated_at, email, password_hash, full_name, role, status)
values
    (now(), now(), 'admin@nitelestate.local', '$2a$10$aa6Gsh8.BNujyWac7BYteuaimPRGFNoCDi1R8CFNI.j278u1FQ4Ha', 'Super Admin', 'SUPER_ADMIN', 'ACTIVE'),
    (now(), now(), 'cda@nitelestate.local', '$2a$10$aa6Gsh8.BNujyWac7BYteuaimPRGFNoCDi1R8CFNI.j278u1FQ4Ha', 'CDA Administrator', 'CDA_ADMIN', 'ACTIVE'),
    (now(), now(), 'treasurer@nitelestate.local', '$2a$10$aa6Gsh8.BNujyWac7BYteuaimPRGFNoCDi1R8CFNI.j278u1FQ4Ha', 'Estate Treasurer', 'TREASURER', 'ACTIVE'),
    (now(), now(), 'security@nitelestate.local', '$2a$10$aa6Gsh8.BNujyWac7BYteuaimPRGFNoCDi1R8CFNI.j278u1FQ4Ha', 'Security Officer', 'SECURITY', 'ACTIVE');

insert into levy (created_at, updated_at, name, amount, frequency, active)
values
    (now(), now(), 'Estate Levy', 100000.00, 'ANNUAL', true),
    (now(), now(), 'Security Levy', 30000.00, 'ANNUAL', true),
    (now(), now(), 'Waste Levy', 20000.00, 'ANNUAL', true),
    (now(), now(), 'Development Levy', 50000.00, 'ANNUAL', true);

insert into gate (created_at, updated_at, name, code, location, type, status)
values (now(), now(), 'Main Gate', 'MAIN-GATE-01', 'Estate main entrance', 'VEHICLE', 'ACTIVE');

-- Sample resident (Mr. Adewale, House 24B) with a portal login, per the worked example in the spec.
insert into property (created_at, updated_at, block, plot, house_number, address, property_type, occupancy_status)
values (now(), now(), 'Block C', 'Plot 124', 'House 24B', 'House 24B, Nitel Estate', 'DETACHED_HOUSE', 'OCCUPIED');

insert into resident (created_at, updated_at, full_name, phone, email, property_id, resident_type, status, registration_date)
select now(), now(), 'Mr. Adewale', '+2348000000001', 'resident@nitelestate.local', p.id, 'OWNER', 'ACTIVE', current_date
from property p where p.house_number = 'House 24B';

update property set owner_id = (select id from resident where email = 'resident@nitelestate.local')
where house_number = 'House 24B';

insert into app_user (created_at, updated_at, email, password_hash, full_name, role, status, resident_id)
select now(), now(), 'resident@nitelestate.local', '$2a$10$aa6Gsh8.BNujyWac7BYteuaimPRGFNoCDi1R8CFNI.j278u1FQ4Ha',
       'Mr. Adewale', 'RESIDENT', 'ACTIVE', r.id
from resident r where r.email = 'resident@nitelestate.local';
