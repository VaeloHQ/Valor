# Valor launcher

Electron app that installs Minecraft 1.8.9, Forge and Java 8 into `~/.Valor/game` and starts the game with the Valor mod.

- Run from source: `npm install`, then `npm start` (uses the newest `../build/libs/valor-*.jar`).
- Windows installer: built by `.github/workflows/launcher.yml`, which bundles the mod as `resources/valor.jar`.

## Microsoft accounts

Microsoft sign-in needs Valor's own Azure app:

1. In portal.azure.com, register an app for **personal Microsoft accounts**.
2. Add the redirect URI `https://login.microsoftonline.com/common/oauth2/nativeclient` under **Mobile and desktop applications**.
3. Ask Microsoft to allow it to use the Minecraft API with their form at aka.ms/mce-reviewappid (approval can take days).
4. Paste the app's client ID in the launcher under Settings, Microsoft sign-in.

Offline accounts work without any of this (singleplayer and offline-mode servers).
