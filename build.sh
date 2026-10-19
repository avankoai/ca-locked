#!/bin/zsh
set -eu
cd "${0:A:h}"
# Developer fallback using an existing RuneLite launcher installation.
# Release builds use ./gradlew clean build on Windows, macOS or Linux.
repoDir="${RUNELITE_REPOSITORY:-$HOME/.runelite/repository2}"
CP="$(find "$repoDir" -maxdepth 1 -name '*.jar' ! -name '*1.12.39*' -print | paste -sd ':' -)"
rm -rf build/classes build/test-classes
mkdir -p build/classes build/test-classes
javac --release 11 -cp "$CP" -d build/classes $(find src/main/java -name '*.java')
cp -R src/main/resources/. build/classes/
javac --release 11 -cp "build/classes:$CP" -d build/test-classes $(find src/test/java -name '*.java')
for checkClass in ChallengeTest ChallengeStoreTest DependencyRulesTest KillCountRiskTest; do
  java -ea -Djava.awt.headless=true -cp "build/test-classes:build/classes:$CP" "com.vannaka.calocked.$checkClass"
done
jar cf build/ca-locked.jar -C build/classes .
