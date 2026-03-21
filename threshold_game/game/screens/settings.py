"""
THRESHOLD: Global Crisis - Settings Screen
"""
import pygame
from ..renderer import (Renderer, BG, GREEN, GREEN_DIM, AMBER, WHITE,
                         DARK_GRAY, MID_GRAY, SCREEN_W, SCREEN_H, CYAN, RED)


class SettingsScreen:
    def __init__(self, renderer: Renderer, settings: dict):
        self.renderer = renderer
        self.settings = settings
        self.selected = 0
        self.options = [
            {"key": "volume", "label": "AUDIO VOLUME", "type": "slider",
             "min": 0, "max": 100, "step": 10},
            {"key": "crt_effect", "label": "CRT SCANLINES", "type": "bool"},
            {"key": "difficulty", "label": "DIFFICULTY",
             "type": "choice", "choices": ["ANALYST", "DIRECTOR", "CRISIS MODE"]},
            {"key": "_back", "label": "RETURN TO MAIN MENU", "type": "action"},
        ]
        self.result = None  # "back" when done

    def handle_event(self, event: pygame.event.Event) -> str:
        if event.type == pygame.KEYDOWN:
            if event.key in (pygame.K_UP, pygame.K_w):
                self.selected = (self.selected - 1) % len(self.options)
            elif event.key in (pygame.K_DOWN, pygame.K_s):
                self.selected = (self.selected + 1) % len(self.options)
            elif event.key in (pygame.K_LEFT, pygame.K_a):
                self._adjust(-1)
            elif event.key in (pygame.K_RIGHT, pygame.K_d):
                self._adjust(1)
            elif event.key in (pygame.K_RETURN, pygame.K_SPACE):
                opt = self.options[self.selected]
                if opt["key"] == "_back":
                    return "back"
                self._adjust(1)
            elif event.key == pygame.K_ESCAPE:
                return "back"

        elif event.type == pygame.MOUSEBUTTONDOWN and event.button == 1:
            mx, my = event.pos
            for i, rect in enumerate(self._option_rects):
                if rect.collidepoint(mx, my):
                    if self.options[i]["key"] == "_back":
                        return "back"
                    self.selected = i
                    self._adjust(1)

        return None

    def _adjust(self, direction: int):
        opt = self.options[self.selected]
        key = opt["key"]
        if key == "_back":
            return
        if opt["type"] == "slider":
            cur = self.settings.get(key, opt["min"])
            new = max(opt["min"], min(opt["max"], cur + direction * opt["step"]))
            self.settings[key] = new
        elif opt["type"] == "bool":
            self.settings[key] = not self.settings.get(key, True)
        elif opt["type"] == "choice":
            choices = opt["choices"]
            cur = self.settings.get(key, choices[0])
            idx = choices.index(cur) if cur in choices else 0
            new_idx = (idx + direction) % len(choices)
            self.settings[key] = choices[new_idx]

    def update(self, dt: float):
        self.renderer.update(dt)
        self._option_rects = []

    def draw(self):
        r = self.renderer
        r.clear()
        r.draw_header()

        y = 80
        r.text_center("[ SYSTEM CONFIGURATION ]", y, AMBER, r.font_title)
        y += 50
        r.divider(y, GREEN_DIM)
        y += 20

        self._option_rects = []
        ox = SCREEN_W // 2 - 250

        for i, opt in enumerate(self.options):
            is_sel = (i == self.selected)
            oy = y + i * 60
            rect = pygame.Rect(ox, oy - 5, 500, 50)
            self._option_rects.append(rect)

            bg = (15, 30, 15) if is_sel else BG
            pygame.draw.rect(r.screen, bg, rect)
            border_col = GREEN if is_sel else GREEN_DIM
            pygame.draw.rect(r.screen, border_col, rect, 1)

            # Label
            r.text(opt["label"], ox + 15, oy + 8,
                   AMBER if is_sel else GREEN, r.font_med)

            # Value
            key = opt["key"]
            if opt["type"] == "slider":
                val = self.settings.get(key, 0)
                bar_x = ox + 270
                r.progress_bar(bar_x, oy + 10, 150, 14, val, 100,
                                color=CYAN, label=str(val))
                r.text("< >", ox + 430, oy + 8, GREEN_DIM, r.font_small)
            elif opt["type"] == "bool":
                val = self.settings.get(key, True)
                val_txt = "ON" if val else "OFF"
                col = GREEN if val else RED
                r.text(val_txt, ox + 270, oy + 8, col, r.font_large)
            elif opt["type"] == "choice":
                choices = opt.get("choices", [])
                val = self.settings.get(key, choices[0] if choices else "")
                r.text(f"< {val} >", ox + 260, oy + 8, CYAN, r.font_med)
            elif opt["type"] == "action":
                r.text("[ENTER]", ox + 270, oy + 8, AMBER, r.font_med)

        r.apply_crt()
