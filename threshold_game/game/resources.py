"""
THRESHOLD: Global Crisis - Resource Management
"""
import json
import os
from dataclasses import dataclass, field
from typing import Dict


RESOURCE_NAMES = ["INTEL", "STABILITY", "FUNDS", "INFLUENCE", "PUBLIC_TRUST"]

RESOURCE_COLORS = {
    "INTEL": (0, 200, 255),
    "STABILITY": (0, 255, 65),
    "FUNDS": (255, 176, 0),
    "INFLUENCE": (180, 100, 255),
    "PUBLIC_TRUST": (255, 200, 0),
}

RESOURCE_ICONS = {
    "INTEL": "[I]",
    "STABILITY": "[S]",
    "FUNDS": "[$]",
    "INFLUENCE": "[X]",
    "PUBLIC_TRUST": "[T]",
}


@dataclass
class Resources:
    INTEL: int = 50
    STABILITY: int = 60
    FUNDS: int = 70
    INFLUENCE: int = 50
    PUBLIC_TRUST: int = 65

    def get(self, name: str) -> int:
        return getattr(self, name, 0)

    def set(self, name: str, value: int):
        clamped = max(0, min(100, value))
        setattr(self, name, clamped)

    def apply_delta(self, name: str, delta: int) -> int:
        """Apply delta to resource, return actual change."""
        old = self.get(name)
        new = max(0, min(100, old + delta))
        self.set(name, new)
        return new - old

    def apply_deltas(self, deltas: Dict[str, int]) -> Dict[str, int]:
        """Apply multiple deltas, return dict of actual changes."""
        changes = {}
        for name, delta in deltas.items():
            if name in RESOURCE_NAMES:
                changes[name] = self.apply_delta(name, delta)
        return changes

    def any_zero(self) -> list:
        """Return list of resources at zero."""
        zero = []
        for name in RESOURCE_NAMES:
            if self.get(name) <= 0:
                zero.append(name)
        return zero

    def all_above(self, threshold: int) -> bool:
        return all(self.get(n) > threshold for n in RESOURCE_NAMES)

    def to_dict(self) -> Dict[str, int]:
        return {n: self.get(n) for n in RESOURCE_NAMES}

    @classmethod
    def from_dict(cls, data: Dict[str, int]) -> "Resources":
        r = cls()
        for name, val in data.items():
            r.set(name, val)
        return r

    def difficulty_scale(self, difficulty: str):
        """Scale starting resources by difficulty."""
        if difficulty == "ANALYST":
            # Easier - more resources
            self.INTEL = 60
            self.STABILITY = 70
            self.FUNDS = 80
            self.INFLUENCE = 60
            self.PUBLIC_TRUST = 75
        elif difficulty == "DIRECTOR":
            pass  # Default
        elif difficulty == "CRISIS MODE":
            self.INTEL = 35
            self.STABILITY = 40
            self.FUNDS = 50
            self.INFLUENCE = 35
            self.PUBLIC_TRUST = 45
