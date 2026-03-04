-- Adicionar campos de geolocalização (latitude, longitude, source) nas tabelas de tracking

ALTER TABLE site.tracking_visitors
    ADD COLUMN latitude DECIMAL(10, 7),
    ADD COLUMN longitude DECIMAL(10, 7),
    ADD COLUMN geo_source VARCHAR(10);

ALTER TABLE site.tracking_sessions
    ADD COLUMN latitude DECIMAL(10, 7),
    ADD COLUMN longitude DECIMAL(10, 7),
    ADD COLUMN geo_source VARCHAR(10);
