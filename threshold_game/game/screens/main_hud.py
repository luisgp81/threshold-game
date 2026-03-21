"""
THRESHOLD: Global Crisis - Main HUD Screen
Map left, resources right, events bottom
"""
import pygame
from typing import Optional, List
from ..renderer import (Renderer, BG, GREEN, GREEN_DIM, AMBER, WHITE,
                         DARK_GRAY, SCREEN_W, SCREEN_H, CYAN, RED, PURPLE,
                         MID_GRAY)
from ..engine import GameEngine
from ..regions import Region


class MainHUD:
    def __init__(self, renderer: Renderer, engine: GameEngine):
        self.renderer = renderer
        self.engine = engine
        self.selected_event_idx: int = -1
        self.selected_region: Optional[Region] = None
        self.hover_region: Optional[Region] = None
        self._map_rects = {}
        self._event_rects = []
        self._btn_rects = {}
        self.status_msg: str = ""
        self.status_timer: float = 0.0
        self.show_preview: bool = False

    def handle_event(self, event: pygame.event.Event):
        """Returns action string or None."""
        if event.type == pygame.MOUSEMOTION:
            mx, my = event.pos
            self.hover_region = None
            for rid, rect in self._map_rects.items():
                if rect.collidepoint(mx, my):
                    self.hover_region = self.engine.get_region_by_id(rid)
                    break

        if event.type == pygame.MOUSEBUTTONDOWN and event.button == 1:
            mx, my = event.pos
            # Check region clicks
            for rid, rect in self._map_rects.items():
                if rect.collidepoint(mx, my):
                    self.selected_region = self.engine.get_region_by_id(rid)
                    return ("region_detail", self.selected_region)

            # Check event clicks
            for i, rect in enumerate(self._event_rects):
                if rect.collidepoint(mx, my) and i < len(self.engine.current_events):
                    self.selected_event_idx = i
                    return ("event_detail", self.engine.current_events[i])

            # Check buttons
            for name, rect in self._btn_rects.items():
                if rect.collidepoint(mx, my):
                    return (name, None)

        if event.type == pygame.KEYDOWN:
            if event.key == pygame.K_n:
                return ("next_turn", None)
            elif event.key == pygame.K_l:
                return ("director_log", None)
            elif event.key == pygame.K_ESCAPE:
                return ("settings", None)
            elif event.key == pygame.K_p:
                self.show_preview = not self.show_preview
            elif event.key in (pygame.K_1, pygame.K_2, pygame.K_3):
                idx = event.key - pygame.K_1
                if 0 <= idx < len(self.engine.current_events):
                    self.selected_event_idx = idx
                    return ("event_detail", self.engine.current_events[idx])

        return None

    def set_status(self, msg: str, duration: float = 3.0):
        self.status_msg = msg
        self.status_timer = duration

    def update(self, dt: float):
        self.renderer.update(dt)
        if self.status_timer > 0:
            self.status_timer -= dt
            if self.status_timer <= 0:
                self.status_msg = ""

    def draw(self):
        r = self.renderer
        engine = self.engine
        r.clear()
        r.draw_header()

        # Layout
        MAP_X, MAP_Y = 10, 35
        MAP_W, MAP_H = 720, 390
        RES_X, RES_Y = 740, 35
        RES_W = SCREEN_W - RES_X - 10

        EVT_X, EVT_Y = 10, MAP_Y + MAP_H + 8
        EVT_W = SCREEN_W - 20
        EVT_H = SCREEN_H - EVT_Y - 30

        # World Map
        self._map_rects = r.draw_world_map(
            engine.regions, engine.turn, MAP_X, MAP_Y, MAP_W, MAP_H,
            self.selected_region
        )

        # Resources
        ry_end = r.draw_resource_panel(engine.resources, RES_X, RES_Y, RES_W)

        # Turn info
        r.draw_turn_info(engine.turn, engine.max_turns,
                          engine.flavor_text, RES_X, RES_Y + 210, RES_W)

        # Consequence messages
        if engine.consequence_messages:
            cm_y = RES_Y + 280
            for msg in engine.consequence_messages[:3]:
                r.text(msg[:40], RES_X, cm_y, AMBER, r.font_small)
                cm_y += 15

        # Region tooltip
        if self.hover_region and (
                self.hover_region.unlocks_turn == 0 or
                engine.turn >= self.hover_region.unlocks_turn):
            reg = self.hover_region
            tooltip_x = MAP_X + int(reg.map_pos[0] * MAP_W) + 20
            tooltip_y = MAP_Y + int(reg.map_pos[1] * MAP_H) - 10
            tooltip_x = min(tooltip_x, SCREEN_W - 200)
            tooltip_y = min(tooltip_y, MAP_Y + MAP_H - 60)
            pygame.draw.rect(r.screen, DARK_GRAY,
                              (tooltip_x, tooltip_y, 190, 55))
            pygame.draw.rect(r.screen, reg.color,
                              (tooltip_x, tooltip_y, 190, 55), 1)
            r.text(reg.name, tooltip_x + 5, tooltip_y + 5, reg.color, r.font_small)
            r.text(f"STABILITY: {reg.stability_level:3d}", tooltip_x + 5,
                   tooltip_y + 20, GREEN, r.font_small)
            r.text(f"THREAT:    {reg.threat_level:3d}", tooltip_x + 5,
                   tooltip_y + 34, RED if reg.is_critical else AMBER, r.font_small)

        # Action buttons (right panel lower)
        btn_y = RES_Y + 380
        self._btn_rects = {}
        btn_w = RES_W
        btn_items = [
            ("next_turn", "[ N ] END WEEK / ADVANCE TURN", GREEN if engine.actions_taken_this_turn > 0 else AMBER),
            ("director_log", "[ L ] DIRECTOR'S LOG", GREEN_DIM),
            ("settings", "[ ESC ] SETTINGS", GREEN_DIM),
        ]
        for name, label, col in btn_items:
            rect = r.button(RES_X, btn_y, btn_w, 26, label, color=col)
            self._btn_rects[name] = rect
            btn_y += 30

        # AI Analysis preview
        if self.show_preview and engine.preview_events:
            r.text(">> AI ORACLE: PREDICTED NEXT EVENTS:", MAP_X + 5,
                   MAP_Y + MAP_H - 20, CYAN, r.font_small)

        # Event list
        self._event_rects = r.draw_event_list(
            engine.current_events, EVT_X, EVT_Y, EVT_W, EVT_H,
            self.selected_event_idx
        )

        # Status message
        if self.status_msg:
            r.text(f">> {self.status_msg}", EVT_X + 10, SCREEN_H - 28,
                   AMBER, r.font_small)

        # Flavor text
        if engine.flavor_text and not self.status_msg:
            r.text(f'"{engine.flavor_text}"', EVT_X + 10, SCREEN_H - 28,
                   GREEN_DIM, r.font_small)

        r.draw_footer(
            engine.actions_taken_this_turn,
            engine.max_actions_per_turn,
            engine.summit_cooldown,
            engine.unlocked_abilities
        )
        r.apply_crt()
