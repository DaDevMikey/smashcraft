# ⚡ Smash Craft

### 🎮 Super Smash Bros. meets Minecraft — a total combat overhaul!

**Smash Craft** transforms Minecraft's combat into an explosive, percentage-based brawl inspired by *Super Smash Bros.* Every hit builds damage percentage, knockback scales the higher it climbs, and at critical levels — enemies get launched into the stratosphere with a spectacular **Star KO**. Grab your swords, charge your smash attacks, and send your friends flying!

---

## ✨ Features

### 💥 Percentage System
- Every entity tracks a **damage percentage** that increases when hit
- Damage dealt is multiplied by **3×** and added to the target's percent (critical hits multiply by an additional **1.5×**!)
- A beautiful **color-coded HUD bar** displays your current percentage — shifting from green to red as danger rises
- Percentage is shown on the scoreboard below player names for all to see
- Eating food **heals 15%** of your accumulated damage — strategic healing matters!
- Percentage resets on death by void or Star KO, but **persists through normal deaths**

### 🌟 Star KO
- When an entity's percentage exceeds **150%**, every hit has an escalating chance to trigger a **Star KO**
- The chance formula: `(percent - 150) / 100` — at 250%, it's guaranteed!
- Star KO'd entities are launched **straight into the sky** with Levitation 50, phasing through blocks until they reach Y=320 where they're eliminated
- Massive **explosion particles**, **firework bursts**, **end rod trails**, and a **shockwave ring** of cloud particles erupt on impact
- A custom **Star KO sound effect** plays alongside an explosion sound
- The attacker gets knocked back from the sheer force of the impact
- **Damage Scaling**: Launch speed, explosion size, crater size, and attacker recoil all scale with the weapon's damage!

### 🛡️ Smash Shield
- Activate a **particle shield bubble** (blue dust particles) that absorbs incoming damage
- Shield has **100 HP** and takes **10× the attack damage** — strong hits drain it fast!
- Shield **passively drains** while active (0.2/tick) and **regenerates** when deactivated (0.5/tick)
- When the shield breaks: glass break sound, plus **5 seconds of Slowness V and Weakness V** — you're completely vulnerable!
- A **shield health bar** (cyan) appears above the damage bar when shielding is active

### 💪 Smash Attacks
- Charge up a devastating **Smash Attack** that multiplies your next hit's knockback by **3×**
- A flashing **"SMASH READY"** indicator appears on your HUD (alternating yellow/red)
- **Directional Smash Attacks**: 
  - Look UP to perform an **Up Smash** (massive vertical knockback, low horizontal)
  - Sneak/Crouch to perform a **Down Smash** (massive horizontal knockback, purely horizontal trajectory)
  - Standard attack is a **Forward Smash**
- Lands with **explosion and firework particles** for maximum impact
- The charged state persists until you land a hit — time it right!

### 🦅 Double Jump
- Perform a **mid-air double jump** once per airborne period — resets when you touch the ground
- Launches you upward with a velocity of **0.8** and spawns **cloud particles** under your feet
- A soft wool sound plays for satisfying audio feedback
- Essential for recovery after being launched!

### 🧗 Ledge Grabbing
- Falling near a ledge? Simply brush up against the wall, and you'll **automatically snap to the ledge** and hang there!
- Press **Jump** to quickly launch yourself up and over the ledge.
- Press **Sneak** to drop down.
- A critical tool for surviving those intense blast zone recoveries!

### 🔮 Smash Ball & Final Smash
- The legendary **Smash Ball** floats around the world! It has a rare chance to spawn near players (roughly every 8 minutes).
- It takes 3-4 solid hits to break the glowing, rainbow-colored orb.
- Break it to gain the **Final Smash**!
- With the Final Smash, your next hit is an **instant Star KO**, completely ignoring the opponent's percentage!

### 🔄 Knockback Scaling
- Vanilla knockback is **completely replaced** with a percentage-based system
- Knockback multiplier: `(1.0 + (percent / 40.0)) * (0.8 + dmgScale * 0.2)` — knockback scales with both percentage and weapon damage!
- High knockback hits spawn **firework particles** and trigger **screenshake**
- **Critical hits** (falling + airborne) deal 1.5× percent damage and display a "CRITICAL HIT!" message

### ❄️ Freeze Frames
- Star KO hits have a **40% chance** to trigger dramatic **freeze frames**
- Both the victim AND the attacker are frozen in place for a few ticks (duration scales with weapon damage)
- Red dust particles swirl around frozen entities for cinematic flair
- After the freeze ends, the victim is explosively launched with an extra **explosion emitter** particle burst

### 💣 Terrain Destruction
- Star KO impacts create a **crater** at the impact point (radius scales with weapon damage)
- Destroys blocks in a sphere, dropping items naturally
- Respects block hardness — **obsidian and bedrock are safe** (hardness ≥ 50 is protected)

