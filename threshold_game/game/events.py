"""
THRESHOLD: Global Crisis - Event System
"""
import json
import os
import random
from dataclasses import dataclass, field
from typing import List, Dict, Optional, Any


@dataclass
class ActionChoice:
    id: str
    label: str
    description: str
    resource_cost: Dict[str, int]
    resource_gain: Dict[str, int]
    region_effect: Dict[str, int]  # stability/threat/influence deltas
    narrative_result: str
    risk_level: str = "LOW"  # LOW, MEDIUM, HIGH
    requires_unlock: str = ""  # e.g. "BLACK_OPS", "AI_ANALYSIS"


@dataclass
class Event:
    id: str
    title: str
    description: str
    category: str  # POLITICAL, ECONOMIC, TECHNOLOGICAL, MILITARY, HUMANITARIAN
    region_affected: str
    urgency: str  # LOW, MEDIUM, HIGH, CRITICAL
    choices: List[ActionChoice]
    hidden_consequence_turn: int  # turns until consequence triggers
    hidden_consequence: Optional[Dict[str, Any]] = None
    flavor_intro: str = ""
    already_shown: bool = False

    def get_net_cost(self, choice_id: str) -> Dict[str, int]:
        """Get net resource change for a choice."""
        for ch in self.choices:
            if ch.id == choice_id:
                net = {}
                for k, v in ch.resource_cost.items():
                    net[k] = net.get(k, 0) - v
                for k, v in ch.resource_gain.items():
                    net[k] = net.get(k, 0) + v
                return net
        return {}


class EventManager:
    def __init__(self, data_path: str):
        self.all_events: List[Event] = []
        self.active_events: List[Event] = []
        self.pending_consequences: List[Dict] = []
        self.load_events(data_path)

    def load_events(self, data_path: str):
        events_file = os.path.join(data_path, "events.json")
        try:
            with open(events_file, "r", encoding="utf-8") as f:
                data = json.load(f)
            for ed in data["events"]:
                choices = []
                for cd in ed.get("choices", []):
                    choices.append(ActionChoice(
                        id=cd["id"],
                        label=cd["label"],
                        description=cd["description"],
                        resource_cost=cd.get("resource_cost", {}),
                        resource_gain=cd.get("resource_gain", {}),
                        region_effect=cd.get("region_effect", {}),
                        narrative_result=cd.get("narrative_result", ""),
                        risk_level=cd.get("risk_level", "LOW"),
                        requires_unlock=cd.get("requires_unlock", ""),
                    ))
                event = Event(
                    id=ed["id"],
                    title=ed["title"],
                    description=ed["description"],
                    category=ed["category"],
                    region_affected=ed["region_affected"],
                    urgency=ed.get("urgency", "MEDIUM"),
                    choices=choices,
                    hidden_consequence_turn=ed.get("hidden_consequence_turn", 0),
                    hidden_consequence=ed.get("hidden_consequence"),
                    flavor_intro=ed.get("flavor_intro", ""),
                )
                self.all_events.append(event)
        except Exception as e:
            print(f"Warning: Could not load events: {e}")
            self._load_fallback_events()

    def _load_fallback_events(self):
        """Minimal fallback if JSON fails to load."""
        fallback = Event(
            id="fallback_001",
            title="COMMUNICATIONS BLACKOUT",
            description="All secure channels have gone dark. Something is happening.",
            category="MILITARY",
            region_affected="NORTH_ATLANTIC",
            urgency="HIGH",
            choices=[
                ActionChoice(
                    id="investigate",
                    label="DEPLOY INTEL ASSETS",
                    description="Send field operatives to assess the situation.",
                    resource_cost={"FUNDS": 15},
                    resource_gain={"INTEL": 20},
                    region_effect={"threat": -10},
                    narrative_result="Operatives report unusual electromagnetic activity.",
                )
            ],
            hidden_consequence_turn=3,
        )
        self.all_events.append(fallback)

    def get_events_for_turn(self, turn: int, unlocked: List[str], regions_active: List[str]) -> List[Event]:
        """Select 2-3 events for the current turn."""
        available = [
            e for e in self.all_events
            if not e.already_shown
            and e.region_affected in regions_active
        ]
        if len(available) < 2:
            # Reset already_shown flags if we run out
            for e in self.all_events:
                if e.region_affected in regions_active:
                    e.already_shown = False
            available = [
                e for e in self.all_events
                if e.region_affected in regions_active
            ]

        # Weight by urgency
        weights = []
        for e in available:
            w = {"LOW": 1, "MEDIUM": 2, "HIGH": 3, "CRITICAL": 4}.get(e.urgency, 2)
            weights.append(w)

        count = random.randint(2, min(3, len(available)))
        if len(available) <= count:
            selected = available[:count]
        else:
            selected = random.choices(available, weights=weights, k=count)
            # Deduplicate
            seen = set()
            unique = []
            for e in selected:
                if e.id not in seen:
                    seen.add(e.id)
                    unique.append(e)
            # Fill if dedup reduced count
            while len(unique) < count and len(available) > len(unique):
                for e in available:
                    if e.id not in seen:
                        unique.append(e)
                        seen.add(e.id)
                        break
            selected = unique

        for e in selected:
            e.already_shown = True
        return selected

    def filter_choices(self, event: Event, unlocked: List[str]) -> List[ActionChoice]:
        """Filter choices based on unlocked abilities."""
        return [
            c for c in event.choices
            if not c.requires_unlock or c.requires_unlock in unlocked
        ]

    def register_consequence(self, consequence: Dict, trigger_turn: int):
        self.pending_consequences.append({
            "consequence": consequence,
            "trigger_turn": trigger_turn,
        })

    def get_due_consequences(self, turn: int) -> List[Dict]:
        due = [pc["consequence"] for pc in self.pending_consequences if pc["trigger_turn"] <= turn]
        self.pending_consequences = [pc for pc in self.pending_consequences if pc["trigger_turn"] > turn]
        return due
