"""
THRESHOLD: Global Crisis - Main HUD Screen
Map left, resources right, events bottom.
Android: large on-screen buttons replace keyboard shortcuts.
"""
import pygame
from typing import Optional, List
from ..renderer import (Renderer, BG, GREEN, GREEN_DIM, AMBER, WHITE,
                         DARK_GRAY, SCREEN_W, SCREEN_H, CYAN, RED, PURPLE,
                         MID_GRAY)
from ..engine import GameEngine
from ..regions import Region

try:
    import android  # noqa: F401
    _IS_ANDROID = True
except ImportError:
    _IS_ANDROID = False


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
        """Returns (action, data) tuple or None."""
        if event.type == pygame.MOUSEMOTION:
            mx, my = event.pos
            self.hover_region = None
            for rid, rect in self._map_rects.items():
                if rect.collidepoint(mx, my):
                    self.hover_region = self.engine.get_region_by_id(rid)
                    break

        if event.type == pygame.MOUSEBUTTONDOWN and event.button == 1:
            mx, my = event.pos

            # Named action buttons (END TURN, LOG, SETTINGS)
            for name, rect in self._btn_rects.items():
                if rect.collidepoint(mx, my):
                    return (name, None)

            # Region clicks on map
            for rid, rect in self._map_rects.items():
                if rect.collidepoint(mx, my):
                    self.selected_region = self.engine.get_region_by_id(rid)
                    return ("region_detail", self.selected_region)

            # Event row clicks
            for i, rect in enumerate(self._event_rects):
                if rect.collidepoint(mx, my) and i < len(self.engine.current_events):
                    self.selected_event_idx = i
                    return ("event_detail", self.engine.current_events[i])

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

    # ── Layout constants ──────────────────────────────────────────────────────
    # On Android we shrink the map slightly and give more vertical space to
    # the event list and the large touch buttons at the bottom.

    def _layout(self):
        if _IS_ANDROID:
            # Landscape phone: reserve 130px bottom strip for 3 large buttons
            MAP_X, MAP_Y = 8, 34
            MAP_W, MAP_H = 700, 340
            RES_X = MAP_X + MAP_W + 8
            RES_Y = 34
            RES_W = SCREEN_W - RES_X - 8

            BTN_STRIP_H = 130          # tall strip at very bottom for touch
            EVT_Y = MAP_Y + MAP_H + 6
            EVT_H = SCREEN_H - EVT_Y - BTN_STRIP_H - 4
            EVT_X, EVT_W = 8, SCREEN_W - 16
        else:
            MAP_X, MAP_Y = 10, 35
            MAP_W, MAP_H = 720, 390
            RES_X, RES_Y = 740, 35
            RES_W = SCREEN_W - RES_X - 10
            BTN_STRIP_H = 0
            EVT_X, EVT_Y = 10, MAP_Y + MAP_H + 8
            EVT_W = SCREEN_W - 20
            EVT_H = SCREEN_H - EVT_Y - 30

        return (MAP_X, MAP_Y, MAP_W, MAP_H,
                RES_X, RES_Y, RES_W,
                EVT_X, EVT_Y, EVT_W, EVT_H,
                BTN_STRIP_H)

    def draw(self):
        r = self.renderer
        engine = self.engine
        r.clear()
        r.draw_header()

        (MAP_X, MAP_Y, MAP_W, MAP_H,
         RES_X, RES_Y, RES_W,
         EVT_X, EVT_Y, EVT_W, EVT_H,
         BTN_STRIP_H) = self._layout()

        # ── World Map ─────────────────────────────────────────────────────────
        self._map_rects = r.draw_world_map(
            engine.regions, engine.turn, MAP_X, MAP_Y, MAP_W, MAP_H,
            self.selected_region
        )

        # ── Resources ─────────────────────────────────────────────────────────
        r.draw_resource_panel(engine.resources, RES_X, RES_Y, RES_W)

        # ── Turn info ─────────────────────────────────────────────────────────
        r.draw_turn_info(engine.turn, engine.max_turns,
                          engine.flavor_text, RES_X, RES_Y + 210, RES_W)

        # ── Consequence alerts ────────────────────────────────────────────────
        if engine.consequence_messages:
            cm_y = RES_Y + 275
            for msg in engine.consequence_messages[:3]:
                r.text(msg[:38], RES_X, cm_y, AMBER, r.font_small)
                cm_y += 15

        # ── Region tooltip (desktop hover / last tapped on Android) ───────────
        show_tip = self.hover_region or (
            _IS_ANDROID and self.selected_region)
        tip_region = self.hover_region or (
            self.selected_region if _IS_ANDROID else None)
        if tip_region and (tip_region.unlocks_turn == 0 or
                           engine.turn >= tip_region.unlocks_turn):
            reg = tip_region
            tip_x = MAP_X + int(reg.map_pos[0] * MAP_W) + 18
            tip_y = MAP_Y + int(reg.map_pos[1] * MAP_H) - 10
            tip_x = min(tip_x, SCREEN_W - 205)
            tip_y = min(max(tip_y, MAP_Y + 5), MAP_Y + MAP_H - 65)
            tip_h = 70 if _IS_ANDROID else 58
            pygame.draw.rect(r.screen, DARK_GRAY, (tip_x, tip_y, 200, tip_h))
            pygame.draw.rect(r.screen, reg.color, (tip_x, tip_y, 200, tip_h), 1)
            r.text(reg.name, tip_x + 6, tip_y + 5, reg.color, r.font_small)
            r.text(f"STABILITY: {reg.stability_level:3d}", tip_x + 6,
                   tip_y + 22, GREEN, r.font_small)
            r.text(f"THREAT:    {reg.threat_level:3d}", tip_x + 6,
                   tip_y + 38, RED if reg.is_critical else AMBER, r.font_small)
            if _IS_ANDROID:
                r.text(f"INFLUENCE: {reg.influence_level:3d}", tip_x + 6,
                       tip_y + 54, CYAN, r.font_small)

        # ── Action buttons ────────────────────────────────────────────────────
        self._btn_rects = {}
        if _IS_ANDROID:
            self._draw_android_buttons(BTN_STRIP_H)
        else:
            self._draw_desktop_buttons(RES_X, RES_Y, RES_W)

        # ── AI Analysis preview label ─────────────────────────────────────────
        if self.show_preview and engine.preview_events:
            r.text(">> AI ORACLE: PREDICTED NEXT EVENTS:", MAP_X + 5,
                   MAP_Y + MAP_H - 18, CYAN, r.font_small)

        # ── Event list ────────────────────────────────────────────────────────
        self._event_rects = r.draw_event_list(
            engine.current_events, EVT_X, EVT_Y, EVT_W, EVT_H,
            self.selected_event_idx
        )

        # ── Status / flavor text ──────────────────────────────────────────────
        if not _IS_ANDROID:
            status_y = SCREEN_H - 28
            if self.status_msg:
                r.text(f">> {self.status_msg}", EVT_X + 10, status_y,
                       AMBER, r.font_small)
            elif engine.flavor_text:
                r.text(f'"{engine.flavor_text}"', EVT_X + 10, status_y,
                       GREEN_DIM, r.font_small)

            r.draw_footer(
                engine.actions_taken_this_turn,
                engine.max_actions_per_turn,
                engine.summit_cooldown,
                engine.unlocked_abilities,
            )

        r.apply_crt()

    # ── Button helpers ────────────────────────────────────────────────────────

    def _draw_desktop_buttons(self, res_x, res_y, res_w):
        r = self.renderer
        engine = self.engine
        btn_y = res_y + 380
        items = [
            ("next_turn",
             "[ N ] END WEEK",
             GREEN if engine.actions_taken_this_turn > 0 else AMBER),
            ("director_log", "[ L ] DIRECTOR LOG", GREEN_DIM),
            ("settings",     "[ ESC ] SETTINGS",   GREEN_DIM),
        ]
        for name, label, col in items:
            rect = r.button(res_x, btn_y, res_w, 26, label, color=col)
            self._btn_rects[name] = rect
            btn_y += 30

    def _draw_android_buttons(self, strip_h: int):
        """Three wide touch buttons at the bottom of the screen."""
        r = self.renderer
        engine = self.engine

        strip_y = SCREEN_H - strip_h
        # Thin separator line
        pygame.draw.line(r.screen, GREEN_DIM, (0, strip_y), (SCREEN_W, strip_y), 1)

        margin = 6
        btn_h = strip_h - margin * 2
        total_w = SCREEN_W - margin * 4
        btn_w = total_w // 3

        end_col = GREEN if engine.actions_taken_this_turn > 0 else AMBER
        items = [
            ("next_turn",    "END TURN",  end_col),
            ("director_log", "LOG",       GREEN_DIM),
            ("settings",     "MENU",      GREEN_DIM),
        ]
        for i, (name, label, col) in enumerate(items):
            bx = margin + i * (btn_w + margin)
            by = strip_y + margin
            rect = r.button(bx, by, btn_w, btn_h, label, color=col)
            self._btn_rects[name] = rect
