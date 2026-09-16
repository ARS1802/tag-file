#!/usr/bin/env bash
source "$(dirname -- "$0")/common.sh"
bash "$project_root/database/scripts/linux/check.sh"
mkdir -p "$runtime/logs" "$run"
if [[ -d "$data/mysql" ]]; then check_owner; exit 0; fi
mkdir "$run/initialize.lock" || { echo 'Preparação já está em andamento' >&2; exit 7; }
trap 'rmdir "$run/initialize.lock"' EXIT
if [[ -d "$data" && -n "$(ls -A -- "$data")" ]]; then
    echo 'Diretório de dados incompleto ou desconhecido; preservado para diagnóstico.' >&2; exit 8
fi
mkdir -p "$data"
# Substituição literal sem interpretar caracteres do caminho no sed.
while IFS= read -r line; do printf '%s\n' "${line//@ROOT@/$project_root}"; done < "$project_root/database/config/mysql-linux.cnf.template" > "$config"
mysqld --no-defaults --initialize-insecure --datadir="$data" --log-error="$runtime/logs/initialize.log"
printf '%s\n' "$project_root" > "$runtime/instance.owner"
cat > "$client_config" <<'CLIENT'
[client]
host=127.0.0.1
port=3333
protocol=TCP
user=root
password=TagFile123!
connect-timeout=3
CLIENT
cat > "$run/bootstrap.sql" <<'SQL'
ALTER USER 'root'@'localhost' IDENTIFIED BY 'TagFile123!';
CREATE DATABASE IF NOT EXISTS tag_file CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;
SQL
