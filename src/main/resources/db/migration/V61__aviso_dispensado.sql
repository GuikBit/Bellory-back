CREATE TABLE IF NOT EXISTS app.aviso_dispensado (
    id              BIGSERIAL PRIMARY KEY,
    organizacao_id  BIGINT       NOT NULL REFERENCES app.organizacao(id),
    usuario_id      BIGINT       NOT NULL,
    aviso_id        VARCHAR(100) NOT NULL,
    dt_dispensado   TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uk_aviso_dispensado UNIQUE (organizacao_id, usuario_id, aviso_id)
);

CREATE INDEX idx_aviso_disp_org_user ON app.aviso_dispensado (organizacao_id, usuario_id);
