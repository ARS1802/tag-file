#!/usr/bin/env bash
source "$(dirname -- "$0")/common.sh"
check_owner
if mysqladmin --defaults-extra-file="$client_config" ping >/dev/null 2>&1; then check_server; exit 0; fi
if [[ -f "$run/mysql.pid" ]] && kill -0 "$(cat "$run/mysql.pid")" 2>/dev/null; then
    echo 'Processo indicado no PID já existe, mas não responde; não foi criado outro servidor.' >&2; exit 9
fi
args=("--defaults-file=$config")
if [[ -f "$run/bootstrap.sql" ]]; then args+=("--init-file=$run/bootstrap.sql"); fi
nohup mysqld "${args[@]}" > "$runtime/logs/launcher.log" 2>&1 < /dev/null &
server_pid=$!
for ((attempt=0; attempt<60; attempt++)); do
    if mysql --defaults-extra-file="$client_config" -e 'SELECT 1' >/dev/null 2>&1; then
        check_server
        rm -f -- "$run/bootstrap.sql"
        exit 0
    fi
    kill -0 "$server_pid" 2>/dev/null || { echo 'Servidor encerrou; consulte database/runtime/logs/mysql.log' >&2; exit 10; }
    sleep 1
done
echo 'Tempo de inicialização excedido; processo/dados preservados para diagnóstico' >&2
exit 11
