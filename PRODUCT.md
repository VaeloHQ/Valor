# Product

<!-- impeccable:product-schema 1 -->

## Platform

desktop: Minecraft Java 1.8.9 Forge mod (Mixin). Screens are drawn in OpenGL inside the game window, not in a browser.

## Users

Minecraft 1.8.9 PvP players. Many play on weak laptops: the owner's laptop cannot run modern-OpenGL versions of Minecraft, which is why the client targets 1.8.9.

## Product Purpose

Valor is a client in the style of Lunar Client: custom screens replacing every vanilla menu, performance improvements, and later in-game modules and a launcher. Success is a client that looks premium and costs nothing extra while playing.

## Positioning

Performance comes first. Menu visuals use resources only while a menu is open and are freed on entering a world, so the client is as light in-game as vanilla or lighter.

## Operating Context

- Players launch through the Valor launcher (offline name for now) or a normal Forge 1.8.9 install.
- Default window 854x480, often fullscreen 1920x1080; Minecraft GUI scale changes the usable size.
- Main menu is a daily surface: players pass through it to Singleplayer or, mostly, Multiplayer servers.

## Capabilities and Constraints

- Forge 1.8.9-11.15.1.2318, Java 8, LWJGL 2, OpenGL 2.0 baseline with a fallback when shaders are missing.
- No Lunar code or assets. No features that give an unfair advantage on servers.
- Undecided: Microsoft login, in-game modules, HUD caching, skin preview implementation.

## Brand Commitments

- Name: Valor. Logo: the blue-gradient folded V (`.Valor/assets/logo.svg`).
- Palette taken from the logo: primary #0A4DFF, azure #00B4FF, cyan #00E5FF, navy #02021A, background #060A1E.
- Menu font: Inter (bundled, SIL OFL).
- Style reference chosen by the owner: Larp Client's minimal dark menus, done with real hierarchy.
- The black "Press to enter" intro stays and waits for a key press.
- A custom "Valor" wordmark exists but is not in the repo yet.

## Evidence on Hand

Logo files in `.Valor/assets`. No testimonials, user numbers or benchmarks exist; none may be invented.

## Product Principles

1. Zero cost in-game: anything drawn in a menu is freed when play starts.
2. Minimal, never empty: few elements, clear hierarchy, nothing decorative without a job.
3. One system on every screen: no screen falls back to the vanilla look.
4. Fast to the game: the path to a server is the shortest path on the menu.
