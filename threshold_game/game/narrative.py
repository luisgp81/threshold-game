"""
THRESHOLD: Global Crisis - Narrative System
"""
import json
import os
import random
from typing import List, Dict, Optional
from dataclasses import dataclass, field


@dataclass
class LogEntry:
    turn: int
    event_title: str
    choice_label: str
    narrative_result: str
    resource_changes: Dict[str, int]
    timestamp: str = ""


class NarrativeManager:
    def __init__(self, data_path: str):
        self.director_log: List[LogEntry] = []
        self.flavor_texts: List[str] = []
        self.consequence_texts: List[str] = []
        self.victory_texts: Dict[str, str] = {}
        self.defeat_texts: Dict[str, str] = {}
        self.load_narrative(data_path)

    def load_narrative(self, data_path: str):
        narrative_file = os.path.join(data_path, "narrative.json")
        try:
            with open(narrative_file, "r", encoding="utf-8") as f:
                data = json.load(f)
            self.flavor_texts = data.get("flavor_texts", [])
            self.consequence_texts = data.get("consequence_texts", [])
            self.victory_texts = data.get("victory_texts", {})
            self.defeat_texts = data.get("defeat_texts", {})
        except Exception as e:
            print(f"Warning: Could not load narrative: {e}")
            self._load_fallback_narrative()

    def _load_fallback_narrative(self):
        self.flavor_texts = [
            "The world watches. History holds its breath.",
            "Every decision leaves a scar on the timeline.",
            "Intelligence is power. Power is responsibility.",
            "The clock ticks. The crisis deepens.",
            "From this room, empires rise and fall.",
        ]
        self.victory_texts = {
            "DIPLOMATIC": "Through careful diplomacy, you prevented global catastrophe.",
            "INTELLIGENCE": "Your intelligence network became the invisible hand of peace.",
            "PRAGMATIC": "Balanced control maintained the fragile order.",
        }
        self.defeat_texts = {
            "RESOURCE_ZERO": "Critical failure. The agency has lost all operational capacity.",
            "CRITICAL_REGIONS": "Three simultaneous crises overwhelmed your response capability.",
        }

    def get_random_flavor(self) -> str:
        if self.flavor_texts:
            return random.choice(self.flavor_texts)
        return "The situation remains fluid..."

    def add_log_entry(self, turn: int, event_title: str, choice_label: str,
                      narrative_result: str, resource_changes: Dict[str, int]):
        entry = LogEntry(
            turn=turn,
            event_title=event_title,
            choice_label=choice_label,
            narrative_result=narrative_result,
            resource_changes=resource_changes,
        )
        self.director_log.append(entry)

    def get_log_entries(self) -> List[LogEntry]:
        return list(reversed(self.director_log))

    def generate_declassified_report(self, game_state) -> List[str]:
        """Generate the end-game declassified report."""
        lines = [
            "=" * 52,
            "   DECLASSIFIED AFTER-ACTION REPORT",
            "   OPERATION: THRESHOLD",
            "   CLASSIFICATION: EYES ONLY",
            "=" * 52,
            "",
            f"  DURATION: {game_state.turn} WEEKS ACTIVE",
            f"  DECISIONS MADE: {len(self.director_log)}",
            "",
            "  OPERATIONAL SUMMARY:",
        ]

        # Count action types
        action_counts = {}
        for entry in self.director_log:
            label = entry.choice_label
            action_counts[label] = action_counts.get(label, 0) + 1

        if action_counts:
            top_action = max(action_counts, key=action_counts.get)
            lines.append(f"  PREFERRED TACTIC: {top_action}")
            lines.append(f"  USES: {action_counts[top_action]}")

        lines.append("")
        lines.append("  RESOURCE FINAL STATE:")
        for name, val in game_state.resources.to_dict().items():
            bar = "[" + "#" * (val // 10) + "." * (10 - val // 10) + "]"
            lines.append(f"  {name:<14} {bar} {val:>3}")

        lines.append("")
        lines.append("  HISTORICAL ASSESSMENT:")

        if len(self.director_log) > 0:
            lines.append(f"  The Director made {len(self.director_log)} documented")
            lines.append(f"  decisions over {game_state.turn} weeks of crisis.")

        lines.append("")
        lines.append("  [END OF REPORT]")
        lines.append("  [ARCHIVE REF: THRESHOLD-2027-FINAL]")

        return lines

    def get_score(self, game_state) -> int:
        """Calculate final score."""
        score = 0
        resources = game_state.resources.to_dict()
        for val in resources.values():
            score += val

        # Bonus for winning type
        if game_state.win_condition:
            if game_state.win_condition == "DIPLOMATIC_VICTORY":
                score += 500
            elif game_state.win_condition == "INTELLIGENCE_SUPREMACY":
                score += 600
            elif game_state.win_condition == "PRAGMATIC_CONTROL":
                score += 400

        # Turn bonus - faster is better
        turn_bonus = max(0, (30 - game_state.turn) * 10)
        score += turn_bonus

        return score
