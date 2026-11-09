# Valor launcher

Electron app that installs Minecraft 1.8.9, Forge and Java 8 into `~/.Valor/game` and starts the game with the Valor mod.

- Run from source: `npm install`, then `npm start` (uses the newest `../build/libs/valor-*.jar`).
- Windows installer: built by `.github/workflows/launcher.yml`, which bundles the mod as `resources/valor.jar`.
- The installer puts the launcher in `C:\Users\<name>\.Valor\launcher`. Everything else is in `~/.Valor` too: Electron data in
  `launcher-data`, settings in `launcher.json`, Java in `runtime` and the game in `game`.

## Microsoft accounts

Valor signs in with Microsoft only, so only players who own Minecraft can play. The launcher uses Valor's Azure app
(client ID `f33b6a77-918e-47d4-919c-6d866b6ca339` in `src/auth.js`; a client ID is public, not a secret).

Until Microsoft approves that app for the Minecraft API (form at aka.ms/mce-reviewappid), sign-in stops with
"Valor is waiting for Microsoft to approve it for Minecraft sign-in."

## Dev mode (offline)

For Valor's developers only: create a file or folder named `dev` in `~/.Valor` (`C:\Users\<name>\.Valor\dev`) and restart
the launcher. Settings then shows an **Offline name**, and Launch plays offline with it when no Microsoft account is
signed in (singleplayer and offline-mode servers). `tools/dev_launcher.py` also starts the game offline without the launcher.
