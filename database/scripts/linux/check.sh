#!/usr/bin/env bash
source "$(dirname -- "$0")/common.sh"
for binary in mysqld mysql mysqladmin; do
    [[ -x "$runtime/mysql/bin/$binary" ]] || { echo "Componente portátil ausente: $binary (database/runtime/mysql/bin)" >&2; exit 4; }
done
mysqld --version
mysql --version
