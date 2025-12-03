ALTER TABLE instance
    ADD COLUMN instance_id VARCHAR(100);

ALTER TABLE instance
    ADD CONSTRAINT uc_instance_instance_id UNIQUE (instance_id);