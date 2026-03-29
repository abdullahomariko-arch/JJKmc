# JJKmc Skript Scripts

This folder contains Skript visual scripts for the JJK Cursed Tools plugin.

## How to use

1. Install [Skript](https://github.com/SkriptLang/Skript/releases) and [SkBee](https://github.com/ShaneBeee/SkBee/releases) on your server.
2. Copy the `.sk` files from this folder into your server's `plugins/Skript/scripts/` directory.
3. Run `/skript reload all` (or restart the server).

## Files

| File | Description |
|------|-------------|
| `jjk_energy_discharge.sk` | Visual hooks for the Energy Discharge technique |

## How the Java plugin talks to Skript

The plugin uses `SkriptBridge.triggerVisual(scriptName, player, location)` which dispatches
a console command in the format:

```
jjk_visual <scriptName> <playerName> <world> <x> <y> <z>
```

Your Skript file can listen for this command and respond with particle effects, sounds, etc.

## Dependency

These scripts are **optional**. The JJKmc plugin works perfectly without Skript installed.
Skript and SkBee are soft dependencies — the plugin will silently skip visual triggers
if they are not present.
