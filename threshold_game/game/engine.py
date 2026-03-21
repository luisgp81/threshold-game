"""
THRESHOLD: Global Crisis - Game Engine
Core game loop and state machine
"""
import json
import os
import random
import time
from typing import List, Dict, Optional, Any
from enum import Enum, auto

from .resources import Resources, RESOURCE_NAMES
from .regions import Region, create_regions, get_critical_regions
from .events import Event, EventManager, ActionChoice
from .narrative import NarrativeManager, LogEntry


class GameState(Enum):
    INTRO = auto()
    MAIN_HUD = auto()
    EVENT_DETAIL = auto()
    REGION_INTEL = auto()
    DIRECTOR_LOG = auto()
    SETTINGS = auto()
    GAME_OVER = auto()
    VICTORY = auto()
    PAUSED = auto()


class UnlockedAbility:
    BLACK_OPS = "BLACK_OPS"
    AI_ANALYSIS = "AI_ANALYSIS"
    ARCTIC = "ARCTIC"


# Built-in actions available each turn (supplementing event actions)
STANDARD_ACTIONS = [
    {
        "id": "deploy_intel",
        "label": "DEPLOY INTELLIGENCE ASSET",
        "description": "Dispatch field operatives to gather critical intelligence.",
        "resource_cost": {"FUNDS": 15},
        "resource_gain": {"INTEL": 20},
        "region_effect": {},
        "narrative_result": "Field assets deployed. Intelligence streams flowing.",
        "risk_level": "LOW",
        "requires_unlock": "",
    },
    {
        "id": "broker_deal",
        "label": "BROKER BACK-CHANNEL DEAL",
        "description": "Use diplomatic back-channels to stabilize the situation.",
        "resource_cost": {"INFLUENCE": 20},
        "resource_gain": {"STABILITY": 15},
        "region_effect": {"stability": 5},
        "narrative_result": "Back-channel negotiations yielded a fragile agreement.",
        "risk_level": "LOW",
        "requires_unlock": "",
    },
    {
        "id": "economic_pressure",
        "label": "ECONOMIC PRESSURE",
        "description": "Apply coordinated economic sanctions to reduce regional threat.",
        "resource_cost": {"FUNDS": 10, "INFLUENCE": 10},
        "resource_gain": {},
        "region_effect": {"threat": -15},
        "narrative_result": "Economic pressure applied. Regional actors recalibrating.",
        "risk_level": "MEDIUM",
        "requires_unlock": "",
    },
    {
        "id": "media_operation",
        "label": "MEDIA OPERATION",
        "description": "Shape the narrative. Control public perception.",
        "resource_cost": {"FUNDS": 15},
        "resource_gain": {"PUBLIC_TRUST": 20},
        "region_effect": {},
        "narrative_result": "Information channels saturated. Public trust rising.",
        "risk_level": "LOW",
        "requires_unlock": "",
    },
    {
        "id": "emergency_summit",
        "label": "EMERGENCY SUMMIT",
        "description": "Convene world leaders. High cost, major stability gain. 3-turn cooldown.",
        "resource_cost": {"INFLUENCE": 25},
        "resource_gain": {"STABILITY": 30},
        "region_effect": {"stability": 15, "threat": -20},
        "narrative_result": "Summit achieved. World leaders reached tentative consensus.",
        "risk_level": "LOW",
        "requires_unlock": "",
        "cooldown": 3,
    },
    {
        "id": "black_ops",
        "label": "BLACK OPS",
        "description": "Deniable operation. High risk, high reward. Exposure costs PUBLIC_TRUST.",
        "resource_cost": {"INTEL": 20, "FUNDS": 20},
        "resource_gain": {"STABILITY": 20, "INFLUENCE": 15},
        "region_effect": {"threat": -25},
        "narrative_result": "Operation executed. No official record. Results... effective.",
        "risk_level": "HIGH",
        "requires_unlock": "BLACK_OPS",
    },
    {
        "id": "ai_analysis",
        "label": "AI ANALYSIS",
        "description": "Deploy AI forecasting. Reveals next 2 events. Costs INTEL.",
        "resource_cost": {"INTEL": 20},
        "resource_gain": {},
        "region_effect": {},
        "narrative_result": "ORACLE system activated. Predictive models processing.",
        "risk_level": "LOW",
        "requires_unlock": "AI_ANALYSIS",
    },
]


