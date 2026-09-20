#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")/.."
if [[ -n "${TAG_FILE_JDK:-}" ]]; then
    javac_command="$TAG_FILE_JDK/bin/javac"
    javadoc_command="$TAG_FILE_JDK/bin/javadoc"
else
    javac_command=javac
    javadoc_command=javadoc
fi
# Confere o compilador antes de apagar uma compilação anterior válida.
if ! compiler_check=$("$javac_command" --release 24 -version 2>&1); then
    printf '%s\n' \
        'Erro: o compilador selecionado não pode compilar o projeto para Java 24.' \
        "Compilador: $javac_command" \
        "$compiler_check" \
        'Use: TAG_FILE_JDK=/caminho/do/jdk-24 bash scripts/build.sh' \
        'Na execução, use o bin/java do mesmo JDK.' \
        'As saídas de compilação não foram alteradas.' >&2
    exit 1
fi
# Evita manter classes e páginas de tipos removidos ou renomeados.
source scripts/elevated-common.sh
trap 'finish_elevated_script "$?" out lib' EXIT
rm -rf -- out/classes out/javadoc
mkdir -p out/classes out/javadoc lib
find src -name '*.java' -type f | sort > out/sources.txt
"$javac_command" --release 24 -encoding UTF-8 -cp 'lib/*' -d out/classes @out/sources.txt
"$javadoc_command" -private -Xdoclint:all -encoding UTF-8 -docencoding UTF-8 -charset UTF-8 -cp 'lib/*' -d out/javadoc @out/sources.txt
