"""
THRESHOLD: Global Crisis - Event Detail Screen
Shows full event info and 3 action choices
"""
import pygame
from typing import Optional, List
from ..renderer import (Renderer, BG, GREEN, GREEN_DIM, AMBER, WHITE,
                         DARK_GRAY, SCREEN_W, SCREEN_H, CYAN, RED, PURPLE,
                         CATEGORY_COLORS, URGENCY_COLORS)
from ..events import Event, ActionChoice
from ..resources import Resources, RESOURCE_NAMES


class EventScreen:
    def __init__(self, renderer: Renderer):
        self.renderer = renderer
        self.event: Optional[Event] = None
        self.available_choices: List[ActionChoice] = []
        self.selected_choice: int = 0
        self.result_action: Optional[ActionChoice] = None
        self.result_message: str = ""
        self.show_result: bool = False
        self.result_timer: float = 0.0
        self.resources: Optional[Resources] = None
        self._choice_rects: List[pygame.Rect] = []

    def set_event(self, event: Event, choices: List[ActionChoice],
                  resources: Resources):
        self.event = event
        self.available_choices = choices
        self.resources = resources
        self.selected_choice = 0
        self.result_action = None
        self.result_message = ""
        self.show_result = False
        self.result_timer = 0.0

    def handle_event(self, ev: pygame.event.Event):
        """Returns ('choose', choice) or ('back', None) or None."""
        if self.show_result:
            if ev.type in (pygame.KEYDOWN, pygame.MOUSEBUTTONDOWN):
                self.show_result = False
                return ("back", None)
            return None

        if ev.type == pygame.KEYDOWN:
            if ev.key == pygame.K_UP:
                self.selected_choice = (self.selected_choice - 1) % max(1, len(self.available_choices))
            elif ev.key == pygame.K_DOWN:
                self.selected_choice = (self.selected_choice + 1) % max(1, len(self.available_choices))
            elif ev.key in (pygame.K_RETURN, pygame.K_SPACE):
                if self.available_choices:
                    return ("choose", self.available_choices[self.selected_choice])
            elif ev.key == pygame.K_ESCAPE:
                return ("back", None)
            elif ev.key in (pygame.K_1, pygame.K_2, pygame.K_3):
                idx = ev.key - pygame.K_1
                if 0 <= idx < len(self.available_choices):
                    return ("choose", self.available_choices[idx])

        elif ev.type == pygame.MOUSEBUTTONDOWN and ev.button == 1:
            mx, my = ev.pos
            for i, rect in enumerate(self._choice_rects):
                if rect.collidepoint(mx, my) and i < len(self.available_choices):
                    return ("choose", self.available_choices[i])
            # Back button
            if hasattr(self, '_back_rect') and self._back_rect.collidepoint(mx, my):
                return ("back", None)

        return None

    def show_result_text(self, message: str, choice: ActionChoice):
        self.result_message = message
        self.result_action = choice
        self.show_result = True
        self.result_timer = 0.0

    def update(self, dt: float):
        self.renderer.update(dt)
        if self.show_result:
            self.result_timer += dt

    def _can_afford(self, choice: ActionChoice) -> bool:
        if not self.resources:
            return True
        for res, cost in choice.resource_cost.items():
            if self.resources.get(res) < cost:
                return False
        return True

    def _resource_delta_text(self, choice: ActionChoice) -> List[tuple]:
        """Returns list of (text, color) for resource impact."""
        parts = []
        for res, cost in choice.resource_cost.items():
            parts.append((f"-{cost} {res}", RED))
        for res, gain in choice.resource_gain.items():
            parts.append((f"+{gain} {res}", GREEN))
        return parts

    def draw(self):
        if not self.event:
            return

        r = self.renderer
        r.clear()
        r.draw_header()

        if self.show_result:
            self._draw_result()
            r.apply_crt()
            return

        ev = self.event
        cat_col = CATEGORY_COLORS.get(ev.category, GREEN)
        urg_col = URGENCY_COLORS.get(ev.urgency, AMBER)

        # Left panel: event details
        panel_x, panel_y = 30, 45
        panel_w = 560
        r.box_titled(panel_x, panel_y, panel_w, SCREEN_H - 80,
                     "[ INCIDENT REPORT ]", cat_col, cat_col)

        y = panel_y + 20
        # Category + urgency badges
        r.text(f"[{ev.category}]", panel_x + 10, y, cat_col, r.font_small)
        r.text(f"[{ev.urgency}]", panel_x + 130, y, urg_col, r.font_small)
        r.text(f"REGION: {ev.region_affected.replace('_', ' ')}",
               panel_x + 230, y, CYAN, r.font_small)
        y += 25

        # Title
        r.text(ev.title, panel_x + 10, y, AMBER, r.font_large)
        y += 36

        r.divider(y, cat_col, panel_x + 5, panel_x + panel_w - 5)
        y += 12

        # Flavor intro
        if ev.flavor_intro:
            y = r.text_wrap(f'"{ev.flavor_intro}"', panel_x + 10, y,
                             panel_w - 20, GREEN_DIM, r.font_small, 16)
            y += 8

        # Description
        y = r.text_wrap(ev.description, panel_x + 10, y,
                         panel_w - 20, WHITE, r.font_small, 18)
        y += 12

        r.divider(y, GREEN_DIM, panel_x + 5, panel_x + panel_w - 5)
        y += 10

        if ev.hidden_consequence_turn > 0:
            r.text(f">> CONSEQUENCE PENDING IN {ev.hidden_consequence_turn} WEEKS",
                   panel_x + 10, y, AMBER, r.font_small)
            y += 16

        # Right panel: actions
        act_x = panel_x + panel_w + 20
        act_w = SCREEN_W - act_x - 30
        r.box_titled(act_x, panel_y, act_w, SCREEN_H - 80,
                     "[ RESPONSE OPTIONS ]", AMBER, AMBER)

        ay = panel_y + 20
        self._choice_rects = []
        mx, my = pygame.mouse.get_pos()

        for i, choice in enumerate(self.available_choices):
            is_sel = (i == self.selected_choice)
            can_afford = self._can_afford(choice)
            is_hover = False

            ch_h = 110
            ch_rect = pygame.Rect(act_x + 8, ay, act_w - 16, ch_h)
            is_hover = ch_rect.collidepoint(mx, my)
            self._choice_rects.append(ch_rect)

            if is_sel:
                bg = (20, 40, 10) if can_afford else (40, 10, 10)
                border = GREEN if can_afford else RED
            elif is_hover:
                bg = (15, 30, 8)
                border = AMBER
            else:
                bg = DARK_GRAY
                border = GREEN_DIM if can_afford else (50, 20, 20)

            pygame.draw.rect(r.screen, bg, ch_rect)
            pygame.draw.rect(r.screen, border, ch_rect, 1)

            # Number key hint
            r.text(f"[{i+1}]", act_x + 12, ay + 8, AMBER, r.font_small)

            # Risk badge
            risk_col = {"LOW": GREEN, "MEDIUM": AMBER, "HIGH": RED}.get(
                choice.risk_level, GREEN)
            r.text(f"RISK:{choice.risk_level}", act_x + 40, ay + 8,
                   risk_col, r.font_small)

            # Label
            label_col = WHITE if can_afford else (100, 50, 50)
            r.text(choice.label, act_x + 12, ay + 26, label_col, r.font_med)

            # Description (truncated)
            desc = choice.description
            r.text_wrap(desc, act_x + 12, ay + 48, act_w - 24,
                         GREEN_DIM if can_afford else (70, 50, 50),
                         r.font_small, 14)

            # Resource impact
            deltas = self._resource_delta_text(choice)
            dx = act_x + 12
            dy = ay + ch_h - 22
            for txt, col in deltas:
                if not can_afford:
                    col = (100, 50, 50)
                w = r.text(txt, dx, dy, col, r.font_small)
                dx += w + 10
                if dx > act_x + act_w - 20:
                    break

            if not can_afford:
                r.text("[INSUFFICIENT RESOURCES]", act_x + 12, dy,
                        (150, 50, 50), r.font_small)

            ay += ch_h + 8

        # Back button
        back_y = SCREEN_H - 55
        self._back_rect = r.button(act_x + 8, back_y, act_w - 16, 30,
                                    "[ ESC ] DEFER INCIDENT", color=GREEN_DIM)

        # Controls hint
        r.text("ARROW KEYS / CLICK TO SELECT  |  ENTER / 1-3 TO CONFIRM",
               panel_x + 10, SCREEN_H - 30, GREEN_DIM, r.font_small)

        r.apply_crt()

    def _draw_result(self):
        r = self.renderer
        y = 200
        r.text_center("[ ACTION EXECUTED ]", y, AMBER, r.font_title)
        y += 60

        if self.result_action:
            r.text_center(self.result_action.label, y, GREEN, r.font_large)
            y += 40

        r.divider(y, GREEN_DIM, 200, SCREEN_W - 200)
        y += 20

        r.text_wrap(self.result_message, 200, y, SCREEN_W - 400,
                     WHITE, r.font_med, 24)
        y += 80

        r.blink_text(">> PRESS ANY KEY TO CONTINUE _", SCREEN_W // 2 - 200,
                      y + 40, AMBER, r.font_med)
