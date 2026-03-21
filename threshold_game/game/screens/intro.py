"""
THRESHOLD: Global Crisis - Intro Screen
Terminal boot animation + typewriter briefing
"""
import pygame
import time
import random
from ..renderer import (Renderer, BG, GREEN, GREEN_DIM, AMBER, WHITE,
                         DARK_GRAY, SCREEN_W, SCREEN_H, CYAN, RED)


class IntroScreen:
    def __init__(self, renderer: Renderer, narrative_data: dict):
        self.renderer = renderer
        self.briefing_lines = narrative_data.get("intro_briefing", [
            "THRESHOLD COMMAND SYSTEM v2.7.4",
            "SECURE BOOT SEQUENCE INITIATED...",
            "",
            "WELCOME, DIRECTOR.",
            "",
            "PRESS ANY KEY TO BEGIN..."
        ])

        self.phase = "boot"  # boot -> typewriter -> ready
        self.boot_progress = 0.0
        self.boot_lines = [
            "BIOS v4.1.7 ... OK",
            "MEMORY CHECK: 1048576K ... OK",
            "ENCRYPTION MODULE: AES-512 ... ACTIVE",
            "BIOMETRIC SCANNER ... VERIFIED",
            "SECURE COMMS LINK ... ESTABLISHED",
            "SATELLITE UPLINK ... CONNECTED",
            "THREAT DATABASE ... LOADING [||||||||||||] 100%",
            "AGENCY PROTOCOLS ... AUTHORIZED",
            "DIRECTOR PROFILE ... LOADED",
            "",
            "SYSTEM READY.",
        ]
        self.boot_shown = 0
        self.boot_timer = 0.0
        self.boot_interval = 0.12

        self.typed_chars = 0
        self.type_timer = 0.0
        self.type_speed = 0.03  # seconds per char
        self.full_text = "\n".join(self.briefing_lines)
        self.ready = False

        # Glitch effect
        self.glitch_timer = 0.0
        self.glitch_active = False

    def update(self, dt: float):
        self.renderer.update(dt)
        self.glitch_timer += dt

        if self.glitch_timer > random.uniform(3.0, 7.0):
            self.glitch_active = True
            self.glitch_timer = 0.0
        elif self.glitch_active:
            self.glitch_active = False

        if self.phase == "boot":
            self.boot_timer += dt
            if self.boot_timer >= self.boot_interval:
                self.boot_timer = 0.0
                if self.boot_shown < len(self.boot_lines):
                    self.boot_shown += 1
                else:
                    self.phase = "typewriter"

        elif self.phase == "typewriter":
            self.type_timer += dt
            if self.type_timer >= self.type_speed:
                self.type_timer = 0.0
                chars_to_add = max(1, int(self.type_speed * 200))
                self.typed_chars = min(
                    self.typed_chars + chars_to_add,
                    len(self.full_text)
                )
                if self.typed_chars >= len(self.full_text):
                    self.phase = "ready"

    def handle_event(self, event: pygame.event.Event) -> bool:
        """Returns True if screen should advance."""
        if event.type == pygame.KEYDOWN or (
                event.type == pygame.MOUSEBUTTONDOWN and event.button == 1):
            if self.phase == "boot":
                self.boot_shown = len(self.boot_lines)
                self.phase = "typewriter"
                return False
            elif self.phase == "typewriter":
                self.typed_chars = len(self.full_text)
                self.phase = "ready"
                return False
            elif self.phase == "ready":
                return True
        return False

    def draw(self):
        self.renderer.clear()

        # Glitch scanline
        if self.glitch_active:
            gy = random.randint(50, SCREEN_H - 50)
            pygame.draw.rect(self.renderer.screen,
                             (0, 40, 0), (0, gy, SCREEN_W, 3))

        y = 40
        r = self.renderer

        if self.phase == "boot" or (self.phase in ("typewriter", "ready")
                                     and self.boot_shown == len(self.boot_lines)):
            # Draw boot lines
            for i, line in enumerate(self.boot_lines[:self.boot_shown]):
                col = GREEN if line.startswith("SYSTEM READY") else GREEN_DIM
                if "OK" in line or "ACTIVE" in line or "VERIFIED" in line or \
                   "ESTABLISHED" in line or "CONNECTED" in line or \
                   "AUTHORIZED" in line or "LOADED" in line:
                    # color the status
                    parts = line.rsplit("...", 1)
                    if len(parts) == 2:
                        r.text(parts[0] + "...", 80, y, GREEN_DIM, r.font_small)
                        r.text(parts[1].strip(), 80 + r.font_small.size(parts[0] + "...")[0] + 5,
                               y, GREEN, r.font_small)
                    else:
                        r.text(line, 80, y, col, r.font_small)
                else:
                    r.text(line, 80, y, col, r.font_small)
                y += 16

        if self.phase in ("typewriter", "ready"):
            y = 260
            r.divider(y - 10, GREEN_DIM)
            # Draw typed portion of briefing
            visible = self.full_text[:self.typed_chars]
            lines = visible.split("\n")
            for line in lines:
                if line.startswith("THRESHOLD") or line.startswith("SECURE") or \
                   line.startswith("WELCOME") or line.startswith("PRESS"):
                    col = AMBER if "WELCOME" in line or "PRESS" in line else CYAN
                    r.text(line, 80, y, col, r.font_med)
                elif line.startswith("DATE") or line.startswith("CLASS"):
                    r.text(line, 80, y, RED, r.font_small)
                elif line == "":
                    pass
                else:
                    r.text(line, 80, y, GREEN, r.font_small)
                y += 20

            if self.phase == "ready":
                # Blinking cursor / press key prompt
                r.blink_text(">> PRESS ANY KEY TO ACCEPT COMMAND _", 80, y + 10,
                              AMBER, r.font_med)

        r.apply_crt()
