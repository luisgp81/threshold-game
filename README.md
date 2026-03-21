# THRESHOLD: Global Crisis

> Turn-based geopolitical strategy. 2027. You have 30 weeks to prevent global collapse.

## Quick Start

```bash
cd threshold_game
pip install -r requirements.txt
python main.py
```

## Controls

| Key | Action |
|-----|--------|
| Click event | Open event detail |
| Click region | View region intel |
| 1 / 2 / 3 | Quick-select action |
| N | End week / advance turn |
| L | Director's Log |
| ESC | Settings / back |
| F11 | Toggle fullscreen |
| Arrow keys | Navigate menus |

## Gameplay

1. Each turn = 1 week. You have 30 weeks.
2. 2–3 incidents appear each turn — click one to respond.
3. Choose from 2–3 response options with resource costs/gains.
4. Resources decay passively. Crises escalate if ignored.
5. Some actions have delayed consequences (2–5 turns later).

**Win conditions:**
- **Diplomatic Victory** — STABILITY > 80 and PUBLIC_TRUST > 70
- **Intelligence Supremacy** — INTEL > 90 and all regions influenced
- **Pragmatic Control** — All resources > 30 at turn 30

**Lose if:**
- Any resource hits 0
- 3+ regions reach critical threat simultaneously

## Unlockable Abilities

| Ability | Unlocks | Effect |
|---------|---------|--------|
| BLACK OPS | Turn 5 | High-risk covert action |
| AI ANALYSIS | Turn 8 | Preview next 2 events |
| ARCTIC ZONE | Turn 10 | New region becomes active |

## Difficulty

| Mode | Effect |
|------|--------|
| ANALYST | Higher starting resources, slower decay |
| DIRECTOR | Standard (default) |
| CRISIS MODE | Low resources, fast decay, high stakes |

## Build for Distribution

```bash
cd threshold_game
python build_exe.py
```

Output in `dist/THRESHOLD_GlobalCrisis/`

## File Structure

```
threshold_game/
├── main.py              # Entry point
├── game/
│   ├── engine.py        # Game loop + state machine
│   ├── events.py        # Event system
│   ├── resources.py     # Resource management
│   ├── regions.py       # Region data
│   ├── narrative.py     # Log + report generation
│   ├── renderer.py      # CRT renderer + HUD
│   └── screens/
│       ├── intro.py
│       ├── main_hud.py
│       ├── event_screen.py
│       ├── game_over.py
│       ├── settings.py
│       └── director_log.py
├── data/
│   ├── events.json      # 10 events (expandable to 40+)
│   └── narrative.json   # Flavor text + endings
├── steam_metadata/      # Steam store assets
├── build_exe.py         # PyInstaller build script
└── requirements.txt
```

## Expanding Events

Add entries to `data/events.json` following the existing schema. The game
supports 40+ events — the minimal 10-event set included is sufficient for
full gameplay and easy to expand.
