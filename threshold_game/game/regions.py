"""
THRESHOLD: Global Crisis - Region Management
"""
from dataclasses import dataclass, field
from typing import List, Optional


REGION_DATA = [
    {
        "id": "NORTH_ATLANTIC",
        "name": "NORTH ATLANTIC",
        "short": "N.ATL",
        "description": "NATO sphere of influence. Financial hubs and legacy alliances strain under populist pressure.",
        "map_pos": (0.18, 0.22),
        "color": (0, 140, 200),
    },
    {
        "id": "EASTERN_BLOC",
        "name": "EASTERN BLOC",
        "short": "E.BLC",
        "description": "Resurgent authoritarian movements fragment old treaty structures. Intelligence assets compromised.",
        "map_pos": (0.55, 0.20),
        "color": (200, 50, 50),
    },
    {
        "id": "MIDDLE_CORRIDOR",
        "name": "MIDDLE CORRIDOR",
        "short": "M.COR",
        "description": "Resource-rich flashpoint. Three proxy conflicts intersect. Humanitarian crisis accelerating.",
        "map_pos": (0.52, 0.42),
        "color": (200, 140, 0),
    },
    {
        "id": "PACIFIC_RIM",
        "name": "PACIFIC RIM",
        "short": "P.RIM",
        "description": "Economic powerhouse. Technology race intensifies. Maritime disputes escalating daily.",
        "map_pos": (0.78, 0.32),
        "color": (0, 180, 120),
    },
    {
        "id": "SOUTHERN_FRONT",
        "name": "SOUTHERN FRONT",
        "short": "S.FNT",
        "description": "Emerging power blocs challenge old order. Climate displacement drives instability.",
        "map_pos": (0.38, 0.65),
        "color": (180, 80, 160),
    },
    {
        "id": "ARCTIC_ZONE",
        "name": "ARCTIC ZONE",
        "short": "ARCT",
        "description": "[CLASSIFIED] New resource frontier. Hidden bases detected. Sovereignty claims contested.",
        "map_pos": (0.50, 0.06),
        "color": (100, 200, 255),
        "unlocks_turn": 10,
    },
]


@dataclass
class Region:
    id: str
    name: str
    short: str
    description: str
    map_pos: tuple
    color: tuple
    stability_level: int = 50
    threat_level: int = 30
    influence_level: int = 20
    unlocks_turn: int = 0
    is_critical: bool = False

    def update_threat(self, delta: int):
        self.threat_level = max(0, min(100, self.threat_level + delta))
        self.is_critical = self.threat_level >= 75

    def update_stability(self, delta: int):
        self.stability_level = max(0, min(100, self.stability_level + delta))

    def update_influence(self, delta: int):
        self.influence_level = max(0, min(100, self.influence_level + delta))

    def is_influenced(self) -> bool:
        return self.influence_level >= 60

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "stability_level": self.stability_level,
            "threat_level": self.threat_level,
            "influence_level": self.influence_level,
            "is_critical": self.is_critical,
        }

    @classmethod
    def from_data(cls, data: dict) -> "Region":
        return cls(
            id=data["id"],
            name=data["name"],
            short=data["short"],
            description=data["description"],
            map_pos=data["map_pos"],
            color=data["color"],
            unlocks_turn=data.get("unlocks_turn", 0),
        )


def create_regions() -> List[Region]:
    regions = []
    for data in REGION_DATA:
        r = Region.from_data(data)
        regions.append(r)
    return regions


def get_critical_regions(regions: List[Region]) -> List[Region]:
    return [r for r in regions if r.is_critical]
