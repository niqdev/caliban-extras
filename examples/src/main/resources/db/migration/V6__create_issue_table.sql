CREATE TABLE IF NOT EXISTS example.issue (
    id UUID NOT NULL DEFAULT RANDOM_UUID(),
    repository_id UUID NOT NULL,
    number INT NOT NULL AUTO_INCREMENT,
    status VARCHAR(10) NOT NULL,
    title VARCHAR(250) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    CONSTRAINT issue_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS issue_repository_id_idx ON example.issue (repository_id);
CREATE INDEX IF NOT EXISTS issue_number_idx ON example.issue (number);
CREATE INDEX IF NOT EXISTS issue_status_idx ON example.issue (status);

ALTER TABLE example.issue ADD FOREIGN KEY (repository_id) REFERENCES example.repository (id) ON DELETE CASCADE;