### 📸 Screenshake & Impact Frames
- **Camera screenshake** with intensity scaling based on hit power — the screen rumbles!
- **Impact frames** flash a red overlay across the screen for 3 ticks on devastating hits
- Players within **64 blocks** of a major impact feel the shake
- Smash Attacks trigger intensity **0.8** shake; Star KOs trigger dynamic intensity up to **2.0**!

### 🏆 Advancements
Unlock **7 custom advancements** on your journey to becoming the ultimate brawler:

| Advancement | Description | Icon |
|---|---|---|
| **Welcome to Smash Craft** | Start building up some percentage! | 🗡️ Iron Sword |
| **Airborne Ace** | Perform a Double Jump to recover | 🪶 Feather |
| **Home Run** | Land a fully charged Smash Attack | 💎 Diamond Sword |
| **Shield Breaker** | Break an opponent's Smash Shield | 🛡️ Shield |
| **Invincible** | Block a massive hit (15+ damage) using your Smash Shield | ⬛ Obsidian |
| **It's Over 300!** | Get an entity's percentage above 300% | ⚔️ Golden Sword |
| **Team Rocket's Blasting Off Again!** | Send someone to the stratosphere with a Star KO | ⭐ Nether Star |

### ⌨️ Keybinds
All keybinds are fully **rebindable** in Minecraft's controls menu under the Gameplay category:

| Key | Action |
|---|---|
| **R** | 🦅 Double Jump (mid-air only) |
| **V** | 💪 Charge Smash Attack |
| **G** | 🛡️ Toggle Shield |

### 💬 Commands
All commands are under `/smash`:

| Command | Description |
|---|---|
| `/smash` | Show command help |
| `/smash set <targets> <percent>` | Set an entity's percentage |
| `/smash add <targets> <percent>` | Add to an entity's percentage |
| `/smash get <target>` | Check an entity's current percentage |
| `/smash jump` | Perform a double jump (command alternative) |
| `/smash attack` | Charge a smash attack (command alternative) |
| `/smash shield` | Toggle your shield (command alternative) |
| `/smash scoreboard show` | Display the smash percentage sidebar |
| `/smash scoreboard hide` | Hide the smash percentage sidebar |

### ⚙️ Server Configuration
Server owners can toggle all custom mechanics dynamically in-game using the new `/smashcraft rule` command!

| Rule | Default | Description |
|---|---|---|
| `RULE_LEDGE_GRABBING` | `true` | Allows players to grab ledges when falling |
| `RULE_DIRECTIONAL_SMASH` | `true` | Enables Up Smash and Down Smash trajectories |
| `RULE_SMASH_BALL` | `true` | Allows Smash Balls to randomly spawn near players |
| `RULE_SHIELD` | `true` | Enables the Smash Shield mechanic |
| `RULE_SMASH_ATTACK` | `true` | Enables charging Smash Attacks |
| `RULE_DOUBLE_JUMP` | `true` | Enables the Double Jump mechanic |

**Usage Example:**
`/smashcraft rule RULE_LEDGE_GRABBING false`

### 🎨 Visual & Audio Polish
- **Title screen badge** — "SMASHCRAFT ACTIVE!" with "Get ready to brawl!" displays on the main menu
- **Custom HUD** with percentage bar, shield bar, and smash ready indicator
- **Mob support** — all living entities (not just players) track percentage and can be Star KO'd; mobs display their percentage via custom name tags
- **Scoreboard integration** — percentage is tracked via a `smashcraft_percent` objective shown below player names

---

## 📦 Installation

1. Install **Minecraft 26.1.2** with the **Fabric Loader** (v0.19.3+)
2. Download and install **Fabric API** (v0.151.0+)
3. Drop the **Smash Craft** `.jar` file into your `mods` folder
4. Launch Minecraft and get ready to brawl! ⚡

### Requirements
- ☕ **Java 25** or newer
- 🧵 **Fabric Loader** 0.19.3+
- 🔧 **Fabric API** 0.151.0+
- 🎮 **Minecraft** 26.1.2

---

## 🌐 Multiplayer Compatibility

Smash Craft is built with **full multiplayer support** from the ground up! All state is synced via custom networking packets, so every player sees percentage bars, shield particles, star KOs, and screenshake effects in real-time.

> **💡 Tip:** Works great with the **Essentials** mod for enhanced multiplayer server features!

The mod must be installed on **both the server and all clients** for full functionality.

---

## 🔧 Compatibility Notes

- ✅ Works on **dedicated servers** and **singleplayer**
- ✅ Applies to **all living entities** — fight mobs with Smash mechanics too!
- ✅ Scoreboard integration works alongside other scoreboard objectives
- ⚠️ Terrain destruction from Star KO impacts **does break blocks** — consider region protection on build servers

---

## 📜 License

This project is licensed under the **GNU General Public License v3.0** — see the `LICENSE` file for details.

---

## 💜 Credits

**Created by [DaManMikey](https://github.com/DaManMikey)** — built with love for Smash Bros. and Minecraft alike.

Made with the **Fabric** modding toolchain.

---

*Now get out there and send your friends to the blast zone!* 💫
