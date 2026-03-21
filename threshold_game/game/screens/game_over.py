"""
THRESHOLD: Global Crisis - Game Over & Victory Screens
Cinematic + final score report
"""
import pygame
import time
from ..renderer import (Renderer, BG, GREEN, GREEN_DIM, AMBER, WHITE,
                         DARK_GRAY, SCREEN_W, SCREEN_H, CYAN, RED, PURPLE)
from ..engine import GameEngine


class GameOverScreen:
    def __init__(self, renderer: Renderer, engine: GameEngine):
        self.renderer = renderer
        self.engine = engine
        self.is_victory = False
        self.lines: list = []
        self.scroll_y: int = 0
        self.reveal_timer: float = 0.0
        self.lines_revealed: int = 0
        self.ready: bool = False
        self._btn_rects = {}

    def setup(self, is_victory: bool):
        self.is_victory = is_victory
        self.scroll_y = 0
        self.reveal_timer = 0.0
        self.lines_revealed = 0
        self.ready = False
        self.lines = self.engine.narrative.generate_declassified_report(self.engine)

    def handle_event(self, event: pygame.event.Event):
        """Returns 'menu', 'restart', or None."""
        if event.type == pygame.KEYDOWN:
            if event.key == pygame.K_UP:
                self.scroll_y = max(0, self.scroll_y - 20)
            elif event.key == pygame.K_DOWN:
                self.scroll_y += 20
            elif event.key == pygame.K_SPACE:
                # Reveal all instantly
                self.lines_revealed = len(self.lines)
                self.ready = True
            elif event.key in (pygame.K_r,):
                return "restart"
            elif event.key == pygame.K_ESCAPE:
                return "menu"

        elif event.type == pygame.MOUSEBUTTONDOWN and event.button == 1:
            mx, my = event.pos
            if "restart" in self._btn_rects and self._btn_rects["restart"].collidepoint(mx, my):
                return "restart"
            if "menu" in self._btn_rects and self._btn_rects["menu"].collidepoint(mx, my):
                return "menu"
            if event.button == 4:  # scroll up
                self.scroll_y = max(0, self.scroll_y - 20)
            if event.button == 5:  # scroll down
                self.scroll_y += 20

        elif event.type == pygame.MOUSEWHEEL:
            self.scroll_y = max(0, self.scroll_y - event.y * 20)

        return None

    def update(self, dt: float):
        self.renderer.update(dt)
        if self.lines_revealed < len(self.lines):
            self.reveal_timer += dt
            if self.reveal_timer >= 0.06:
                self.reveal_timer = 0.0
                self.lines_revealed += 1
                if self.lines_revealed >= len(self.lines):
                    self.ready = True

    def draw(self):
        r = self.renderer
        r.clear()

        # Background tint
        if self.is_victory:
            tint = pygame.Surface((SCREEN_W, SCREEN_H), pygame.SRCALPHA)
            tint.fill((0, 20, 0, 40))
            r.screen.blit(tint, (0, 0))
        else:
            tint = pygame.Surface((SCREEN_W, SCREEN_H), pygame.SRCALPHA)
            tint.fill((20, 0, 0, 40))
            r.screen.blit(tint, (0, 0))

        r.draw_header()

        # Title
        y = 50
        if self.is_victory:
            title = "MISSION ACCOMPLISHED"
            sub = {
                "DIPLOMATIC_VICTORY": "DIPLOMATIC VICTORY",
                "INTELLIGENCE_SUPREMACY": "INTELLIGENCE SUPREMACY",
                "PRAGMATIC_CONTROL": "PRAGMATIC CONTROL",
            }.get(self.engine.win_condition or "", "VICTORY")
            title_col = GREEN
            sub_col = CYAN
        else:
            title = "OPERATION TERMINATED"
            sub = "MISSION FAILURE"
            title_col = RED
            sub_col = AMBER

        r.text_center(title, y, title_col, r.font_title)
        y += 42
        r.text_center(sub, y, sub_col, r.font_large)
        y += 30

        # Score
        score_txt = f"FINAL SCORE: {self.engine.score:,}"
        r.text_center(score_txt, y, AMBER, r.font_large)
        y += 40

        r.divider(y, title_col if self.is_victory else RED)
        y += 15

        # Declassified report
        report_surface = pygame.Surface((SCREEN_W - 40, 500))
        report_surface.fill(BG)

        ry = 10
        for i, line in enumerate(self.lines[:self.lines_revealed]):
            col = WHITE
            if line.startswith("="):
                col = GREEN_DIM
            elif "DECLASSIFIED" in line or "THRESHOLD" in line or "EYES ONLY" in line:
                col = AMBER
            elif line.startswith("  RESOURCE") or line.startswith("  DURATION") or \
                 line.startswith("  DECISIONS") or line.startswith("  PREFERRED"):
                col = CYAN
            elif "#" in line and "[" in line:
                # progress bar line
                col = GREEN

            surf = r.font_small.render(line, True, col)
            report_surface.blit(surf, (20, ry - self.scroll_y))
            ry += 16

        # Clip and blit
        clip_rect = pygame.Rect(20, y, SCREEN_W - 40, SCREEN_H - y - 100)
        r.screen.set_clip(clip_rect)
        r.screen.blit(report_surface, (20, y))
        r.screen.set_clip(None)

        # Buttons
        btn_y = SCREEN_H - 80
        self._btn_rects = {}

        if self.ready:
            r1 = r.button(SCREEN_W // 2 - 270, btn_y, 240, 40,
                           "[ R ] NEW GAME", color=GREEN)
            self._btn_rects["restart"] = r1
            r2 = r.button(SCREEN_W // 2 + 30, btn_y, 240, 40,
                           "[ ESC ] MAIN MENU", color=GREEN_DIM)
            self._btn_rects["menu"] = r2
        else:
            r.text_center("[ SPACE ] SKIP TO REPORT", btn_y + 10, GREEN_DIM, r.font_small)

        # Flavor text from narrative
        if self.is_victory:
            narrative = self.engine.narrative.victory_texts.get(
                self.engine.win_condition or "PRAGMATIC_CONTROL", "")
        else:
            reason_key = "RESOURCE_ZERO"
            if "THREE SIMULTANEOUS" in (self.engine.lose_reason or ""):
                reason_key = "CRITICAL_REGIONS"
            elif "TURN LIMIT" in (self.engine.lose_reason or ""):
                reason_key = "TURN_LIMIT"
            narrative = self.engine.narrative.defeat_texts.get(reason_key, "")

        if narrative and self.ready:
            r.text_wrap(narrative, 60, btn_y + 50, SCREEN_W - 120,
                         GREEN_DIM, r.font_small, 16)

        r.apply_crt()
