#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../.." && pwd -P)"
runtime="$project_root/database/runtime"
data="$runtime/data"
run="$runtime/run"
config="$runtime/mysql.cnf"
client_config="$runtime/client.cnf"
export PATH="$runtime/mysql/bin:$PATH:/usr/sbin"
export LD_LIBRARY_PATH="$runtime/mysql/lib${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
umask 077

# Reconhece somente a instância deste checkout antes de iniciar/parar processos.
check_owner() {
    [[ -f "$runtime/instance.owner" && "$(cat "$runtime/instance.owner")" == "$project_root" ]] || { echo 'Instância sem identificação deste projeto' >&2; exit 2; }
}
check_server() {
    check_owner
    local observed
    observed="$(mysql --defaults-extra-file="$client_config" --batch --skip-column-names -e 'SELECT @@datadir,@@port')"
    [[ "$observed" == "$data/"$'\t3333' ]] || { echo 'Porta 3333 pertence a outra instância; operação recusada' >&2; exit 3; }
}
