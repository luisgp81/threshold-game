"""
THRESHOLD: Global Crisis - Renderer
CRT effect, HUD drawing, color palette
"""
import pygame
import math
import time
from typing import Optional, Tuple, List, Dict

# Color Palette
BG = (10, 10, 26)
GREEN = (0, 255, 65)
GREEN_DIM = (0, 140, 36)
AMBER = (255, 176, 0)
RED = (255, 45, 45)
CYAN = (0, 220, 255)
WHITE = (220, 220, 220)
DARK_GRAY = (30, 30, 50)
MID_GRAY = (60, 60, 80)
PURPLE = (160, 80, 255)
YELLOW = (255, 230, 0)
SCANLINE_COLOR = (0, 0, 0, 60)

CATEGORY_COLORS = {
    "POLITICAL": CYAN,
    "ECONOMIC": AMBER,
    "TECHNOLOGICAL": PURPLE,
    "MILITARY": RED,
    "HUMANITARIAN": GREEN,
}

URGENCY_COLORS = {
    "LOW": GREEN_DIM,
    "MEDIUM": AMBER,
    "HIGH": RED,
    "CRITICAL": (255, 0, 80),
}

SCREEN_W, SCREEN_H = 1280, 720


class Renderer:
    def __init__(self, screen: pygame.Surface, settings: dict):
        self.screen = screen
        self.settings = settings
        self.font_small = None
        self.font_med = None
        self.font_large = None
        self.font_title = None
        self._scanline_surface: Optional[pygame.Surface] = None
        self._init_fonts()
        self._init_scanlines()
        self.blink_timer = 0.0
        self.blink_state = True

    def _init_fonts(self):
        pygame.font.init()
        # Try system monospace fonts
        mono_candidates = [
            "Courier New", "Courier", "DejaVu Sans Mono",
            "Liberation Mono", "FreeMono", "monospace"
        ]
        found = None
        for name in mono_candidates:
            try:
                test = pygame.font.SysFont(name, 14)
                if test:
                    found = name
                    break
            except Exception:
                continue

        if found:
            self.font_small = pygame.font.SysFont(found, 13)
            self.font_med = pygame.font.SysFont(found, 16)
            self.font_large = pygame.font.SysFont(found, 22)
            self.font_title = pygame.font.SysFont(found, 32, bold=True)
        else:
            self.font_small = pygame.font.Font(None, 18)
            self.font_med = pygame.font.Font(None, 22)
            self.font_large = pygame.font.Font(None, 28)
            self.font_title = pygame.font.Font(None, 40)

    def _init_scanlines(self):
        self._scanline_surface = pygame.Surface(
            (SCREEN_W, SCREEN_H), pygame.SRCALPHA
        )
        self._scanline_surface.fill((0, 0, 0, 0))
        for y in range(0, SCREEN_H, 3):
            pygame.draw.line(
                self._scanline_surface,
                (0, 0, 0, 55),
                (0, y), (SCREEN_W, y)
            )

    def update(self, dt: float):
        self.blink_timer += dt
        if self.blink_timer >= 0.6:
            self.blink_timer = 0.0
            self.blink_state = not self.blink_state

    def clear(self):
        self.screen.fill(BG)

    def apply_crt(self):
        if self.settings.get("crt_effect", True):
            self.screen.blit(self._scanline_surface, (0, 0))
            # Subtle vignette
            vignette = pygame.Surface((SCREEN_W, SCREEN_H), pygame.SRCALPHA)
            vignette.fill((0, 0, 0, 0))
            # corners
            for corner in [(0, 0), (SCREEN_W, 0), (0, SCREEN_H), (SCREEN_W, SCREEN_H)]:
                pygame.draw.circle(vignette, (0, 0, 0, 80), corner, 300)
            self.screen.blit(vignette, (0, 0))

    # ─── Text helpers ────────────────────────────────────────────────────────

    def text(self, txt: str, x: int, y: int, color=GREEN, font=None,
             shadow: bool = False):
        f = font or self.font_med
        if shadow:
            surf_s = f.render(txt, True, (0, 0, 0))
            self.screen.blit(surf_s, (x + 1, y + 1))
        surf = f.render(txt, True, color)
        self.screen.blit(surf, (x, y))
        return surf.get_width()

    def text_center(self, txt: str, y: int, color=GREEN, font=None):
        f = font or self.font_med
        surf = f.render(txt, True, color)
        x = (SCREEN_W - surf.get_width()) // 2
        self.screen.blit(surf, (x, y))
        return surf.get_width()

    def text_wrap(self, txt: str, x: int, y: int, max_width: int,
                  color=GREEN, font=None, line_height: int = 20) -> int:
        """Render word-wrapped text. Returns ending y position."""
        f = font or self.font_small
        words = txt.split(" ")
        line = ""
        cy = y
        for word in words:
            test = (line + " " + word).strip()
            if f.size(test)[0] <= max_width:
                line = test
            else:
                if line:
                    surf = f.render(line, True, color)
                    self.screen.blit(surf, (x, cy))
                    cy += line_height
                line = word
        if line:
            surf = f.render(line, True, color)
            self.screen.blit(surf, (x, cy))
            cy += line_height
        return cy

    # ─── UI Primitives ────────────────────────────────────────────────────────

    def box(self, x: int, y: int, w: int, h: int, color=GREEN_DIM,
            fill=None, border_width: int = 1):
        if fill:
            pygame.draw.rect(self.screen, fill, (x, y, w, h))
        pygame.draw.rect(self.screen, color, (x, y, w, h), border_width)

    def box_titled(self, x: int, y: int, w: int, h: int, title: str,
                   border_color=GREEN_DIM, title_color=GREEN):
        self.box(x, y, w, h, color=border_color)
        # Title bar
        title_surf = self.font_small.render(f" {title} ", True, border_color)
        title_x = x + 8
        # Draw background behind title
        pygame.draw.rect(self.screen, BG, (title_x - 2, y - 1, title_surf.get_width() + 4, 14))
        self.screen.blit(title_surf, (title_x, y - 7))

    def progress_bar(self, x: int, y: int, w: int, h: int,
                     value: int, max_val: int = 100,
                     color=GREEN, bg_color=DARK_GRAY, label: str = ""):
        pct = max(0, min(1.0, value / max_val))
        filled_w = int(w * pct)

        pygame.draw.rect(self.screen, bg_color, (x, y, w, h))
        if filled_w > 0:
            bar_color = color
            if value <= 20:
                bar_color = RED
            elif value <= 40:
                bar_color = AMBER
            pygame.draw.rect(self.screen, bar_color, (x, y, filled_w, h))
        pygame.draw.rect(self.screen, GREEN_DIM, (x, y, w, h), 1)

        if label:
            label_surf = self.font_small.render(f"{value:3d}", True, WHITE)
            self.screen.blit(label_surf, (x + w + 5, y + (h - label_surf.get_height()) // 2))

    def button(self, x: int, y: int, w: int, h: int, label: str,
               active: bool = False, disabled: bool = False,
               color=GREEN_DIM, hover: bool = False) -> pygame.Rect:
        rect = pygame.Rect(x, y, w, h)
        bg = DARK_GRAY
        border = color
        txt_color = GREEN

        if disabled:
            bg = (15, 15, 25)
            border = (40, 40, 60)
            txt_color = (60, 60, 80)
        elif active:
            bg = (0, 60, 20)
            border = GREEN
            txt_color = GREEN
        elif hover:
            bg = (20, 40, 30)
            border = AMBER
            txt_color = AMBER

        pygame.draw.rect(self.screen, bg, rect)
        pygame.draw.rect(self.screen, border, rect, 1)

        surf = self.font_small.render(label, True, txt_color)
        tx = x + (w - surf.get_width()) // 2
        ty = y + (h - surf.get_height()) // 2
        self.screen.blit(surf, (tx, ty))
        return rect

    def divider(self, y: int, color=GREEN_DIM, x1: int = 0, x2: int = SCREEN_W):
        pygame.draw.line(self.screen, color, (x1, y), (x2, y), 1)

    def blink_text(self, txt: str, x: int, y: int, color=GREEN, font=None):
        if self.blink_state:
            self.text(txt, x, y, color, font)

    # ─── HUD Components ───────────────────────────────────────────────────────

    def draw_resource_panel(self, resources, x: int, y: int, w: int):
        """Draw the resources panel on the right side."""
        from .resources import RESOURCE_NAMES, RESOURCE_COLORS
        self.box_titled(x, y, w, 200, "[ AGENCY STATUS ]", GREEN_DIM, GREEN)

        by = y + 18
        for name in RESOURCE_NAMES:
            val = resources.get(name)
            col = RESOURCE_COLORS.get(name, GREEN)
            # Name
            self.text(name[:8], x + 8, by, col, self.font_small)
            # Bar
            self.progress_bar(x + 90, by + 1, w - 105, 12, val,
                               color=col, label=str(val))
            by += 22
        return by

    def draw_turn_info(self, turn: int, max_turns: int, flavor: str,
                       x: int, y: int, w: int):
        """Draw turn counter and flavor text."""
        self.box_titled(x, y, w, 55, "[ TEMPORAL STATUS ]", AMBER, AMBER)
        turn_txt = f"WEEK {turn:02d} / {max_turns:02d}"
        self.text(turn_txt, x + 10, y + 12, AMBER, self.font_large)
        progress = turn / max_turns
        self.progress_bar(x + 8, y + 36, w - 16, 8, int(progress * 100),
                          color=AMBER, bg_color=(40, 20, 0))

    def draw_world_map(self, regions, turn: int, x: int, y: int, w: int, h: int,
                       selected_region=None):
        """Draw a schematic world map with region nodes."""
        self.box_titled(x, y, w, h, "[ GLOBAL OPERATIONS MAP ]", GREEN_DIM, GREEN)

        # Grid lines
        for gx in range(x + 20, x + w - 10, 40):
            pygame.draw.line(self.screen, (15, 25, 15), (gx, y + 8), (gx, y + h - 5), 1)
        for gy in range(y + 20, y + h - 10, 30):
            pygame.draw.line(self.screen, (15, 25, 15), (x + 5, gy), (x + w - 5, gy), 1)

        map_rects = {}
        for region in regions:
            if region.unlocks_turn > 0 and turn < region.unlocks_turn:
                # Show locked region as dim
                rx = x + int(region.map_pos[0] * w)
                ry = y + int(region.map_pos[1] * h)
                pygame.draw.circle(self.screen, (30, 30, 50), (rx, ry), 14)
                self.text("???", rx - 12, ry - 7, (40, 40, 60), self.font_small)
                continue

            rx = x + int(region.map_pos[0] * w)
            ry = y + int(region.map_pos[1] * h)

            # Threat indicator radius
            threat_r = 14 + int(region.threat_level / 10)
            is_critical = region.is_critical
            is_selected = selected_region and selected_region.id == region.id

            # Threat aura
            if is_critical:
                aura_col = (60, 0, 0) if int(time.time() * 2) % 2 == 0 else (100, 0, 0)
                pygame.draw.circle(self.screen, aura_col, (rx, ry), threat_r + 6)

            # Region circle
            base_col = region.color
            if is_selected:
                pygame.draw.circle(self.screen, WHITE, (rx, ry), threat_r + 3, 2)
            pygame.draw.circle(self.screen, base_col, (rx, ry), threat_r)
            pygame.draw.circle(self.screen, (0, 0, 0), (rx, ry), threat_r, 1)

            # Influence indicator
            if region.influence_level > 0:
                infl_r = int(threat_r * region.influence_level / 100)
                pygame.draw.circle(self.screen, PURPLE, (rx, ry), max(1, infl_r), 2)

            # Label
            label = region.short
            lsurf = self.font_small.render(label, True, WHITE)
            self.screen.blit(lsurf, (rx - lsurf.get_width() // 2, ry + threat_r + 3))

            # Threat level text
            threat_txt = f"T:{region.threat_level}"
            tsurf = self.font_small.render(threat_txt, True,
                                           RED if is_critical else AMBER)
            self.screen.blit(tsurf, (rx - tsurf.get_width() // 2, ry - threat_r - 14))

            map_rects[region.id] = pygame.Rect(rx - threat_r, ry - threat_r,
                                               threat_r * 2, threat_r * 2)

        return map_rects

    def draw_event_list(self, events, x: int, y: int, w: int, h: int,
                        selected_idx: int = -1):
        """Draw the list of current events at the bottom."""
        self.box_titled(x, y, w, h, "[ ACTIVE INCIDENTS ]", RED, RED)

        ey = y + 14
        event_rects = []
        for i, event in enumerate(events):
            is_sel = (i == selected_idx)
            cat_col = CATEGORY_COLORS.get(event.category, GREEN)
            urg_col = URGENCY_COLORS.get(event.urgency, AMBER)

            bg = (20, 5, 5) if is_sel else BG
            rect = pygame.Rect(x + 2, ey, w - 4, 28)
            pygame.draw.rect(self.screen, bg, rect)
            if is_sel:
                pygame.draw.rect(self.screen, RED, rect, 1)

            # Category tag
            self.text(f"[{event.category[:4]}]", x + 6, ey + 4,
                      cat_col, self.font_small)
            # Urgency
            self.text(event.urgency[:4], x + 65, ey + 4, urg_col, self.font_small)
            # Title
            title_x = x + 105
            max_title_w = w - 115
            title = event.title
            # Truncate if needed
            tsuf = self.font_small.render(title, True, WHITE)
            if tsuf.get_width() > max_title_w:
                while title and self.font_small.size(title + "...")[0] > max_title_w:
                    title = title[:-1]
                title += "..."
            self.text(title, title_x, ey + 4, WHITE if not is_sel else AMBER,
                      self.font_small)

            event_rects.append(rect)
            ey += 30
            if ey + 28 > y + h:
                break

        return event_rects

    def draw_consequence_messages(self, messages: List[str], x: int, y: int, w: int):
        """Draw pending consequence alerts."""
        if not messages:
            return y
        self.text(">> DELAYED CONSEQUENCES:", x, y, AMBER, self.font_small)
        y += 16
        for msg in messages[:3]:
            self.text(f"  {msg}", x, y, RED, self.font_small)
            y += 14
        return y

    def draw_header(self):
        """Draw the top header bar."""
        pygame.draw.rect(self.screen, DARK_GRAY, (0, 0, SCREEN_W, 28))
        pygame.draw.line(self.screen, GREEN_DIM, (0, 28), (SCREEN_W, 28), 1)
        self.text("THRESHOLD: GLOBAL CRISIS", 10, 6, GREEN, self.font_med)
        self.text("DIRECTOR'S TERMINAL v2.7", SCREEN_W // 2 - 100, 6, GREEN_DIM,
                  self.font_small)
        ts = time.strftime("%Y-%m-%d %H:%M:%S")
        self.text(ts, SCREEN_W - 175, 8, AMBER, self.font_small)

    def draw_footer(self, actions_taken: int, max_actions: int,
                    summit_cd: int, unlocked: list):
        """Draw the bottom footer bar."""
        fy = SCREEN_H - 24
        pygame.draw.rect(self.screen, DARK_GRAY, (0, fy, SCREEN_W, 24))
        pygame.draw.line(self.screen, GREEN_DIM, (0, fy), (SCREEN_W, fy), 1)

        self.text(f"ACTIONS: {actions_taken}/{max_actions}", 10, fy + 5,
                  AMBER if actions_taken >= max_actions else GREEN, self.font_small)

        # Unlocked abilities
        ux = 160
        for ability in unlocked:
            short = ability[:6]
            self.text(f"[{short}]", ux, fy + 5, CYAN, self.font_small)
            ux += 75

        if summit_cd > 0:
            self.text(f"SUMMIT CD:{summit_cd}", ux + 10, fy + 5, AMBER, self.font_small)

        # Controls hint
        hints = "[CLICK EVENT] [CLICK REGION] [L=LOG] [N=NEXT TURN] [ESC=MENU]"
        hsurf = self.font_small.render(hints, True, GREEN_DIM)
        self.screen.blit(hsurf, (SCREEN_W - hsurf.get_width() - 10, fy + 5))