class GameEngine:
    def __init__(self, data_path: str):
        self.data_path = data_path
        self.resources = Resources()
        self.regions: List[Region] = create_regions()
        self.event_manager = EventManager(data_path)
        self.narrative = NarrativeManager(data_path)

        self.turn: int = 1
        self.max_turns: int = 30
        self.state: GameState = GameState.INTRO
        self.difficulty: str = "DIRECTOR"

        self.unlocked_abilities: List[str] = []
        self.action_cooldowns: Dict[str, int] = {}
        self.actions_taken_this_turn: int = 0
        self.max_actions_per_turn: int = 2

        self.current_events: List[Event] = []
        self.selected_event: Optional[Event] = None
        self.selected_region: Optional[Region] = None

        self.win_condition: Optional[str] = None
        self.lose_reason: Optional[str] = None

        self.preview_events: List[Event] = []  # For AI_ANALYSIS
        self.summit_cooldown: int = 0

        self.save_path = os.path.join(os.path.dirname(data_path), "savegame.json")

        self.flavor_text: str = ""
        self.consequence_messages: List[str] = []
        self.turn_resolution_log: List[str] = []

        self.score: int = 0
        self.settings = {
            "volume": 70,
            "crt_effect": True,
            "difficulty": "DIRECTOR",
            "fullscreen": False,
        }

    def start_new_game(self, difficulty: str = "DIRECTOR"):
        self.difficulty = difficulty
        self.settings["difficulty"] = difficulty
        self.resources = Resources()
        self.resources.difficulty_scale(difficulty)
        self.regions = create_regions()
        self.turn = 1
        self.unlocked_abilities = []
        self.action_cooldowns = {}
        self.actions_taken_this_turn = 0
        self.win_condition = None
        self.lose_reason = None
        self.preview_events = []
        self.summit_cooldown = 0
        self.narrative.director_log.clear()
        self._prepare_turn()
        self.state = GameState.MAIN_HUD

    def _prepare_turn(self):
        """Prepare events and state for a new turn."""
        self.actions_taken_this_turn = 0
        self.turn_resolution_log = []

        # Check unlocks
        if self.turn >= 5 and UnlockedAbility.BLACK_OPS not in self.unlocked_abilities:
            self.unlocked_abilities.append(UnlockedAbility.BLACK_OPS)
            self.turn_resolution_log.append(">> BLACK OPS capability unlocked.")

        if self.turn >= 8 and UnlockedAbility.AI_ANALYSIS not in self.unlocked_abilities:
            self.unlocked_abilities.append(UnlockedAbility.AI_ANALYSIS)
            self.turn_resolution_log.append(">> AI ANALYSIS system online.")

        if self.turn >= 10 and UnlockedAbility.ARCTIC not in self.unlocked_abilities:
            self.unlocked_abilities.append(UnlockedAbility.ARCTIC)
            self.turn_resolution_log.append(">> ARCTIC ZONE surveillance activated.")

        # Decrement cooldowns
        self.summit_cooldown = max(0, self.summit_cooldown - 1)
        for k in list(self.action_cooldowns.keys()):
            self.action_cooldowns[k] = max(0, self.action_cooldowns[k] - 1)

        # Get active regions
        active_region_ids = self._get_active_region_ids()

        # Check for pending consequences
        consequences = self.event_manager.get_due_consequences(self.turn)
        for cons in consequences:
            self._apply_consequence(cons)

        # Apply passive regional pressure
        self._apply_passive_effects()

        # Get new events
        self.current_events = self.event_manager.get_events_for_turn(
            self.turn, self.unlocked_abilities, active_region_ids
        )

        self.flavor_text = self.narrative.get_random_flavor()

    def _get_active_region_ids(self) -> List[str]:
        active = []
        for region in self.regions:
            if region.unlocks_turn == 0 or self.turn >= region.unlocks_turn:
                active.append(region.id)
        return active

    def _apply_passive_effects(self):
        """Apply passive resource drain/gain each turn."""
        # Global stability drift
        critical_count = len(get_critical_regions(self.regions))

        drift_stability = -2 - critical_count
        drift_intel = -1
        drift_funds = -3
        drift_influence = -1
        drift_trust = -1 - critical_count // 2

        # Difficulty multipliers
        if self.difficulty == "ANALYST":
            mult = 0.5
        elif self.difficulty == "CRISIS MODE":
            mult = 1.8
        else:
            mult = 1.0

        self.resources.apply_delta("STABILITY", int(drift_stability * mult))
        self.resources.apply_delta("INTEL", int(drift_intel * mult))
        self.resources.apply_delta("FUNDS", int(drift_funds * mult))
        self.resources.apply_delta("INFLUENCE", int(drift_influence * mult))
        self.resources.apply_delta("PUBLIC_TRUST", int(drift_trust * mult))

        # Regional passive drift
        for region in self.regions:
            if region.unlocks_turn > 0 and self.turn < region.unlocks_turn:
                continue
            # Threat creep
            threat_creep = random.randint(0, 3)
            region.update_threat(threat_creep)
            # Stability erosion
            region.update_stability(-random.randint(0, 2))

    def _apply_consequence(self, consequence: Dict):
        """Apply a hidden consequence from a previous event."""
        msg = consequence.get("message", "Delayed consequence activated.")
        self.consequence_messages.append(f">> CONSEQUENCE: {msg}")
        self.turn_resolution_log.append(f"CONSEQUENCE TRIGGERED: {msg}")

        resource_effects = consequence.get("resource_effects", {})
        for name, delta in resource_effects.items():
            self.resources.apply_delta(name, delta)

        region_effects = consequence.get("region_effects", {})
        region_id = consequence.get("region_id", "")
        if region_id:
            region = self.get_region_by_id(region_id)
            if region:
                for effect, val in region_effects.items():
                    if effect == "threat":
                        region.update_threat(val)
                    elif effect == "stability":
                        region.update_stability(val)
                    elif effect == "influence":
                        region.update_influence(val)

    def take_action(self, event: Event, choice: ActionChoice, target_region: Optional[Region] = None):
        """Execute a player action choice."""
        if self.actions_taken_this_turn >= self.max_actions_per_turn:
            return False, "Maximum actions for this turn reached."

        # Check cooldown for emergency summit
        if choice.id == "emergency_summit" and self.summit_cooldown > 0:
            return False, f"Emergency Summit on cooldown: {self.summit_cooldown} turns remaining."

        # Check can afford
        for resource, cost in choice.resource_cost.items():
            if self.resources.get(resource) < cost:
                return False, f"Insufficient {resource}."

        # Apply costs
        for resource, cost in choice.resource_cost.items():
            self.resources.apply_delta(resource, -cost)

        # Apply gains
        for resource, gain in choice.resource_gain.items():
            self.resources.apply_delta(resource, gain)

        # Black ops exposure risk
        narrative_result = choice.narrative_result
        if choice.id == "black_ops":
            if random.random() < 0.25:  # 25% exposure risk
                self.resources.apply_delta("PUBLIC_TRUST", -20)
                narrative_result += " EXPOSURE RISK: Operation partially compromised. Trust damaged."
                self.turn_resolution_log.append(">> BLACK OPS: PARTIAL EXPOSURE DETECTED")

        # Apply region effects
        region_for_effect = target_region or self._get_region_by_event(event)
        if region_for_effect:
            for effect, val in choice.region_effect.items():
                if effect == "threat":
                    region_for_effect.update_threat(val)
                elif effect == "stability":
                    region_for_effect.update_stability(val)
                elif effect == "influence":
                    region_for_effect.update_influence(val)

        # Handle summit cooldown
        if choice.id == "emergency_summit":
            self.summit_cooldown = 3

        # Handle AI Analysis
        if choice.id == "ai_analysis":
            active_ids = self._get_active_region_ids()
            future_events = self.event_manager.get_events_for_turn(
                self.turn + 1, self.unlocked_abilities, active_ids
            )
            self.preview_events = future_events[:2]
            # Un-mark these as shown since they're previewed
            for e in self.preview_events:
                e.already_shown = False

        # Register consequence
        if event.hidden_consequence and event.hidden_consequence_turn > 0:
            trigger = self.turn + event.hidden_consequence_turn
            self.event_manager.register_consequence(event.hidden_consequence, trigger)

        # Random modifier (±5% variance)
        for name in RESOURCE_NAMES:
            variance = random.randint(-3, 3)
            self.resources.apply_delta(name, variance)

        # Log entry
        resource_changes = {}
        for k, v in choice.resource_cost.items():
            resource_changes[k] = resource_changes.get(k, 0) - v
        for k, v in choice.resource_gain.items():
            resource_changes[k] = resource_changes.get(k, 0) + v

        self.narrative.add_log_entry(
            turn=self.turn,
            event_title=event.title,
            choice_label=choice.label,
            narrative_result=narrative_result,
            resource_changes=resource_changes,
        )

        self.turn_resolution_log.append(
            f"TURN {self.turn}: [{choice.label}] - {narrative_result}"
        )

        self.actions_taken_this_turn += 1
        return True, narrative_result

    def _get_region_by_event(self, event: Event) -> Optional[Region]:
        return self.get_region_by_id(event.region_affected)

    def get_region_by_id(self, region_id: str) -> Optional[Region]:
        for r in self.regions:
            if r.id == region_id:
                return r
        return None

    def advance_turn(self):
        """Move to next turn after actions are resolved."""
        self.turn += 1
        self.consequence_messages = []

        # Check win/lose conditions BEFORE preparing next turn
        result = self.check_win_lose()
        if result:
            return result

        self._prepare_turn()
        return None

    def check_win_lose(self) -> Optional[str]:
        """Check win/lose conditions. Returns state string or None."""
        # Lose conditions
        zero_resources = self.resources.any_zero()
        if zero_resources:
            self.lose_reason = f"CRITICAL FAILURE: {zero_resources[0]} depleted to zero."
            self.state = GameState.GAME_OVER
            self.score = self.narrative.get_score(self)
            return "GAME_OVER"

        critical = get_critical_regions(self.regions)
        if len(critical) >= 3:
            names = ", ".join(r.name for r in critical[:3])
            self.lose_reason = f"THREE SIMULTANEOUS CRISES: {names}"
            self.state = GameState.GAME_OVER
            self.score = self.narrative.get_score(self)
            return "GAME_OVER"

        # Win conditions (at turn 30 or earlier if achieved)
        res = self.resources

        # Diplomatic Victory
        if res.STABILITY > 80 and res.PUBLIC_TRUST > 70:
            self.win_condition = "DIPLOMATIC_VICTORY"
            self.state = GameState.VICTORY
            self.score = self.narrative.get_score(self)
            return "VICTORY"

        # Intelligence Supremacy
        active_regions = [r for r in self.regions if r.unlocks_turn == 0 or self.turn >= r.unlocks_turn]
        if res.INTEL > 90 and all(r.is_influenced() for r in active_regions):
            self.win_condition = "INTELLIGENCE_SUPREMACY"
            self.state = GameState.VICTORY
            self.score = self.narrative.get_score(self)
            return "VICTORY"

        # Pragmatic Control (at turn 30)
        if self.turn >= self.max_turns:
            if res.all_above(30):
                self.win_condition = "PRAGMATIC_CONTROL"
                self.state = GameState.VICTORY
            else:
                self.lose_reason = "TURN LIMIT REACHED: Insufficient stability achieved."
                self.state = GameState.GAME_OVER
            self.score = self.narrative.get_score(self)
            return self.state.name

        return None

    def get_available_standard_actions(self) -> List[Dict]:
        """Return standard actions, filtered by unlocks and cooldowns."""
        available = []
        for action in STANDARD_ACTIONS:
            unlock = action.get("requires_unlock", "")
            if unlock and unlock not in self.unlocked_abilities:
                continue
            if action["id"] == "emergency_summit" and self.summit_cooldown > 0:
                action = dict(action)
                action["label"] = f"EMERGENCY SUMMIT [COOLDOWN: {self.summit_cooldown}]"
                action["_locked"] = True
            available.append(action)
        return available

    def save_game(self):
        """Auto-save current game state to JSON."""
        try:
            data = {
                "turn": self.turn,
                "difficulty": self.difficulty,
                "resources": self.resources.to_dict(),
                "regions": [r.to_dict() for r in self.regions],
                "unlocked_abilities": self.unlocked_abilities,
                "summit_cooldown": self.summit_cooldown,
                "actions_taken": self.actions_taken_this_turn,
                "log_count": len(self.narrative.director_log),
            }
            with open(self.save_path, "w", encoding="utf-8") as f:
                json.dump(data, f, indent=2)
        except Exception as e:
            print(f"Auto-save failed: {e}")

    def get_threat_summary(self) -> Dict[str, int]:
        """Return threat levels for all active regions."""
        return {
            r.id: r.threat_level
            for r in self.regions
            if r.unlocks_turn == 0 or self.turn >= r.unlocks_turn
        }

    def skip_turn(self):
        """End turn without taking full actions (passive play)."""
        self.actions_taken_this_turn = self.max_actions_per_turn
        return self.advance_turn()
