# Neo Tech Reborn

A native NeoForge 1.21.1 port of [Tech Reborn](https://github.com/TechReborn/TechReborn) and its library, Reborn Core.

No Sinytra Connector, no Forgified Fabric API. Just NeoForge.

> **Status: 0.1.0 beta.** It loads and the machines work, but it's a first release. Expect rough edges and please report what you find.

## Why

Tech Reborn is a Fabric mod. On NeoForge the only way to run it was through Sinytra Connector, which pulls in a whole compat layer just for one mod. This port rewrites the Fabric-specific parts (energy, fluids, item transfer, networking, registries, events) to use the NeoForge APIs directly, and gets rid of the mixins that NeoForge events can replace.

## What you get

- Two mods: **Neo Reborn Core** (`reborncore`) and **Neo Tech Reborn** (`techreborn`). You need both.
- Same mod IDs as the originals, so existing worlds and recipes keep pointing at the right things.
- Energy is exposed as NeoForge Forge Energy (FE), fluids and items use the standard NeoForge capabilities, so other mods can talk to the machines normally.
- Fluid amounts in recipes are in **mB** (1 bucket = 1000 mB), not Fabric droplets.
- Some performance work on top of the original: cached recipe lookups and fewer pointless energy lookups per machine tick.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x
- Java 21
- JEI is optional. It's developed against 19.70.0.458, and other versions should work too since no minimum version is enforced.

## Known limitations

- Dedicated KubeJS recipe support is postponed for now. Custom recipes do work through `event.custom`.
- This is a beta, so some features of the original mod may still be missing or behave a bit differently. Check the issue tracker, and open an issue if something's broken.

## Building

```
gradlew build
```

The jars end up in `reborncore/build/libs` and `techreborn/build/libs`. You need Java 21 as the Gradle JVM.

## Project layout

- `reborncore/` - Neo Reborn Core
- `techreborn/` - Neo Tech Reborn

## A note on AI

This port was made with a lot of help from an AI assistant (Claude, by Anthropic). I (RickWeek) decide what gets ported and how, run and test it in game, and I'm the one maintaining it. The AI did much of the heavy lifting: reading the original code, converting it to NeoForge and writing patches.

So if something looks odd, it's more likely a mistake in the port than something from the original mod. Please open an issue and I'll look into it.

## Credits and license

All the original work is by the [Tech Reborn team](https://github.com/TechReborn/TechReborn) (TechReborn and TeamReborn). This port is by RickWeek.

The code is under the MIT license, see [LICENSE.md](LICENSE.md). Textures and other assets come from the original mod and belong to its authors, see the upstream repository for their terms.
