--liquibase formatted sql
--changeset lbodor:ed28f1be-2624-4a80-8407-3875eb79e44b

update setup set type = 'CorsSetup' where type = 'GNSS CORS Setup';
