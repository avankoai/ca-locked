# Plugin Hub release checklist

Owner: `avankoai`. Repository: [avankoai/ca-locked](https://github.com/avankoai/ca-locked).

## Prepared locally

- Standard Java 11 Gradle project and wrapper, including Windows launcher.
- `build=standard` Plugin Hub metadata and a BSD-2-Clause license.
- Launcher/test code excluded from the production JAR.
- Resources loaded from the classpath, not a developer's disk paths.
- No native UI layout dumps or test-allowance switches in production code.
- Account-bound RuneLite configuration with stale-write checks and safe failure on unreadable saves.
- Existing unversioned development saves retain support.

## Required practical acceptance checks

1. Account A: pick and store two known tasks, use a reroll, note the remaining allowance.
2. Log out, log into account B: confirm no tasks or rule choices from A appear.
3. Return to A: confirm the exact active task, storage, rules and rerolls return.
4. Sign in to the intended RuneLite account in the development client, select Save progress, and close normally.
5. On a separate PC/installation, sign into the same RuneLite account and game character.
6. Verify the same challenge state. Make a small change, save/close, and verify the return trip.

The memory-backend tests simulate configuration replacement and a separate reader; they do not prove cloud authentication, successful upload or a second physical PC.

## Submission

1. Publish **only the clean source package** to the public `avankoai/ca-locked` repository. Do not publish the containing home-directory Git repository, development notes, research, player configuration, logs or credentials.
2. Run the build on the published commit and inspect the resulting JAR.
3. Fork `runelite/plugin-hub` and add a single manifest file named `plugins/ca-locked`:

   ```properties
   repository=https://github.com/avankoai/ca-locked.git
   commit=<full 40-character commit SHA that passed checks>
   ```

4. Open one submission pull request. Follow its build feedback and reviewer requests.
5. The plugin becomes normally installable only after RuneLite accepts and publishes it. Do not label an unreviewed build as approved.

Review notes: the dashboard uses the existing game interface group 717 locally. Explicit load-script execution disables packet sending; picking a task does not perform gameplay actions. The native interface and minimap orb should receive specific reviewer attention. Runtime catalogue entries come from the game; bundled dependency rules are only applied to matching entries. Changes to the game cache can require plugin updates.

Submission: https://github.com/runelite/plugin-hub/pull/18957

The author confirmed on 19 October 2026 that the development version works on the second PC after following the synchronization test instructions. Automated storage tests pass; they are separate from this user-reported practical check.
