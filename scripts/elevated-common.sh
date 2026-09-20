#!/usr/bin/env bash

# Usado como trap EXIT: devolve à conta solicitante somente as saídas indicadas.
# PKEXEC_UID é definido pelo próprio pkexec, sem receber senha na aplicação.
finish_elevated_script() {
    local script_status="$1"
    shift
    if [[ "$EUID" -eq 0 && "${PKEXEC_UID:-}" =~ ^[0-9]+$ && "${PKEXEC_UID:-0}" -ne 0 ]]; then
        local original_group output_target
        if original_group=$(id -g -- "$PKEXEC_UID"); then
            for output_target in "$@"; do
                if [[ -e "$output_target" || -L "$output_target" ]]; then
                    if ! chown -R -h -P --preserve-root -- "$PKEXEC_UID:$original_group" "$output_target"; then
                        printf 'Não foi possível restaurar o proprietário de %s\n' "$output_target" >&2
                        script_status=1
                    fi
                fi
            done
        else
            printf 'Conta solicitante da elevação não encontrada.\n' >&2
            script_status=1
        fi
    fi
    exit "$script_status"
}
