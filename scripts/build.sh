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
mkdir -p out/classes out/javadoc lib
find src -name '*.java' -type f | sort > out/sources.txt
"$javac_command" --release 24 -encoding UTF-8 -cp 'lib/*' -d out/classes @out/sources.txt
"$javadoc_command" -private -Xdoclint:all -encoding UTF-8 -docencoding UTF-8 -charset UTF-8 -cp 'lib/*' -d out/javadoc @out/sources.txt
