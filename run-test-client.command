#!/bin/zsh
set -eu
cd "${0:A:h}"
./build.sh
mkdir -p test-home/.runelite
CP="$(find "${RUNELITE_REPOSITORY:-$HOME/.runelite/repository2}" -maxdepth 1 -name '*.jar' ! -name '*1.12.39*' -print | paste -sd ':' -)"
exec java -ea --add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED -Duser.home="$PWD/test-home" -Dcalocked.project="$PWD" -cp "build/test-classes:build/classes:$CP" com.vannaka.calocked.Launcher --developer-mode --debug
