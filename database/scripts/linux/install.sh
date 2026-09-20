#!/usr/bin/env bash
source "$(dirname -- "$0")/common.sh"
[[ "${1:-}" == --authorized ]] || { echo 'Instalação não autorizada' >&2; exit 5; }
source "$project_root/scripts/elevated-common.sh"
installation_outputs=()
trap 'finish_elevated_script "$?" "${installation_outputs[@]}"' EXIT
# A distribuição portátil não registra serviço nem redefine contas de outras instâncias.
[[ "$(uname -m)" == x86_64 ]] || { echo 'Este pacote requer Linux x86_64' >&2; exit 6; }
if [[ ! -d "$runtime" ]]; then installation_outputs+=("$runtime"); fi
mkdir -p "$runtime"
if [[ -d "$runtime/mysql" ]]; then
    bash "$project_root/database/scripts/linux/check.sh"
    exit 0
fi
archive="$runtime/mysql-linux.tar.xz"
installation_outputs+=("$archive" "$runtime/mysql-linux.download.sha256")
curl -fL --retry 2 --connect-timeout 15 -o "$archive" 'https://dev.mysql.com/get/Downloads/MySQL-8.4/mysql-8.4.9-linux-glibc2.28-x86_64-minimal.tar.xz'
staging="$(mktemp -d "$runtime/extract-XXXXXX")"
installation_outputs+=("$staging")
tar -xJf "$archive" -C "$staging" --strip-components=1
[[ -x "$staging/bin/mysqld" ]] || { echo 'Pacote não contém servidor esperado' >&2; exit 6; }
mkdir -p "$staging/lib"
# Ubuntu 24/Mint 22 renomeiam o SONAME para t64; em x86_64 a ABI continua compatível.
if [[ -f /lib/x86_64-linux-gnu/libaio.so.1t64 ]]; then
    ln -s /lib/x86_64-linux-gnu/libaio.so.1t64 "$staging/lib/libaio.so.1"
fi
LD_LIBRARY_PATH="$staging/lib${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}" "$staging/bin/mysqld" --version
mv -- "$staging" "$runtime/mysql"
installation_outputs+=("$runtime/mysql")
sha256sum "$archive" > "$runtime/mysql-linux.download.sha256"
bash "$project_root/database/scripts/linux/check.sh"
