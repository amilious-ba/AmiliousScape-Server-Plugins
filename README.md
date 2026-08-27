# AmiliousScape Server Plugins

Source overlays for a 2009Scape / AmiliousScape server. These are **not** client plugins and **not** drop-in JARs.

AMP (or the scripts in `scripts/`) copies `src/main/content/amilious/` into:

```text
2009scape/Server/src/main/content/amilious/
```

Then the existing Maven build (`Server/mvnw clean package`) compiles them into `server.jar`.

## Layout

```text
src/main/content/amilious/
  commands/     CommandSet classes (::commands)
  listeners/    InteractionListener, TickListener, etc.
  drops/        custom drop / mystery-box style content
  minigames/    custom activities
scripts/
  overlay-into-server.sh
  overlay-into-server.ps1
```

Package prefix is always `content.amilious.*`. Do not edit upstream files.

## One-time setup (GitHub Desktop + IntelliJ IDEA)

### 1. Clone this repo

GitHub Desktop → File → Clone repository → `amilious-ba/AmiliousScape-Server-Plugins`

Put it next to your server checkout, for example:

```text
C:\dev\2009scape\
C:\dev\AmiliousScape-Server-Plugins\
```

or on Linux:

```text
~/dev/2009scape/
~/dev/AmiliousScape-Server-Plugins/
```

You need a 2009Scape **Server** tree (GitLab `2009scape/2009scape`, or the `2009scape/` folder AMP already cloned).

### 2. Overlay into the server (so IDEA can compile)

PowerShell:

```powershell
cd C:\dev\AmiliousScape-Server-Plugins
.\scripts\overlay-into-server.ps1 C:\dev\2009scape
```

Linux / Git Bash:

```bash
chmod +x scripts/overlay-into-server.sh
./scripts/overlay-into-server.sh ~/dev/2009scape
```

Re-run this after you add or change plugin files, **or** work directly in the server tree and copy back. Source of truth is this repo.

### 3. Open in IntelliJ IDEA (not Rider)

Rider is for the C# launcher. Server plugins are Kotlin/Java — use **IntelliJ IDEA**.

1. File → Open → the **2009scape** repo (the one that contains `Server/pom.xml`), not this plugins repo alone.
2. Wait for Maven import. SDK = **Temurin / JDK 11**.
3. Confirm `Server/src/main/content/amilious/commands/AmiliousCommandSet.kt` is visible after the overlay.
4. Build → Build Project (or the Server Maven `package` run config).
5. Run the Server as you already do.

Optional: File → Project Structure → Modules → Server → + Content Root pointing at `AmiliousScape-Server-Plugins/src/main`. Then IDEA compiles from this repo without copying. You still need the overlay (or AMP stage) before a real AMP build.

### 4. Smoke test

Start the server, log in, type:

```text
::amilious
```

You should see: `Amilious server plugins are loaded.`

If the command is unknown, the overlay did not land before Maven, or the class is missing `@Initializable`.

## Adding a command

Edit or add a Kotlin file under `commands/`:

```kotlin
define("heal", Privilege.ADMIN, "::heal", "Full restore.") { player, _ ->
    player.fullRestore()
    notify(player, "Restored.")
}
```

Privileges: `STANDARD` (everyone), `MODERATOR`, `ADMIN`.

## Adding a listener

New file under `listeners/`, for example:

```kotlin
package content.amilious.listeners

import core.game.interaction.InteractionListener
import core.plugin.Initializable

@Initializable
class ExampleListener : InteractionListener {
    override fun defineListeners() {
        // on(...) / onUseWith(...)
    }
}
```

## AMP later

Insert a stage **after** the template’s `git reset --hard && git clean -fd` clone step and **before** Maven:

1. clone/pull this repo
2. run `overlay-into-server.sh` against `./2009scape`
3. write `.needs_rebuild` so Maven runs

Until that stage exists, develop locally with the overlay script + IDEA.

## License

2009Scape is AGPL. Code compiled into the server should be treated the same. See `LICENSE`.
