--liquibase formatted sql
--changeset lbodor:1533698532

update cors_site_network set version=1;
