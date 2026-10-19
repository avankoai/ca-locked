# CA Locked

A RuneLite combat achievement task picker. Choose a challenge, pick a task and work towards completing it. Other achievements earned along the way count too.

## Availability

Prepared for a RuneLite Plugin Hub submission. **Not yet listed or approved.** Once accepted, players can install CA Locked through RuneLite's Plugin Hub on Windows, macOS or Linux. Installing a development JAR is not the normal player installation flow.

## Playing

Create a challenge in four steps: game mode, rerolls, storage and a review of your rules. The rules stay fixed until you end the challenge.

- **CA Locked (default):** complete at least half the Easy tasks to unlock Medium. Each later tier needs 50% of the previous tier and 100% of the tier before that.
- **All Tiers:** pick from unfinished tasks across all tiers.
- **One Tier:** pick tasks from a difficulty you choose.
- **Tier by Tier:** finish a tier before starting the next.

Rerolling and storage are optional. A reroll limit is per tier and never replenishes during the challenge. Set 0 for unlimited rerolls. Storage has one slot per tier; swapping can be enabled or disabled when starting the challenge.

Open the dashboard using the combat achievement orb beside the minimap. Ready tiers glow until you unlock them. The task list supports search, filters and task descriptions.

## Account progress and another PC

Challenge state is stored using RuneLite's **RuneScape-profile configuration**, keyed to the game character rather than its display name. It includes the active task, storage, rerolls, mode, rule choices and activated tiers. Real achievement completion comes from the game.

RuneLite provides the cloud transport for RuneScape-profile configuration. No separate CA Locked server, password or Jagex login is required by this plugin. RuneLite sign-in and Jagex sign-in are different things.

To continue on another computer:

1. Sign in to the **same RuneLite account** on both computers.
2. On the first computer, open Settings and select **Save progress**. This asks RuneLite to save and sync pending changes.
3. Close the first client normally before continuing on the other computer. Use one client at a time; this is not a concurrent multiplayer save system.
4. Start RuneLite on the other computer, install CA Locked once it is available, and log into the **same game character**.
5. Check the active task, stored task and remaining rerolls before continuing.

Without RuneLite sign-in, progress is local to that installation. The plugin does not claim a cloud upload has succeeded merely because you clicked Save progress. Actual transfer between two logged-in PCs still needs a practical acceptance test.

Switching RuneLite's cosmetic/settings profile does not create a second challenge for the same character: character progress lives in RuneLite's shared RuneScape-profile storage.

The storage layer refuses stale writes after configuration changes. Invalid or newer-version saves are left untouched rather than replaced with an empty challenge.

### Existing development-client saves

The development client has a separate `test-home` directory. Its challenge data is not automatically copied into a normal RuneLite installation. Sign in to the intended RuneLite account **in the development client**, save progress, and close it before testing the normal client. Verify the transfer before retiring the development installation. No saved player data, login files or logs are included in the release source package.

## Development

Use Java 11 or later and the included Gradle wrapper:

```sh
./gradlew clean build
./gradlew run
```

On Windows use `gradlew.bat`. `run` launches a development client. A Jagex account needs the standard RuneLite development-client setup; never commit its credentials.

The build uses RuneLite's latest release by default. To check a specific version, pass `-PruneLiteVersion=<version>`.

The normal plugin JAR contains only `src/main` classes and resources. The launcher and test harnesses stay in `src/test`. Checks cover task progression, rules, storage, rerolls, game messages, account isolation, stale-save protection, legacy saves and corrupt/future save protection. These checks do not connect to the cloud service or replace testing on two real PCs.

## Submission

See [the release checklist](docs/PLUGIN-HUB.md) for the remaining account-transfer checks and Plugin Hub submission steps.

Official references:

- [RuneLite Plugin Hub submission guide](https://github.com/runelite/plugin-hub#submitting-a-plugin)
- [RuneLite configuration implementation](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/config/ConfigManager.java)

CA Locked is an independent RuneLite plugin. Old School RuneScape and game assets belong to Jagex; RuneLite's plugin review is separate from Jagex endorsement.
