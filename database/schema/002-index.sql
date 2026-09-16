-- DatabaseManager verifica INFORMATION_SCHEMA antes de aplicar esta alteração.
CREATE INDEX available_files ON LOCAL_FILE(available);
