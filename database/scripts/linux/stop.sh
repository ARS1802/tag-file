#!/usr/bin/env bash
source "$(dirname -- "$0")/common.sh"
check_server
[[ -f "$run/mysql.pid" ]] || { echo 'PID da instância ausente; parada recusada' >&2; exit 12; }
server_pid="$(cat "$run/mysql.pid")"
[[ "$server_pid" =~ ^[0-9]+$ ]] || exit 12
tr '\0' '\n' < "/proc/$server_pid/cmdline" | grep -F -x -- "--defaults-file=$config" >/dev/null || { echo 'Processo não identificado como Tag-File' >&2; exit 12; }
mysqladmin --defaults-extra-file="$client_config" shutdown
