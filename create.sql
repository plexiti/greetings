create table GRT_COMMANDS (type varchar(31) not null, ID varchar(36) not null, version integer, FORWARDED_AT timestamp, JSON text not null, CONTEXT varchar(64) not null, NAME varchar(128) not null, STATUS varchar(16) not null, CORRELATION varchar(128) not null, FINISHED_AT timestamp, STARTED_AT timestamp, FINISHED_BY varchar(255), FLOW_ID varchar(255), VALUE_ID varchar(255), ISSUED_AT timestamp not null, PROBLEM_CODE varchar(255), PROBLEM_MESSAGE varchar(255), PROBLEM_OCCURED_AT timestamp, COMPLETED_AT timestamp, TOKEN_ID varchar(255), TRIGGERED_BY varchar(255), primary key (ID))
create table GRT_EVENTS (ID varchar(36) not null, version integer, FORWARDED_AT timestamp, JSON text not null, CONTEXT varchar(64) not null, NAME varchar(128) not null, STATUS varchar(16) not null, AGG_ID varchar(36) not null, AGG_TYPE varchar(128) not null, AGG_VERSION integer not null, CONSUMED_AT timestamp, FLOW_ID varchar(255), PROCESSED_AT timestamp, RAISED_AT timestamp not null, RAISED_DURING varchar(255), primary key (ID))
create table GRT_GREETINGS (ID varchar(36) not null, version integer, CALLER varchar(255), CONTACTS integer, GREETING varchar(255), primary key (ID))
create table GRT_VALUES (HASH varchar(40) not null, version integer, CREATED_AT timestamp not null, JSON text not null, CONTEXT varchar(64) not null, NAME varchar(128) not null, primary key (HASH))