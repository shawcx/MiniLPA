# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

MiniLPA is a Kotlin/JVM Swing desktop GUI for managing eSIM profiles on eUICC cards over PC/SC. It's a front end to the [lpac](https://github.com/estkme-group/lpac) CLI, which does the real LPA work.

## Build & run

Requirements: JDK 21 (Gradle toolchain; Gradle 9 itself needs JDK 17+) and a PC/SC service at runtime (pcsc-lite on Linux). Everything builds through Gradle; use `gradlew.bat` on Windows.

```sh
./gradlew shadowJar                       # jar -> build/libs/MiniLPA-all.jar
./gradlew shadowJar -Ptarget=all          # standalone jar bundling lpac for every platform
./gradlew jpackage -Ptype=pkg             # native package -> build/dist/MiniLPA-<OS>-<arch>.<ext> (app-image|exe|msi|rpm|deb|pkg|dmg)
./gradlew updateEuiccInfo                 # force-refresh the eUICC manifests
./gradlew run                             # run from source
./gradlew test                            # unit tests (JUnit 5 via kotlin-test)
./gradlew test --tests RedactTest         # a single test class
```

- `setupResources` runs before compilation and resource processing. It:
  - downloads `src/main/resources/eum-accredited.json` and `ci-manifest.json` from osmocom when either one, or `build/euicc_info_update_time`, is missing (these files are gitignored);
  - downloads lpac into `build/lpac/<target>.zip` and checks it against a pinned SHA-256. The release (`lpacVersion`), its creation time (`lpacReleaseTime`) and the per-asset hashes in `build.gradle.kts` are bumped together; the app re-extracts lpac when `LPAC_BUILD_TIME` increases;
  - packs `src/main/languages/*.yaml` into `build/languages.zip`.
- The EUM data is osmocom's `docs/pki/eum/accredited.json` (`{suppliers: [{name, abbr, country, eum: [prefixes]}]}`). It replaced the old `eum/manifest.json`, which is now a 404 and was the only source of the product names (EID pattern → device). The manifests are only fetched at build time; the app itself never downloads them.
- Gradle properties:
  - `-Ptarget=` takes `macos_universal`, `windows_x86`, `windows_aarch64`, `linux_x86`, `linux_aarch64` or `all`. It defaults to the host platform.
  - `-Ptype=`, `-Pnative-wayland`.
  - `-Pskip-setup-resources` skips the network fetches; it only works after one full build.
  - `-Pshort-commit-id=` overrides the commit id, which is otherwise read from git and gets a `-dirty` suffix when there are uncommitted changes.
- jpackage writes to `build/jpackage-output`, which the plugin empties on every run. Finished packages are moved to `build/dist` as `MiniLPA-<OS>-<arch>[-Wayland].<ext>`.
  - An `app-image` build is zipped by `zipAppImage`.
  - On macOS that zip is made with `ditto`. Gradle's `Zip` would replace the symlinks inside the signed `.app` with copies and break the code signature.
- **Dependency verification** is on: `gradle/verification-metadata.xml` pins the SHA-256 of every plugin and dependency.
  - After adding or changing a dependency, regenerate it with `./gradlew --write-verification-metadata sha256 build jpackage -Ptype=app-image`, then review the diff.
  - `./gradlew dependencyUpdates` lists newer versions. It also reports betas and RCs as releases.
- Windows `.msi` builds get a per-commit ProductCode from `scripts/Update-MSI-ProductCode.ps1`, which Gradle runs through Windows PowerShell. It's the only remaining script.
- Tests live in `src/test/kotlin`. Keep them away from the globals in `Main.kt`: initializing those sets up logging and writes settings and language files. There's no lint step.
- CI (`.github/workflows/build.yaml`) runs the tests in the standalone-jar job and builds packages with `./gradlew`: Zulu on Windows/macOS, JetBrains Runtime on Linux. The workflow's token is read-only except in the release job, which runs on `v*` tags and publishes a GitHub release.

