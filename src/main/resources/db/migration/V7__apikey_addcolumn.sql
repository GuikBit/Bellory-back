ALTER TABLE app.api_keys
    ADD apikey VARCHAR(128);

ALTER TABLE app.api_keys
    ALTER COLUMN apikey SET NOT NULL;

ALTER TABLE app.api_keys
    ADD CONSTRAINT uc_api_keys_apikey UNIQUE (apikey);

ALTER TABLE app.api_keys
    ADD CONSTRAINT uc_api_keys_keyhash UNIQUE (key_hash);

ALTER TABLE app.api_keys
    ADD CONSTRAINT FK_API_KEYS_ON_ORGANIZACAO FOREIGN KEY (organizacao_id) REFERENCES app.organizacao (id);
