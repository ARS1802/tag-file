-- MySQL 8.0/8.4. Reaplicação cria ausências, sem apagar/recriar dados existentes.
CREATE TABLE IF NOT EXISTS TAG (
 id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 name VARCHAR(100) NOT NULL,
 color CHAR(7) CHARACTER SET ascii NOT NULL,
 created_at DATETIME(6) NOT NULL,
 last_file_tagged_at DATETIME(6) NULL,
 predefined BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE IF NOT EXISTS LOCAL_FILE (
 id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 path VARCHAR(700) NOT NULL,
 available BOOLEAN NOT NULL,
 size_bytes BIGINT NULL,
 created_at DATETIME(6) NULL,
 modified_at DATETIME(6) NULL,
 last_accessed_at DATETIME(6) NULL,
 UNIQUE KEY unique_path(path),
 CONSTRAINT nonnegative_size CHECK (size_bytes IS NULL OR size_bytes >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE IF NOT EXISTS TAG_EXTENSION (
 tag_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 extension VARCHAR(64) NOT NULL,
 PRIMARY KEY(tag_id,extension),
 FOREIGN KEY(tag_id) REFERENCES TAG(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE IF NOT EXISTS LOCAL_FILE_TAG (
 file_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 tag_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 PRIMARY KEY(file_id,tag_id),
 KEY tag_files(tag_id,file_id),
 FOREIGN KEY(file_id) REFERENCES LOCAL_FILE(id) ON DELETE CASCADE,
 FOREIGN KEY(tag_id) REFERENCES TAG(id) ON DELETE CASCADE
) ENGINE=InnoDB;
-- Não é schema_history: só registra a conclusão da preparação única das predefinidas.
CREATE TABLE IF NOT EXISTS APP_METADATA (
 setting VARCHAR(64) PRIMARY KEY,
 value VARCHAR(255) NOT NULL
) ENGINE=InnoDB;