## Runtime layout

The app data folder is `~/.minilpa`, unless the `minilpa.app-data-folder` system property sets another. Only `./gradlew run` sets it, to the project folder, so running from source writes `setting.yaml`, `languages/`, `logs/` and `<platform>/lpac` into the repo root; `.gitignore` covers them. Released builds never read from the working directory.

On startup (`Main.kt`), the app extracts the bundled lpac zip, language pack and eUICC manifests into the app data folder when the matching `BuildConfig.*_TIME` is newer than the timestamp stored in `setting.yaml`.

## Architecture

- **Globals in `Main.kt`**: `setting`, `language`, `backend`, `platform`, `appDataFolder`, `yaml` and `json` are top-level values that the whole codebase imports directly. `mainFrame` resolves to `MainFrame.instance`.
- **`lpa/LPABackend`** is the backend interface.
  - `LPACExecutor` is the only implementation. It spawns the `lpac` binary for each command and parses each stdout line as JSON into `model/LPACIO` (types `lpa`/`progress`/`driver`/`apdu`). `progress` messages drive the progress bar and status text.
  - The selected reader goes to lpac through the env var that `driver apdu list` names (currently `LPAC_APDU_PCSC_DRV_IFID`). Debug settings set `LIBEUICC_DEBUG_APDU` and `LIBEUICC_DEBUG_HTTP`.
  - A non-zero lpa `code` raises `LPAOperationFailureException`.
- **Logging**: users attach logs to public issues. Pass anything that could contain an EID, ICCID, matching ID, confirmation code or IMEI through `redact()` or `redactLpacArguments()` in `Tool.kt` before logging it.
- **`lpa/LocalProfileAssistant`** orchestrates user operations. Each one follows the same pattern:
  1. `GlobalScope.launch { mainFrame.freezeWithTimeout(successMsg) { ... } }`. This freezes the UI, shows the progress dialog, and turns exceptions into error dialogs through `MainFrame.tryExecute`.
  2. Set `progressBar.stage` to an explicit step count, then call `progressBar.swipePlusAuto()` after each step. **When you add or remove a step, update the stage count.**
  3. After a mutation, refresh the profile list, the notification list and the chip info.
- **UI** (`ui/`, `ui/component/`) is Swing with FlatLaf themes and MigLayout. The main panels and lists are singletons: their constructor assigns a companion `instance`, as in `ProfileList.instance` and `NotificationList.instance`. `addConstructedHook` defers work until `MainFrame` is built.
- **Settings**: `model/Setting` is a kotlinx-serialization data class persisted as YAML (kaml). Mutate it through `setting.update { ... }`, which saves immediately.
- **i18n**: `model/Language` is a data class whose constructor defaults are the English strings. Translations live in `src/main/languages/<locale>.yaml`, which uses the same kebab-case keys. Missing keys fall back to the English defaults, and `en_US.yaml` is generated from them at runtime. To add a string, add the property with its English default to `Language.kt`, then add the key to every yaml.
- **Vendored code**: `com/github/pgreze/process` is a copy of the kotlin-process library, used to run lpac.
- **Non-public JDK access**: `AwtInternals` (in `MiniDropArea.kt`) writes two `java.awt` fields for Windows focus workarounds.
  - Every launcher opens `java.awt` to the app: the `run` and `test` tasks, the jpackage launchers (`--add-opens`), and the jar manifest (`Add-Opens`).
  - If you add another launch path, open `java.awt` there too.
  - Don't use `sun.misc.Unsafe`.

## Code style

Match the existing style, not the IntelliJ defaults:
- Allman braces for classes and multi-line functions.
- Spaces on both sides of the colon in declarations (`fun foo(a : Int) : String`).
- Backtick kebab-case identifiers for serialized keys (`` setting.`show-details` ``).
- Small helpers from `Tool.kt`: `.cast<T>()`, `.drop()` to discard a value as `Unit`, and `logger()`.
