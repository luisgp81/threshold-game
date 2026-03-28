"""
THRESHOLD: Global Crisis - Director's Log Screen
Scrollable history of all decisions.
Android: swipe via FINGERMOTION to scroll; close button on screen.
"""
import pygame
from ..renderer import (Renderer, BG, GREEN, GREEN_DIM, AMBER, WHITE,
                         DARK_GRAY, SCREEN_W, SCREEN_H, CYAN, RED)
from ..narrative import NarrativeManager

try:
    import android  # noqa: F401
    _IS_ANDROID = True
except ImportError:
    _IS_ANDROID = False


class DirectorLogScreen:
    def __init__(self, renderer: Renderer, narrative: NarrativeManager):
        self.renderer = renderer
        self.narrative = narrative
        self.scroll_y: int = 0
        self.line_height: int = 22 if _IS_ANDROID else 18
        self._total_height: int = 0
        self._touch_start_y: int = 0   # for swipe-scroll on Android
        self._close_rect: pygame.Rect = pygame.Rect(0, 0, 1, 1)

    def _max_scroll(self) -> int:
        return max(0, self._total_height - (SCREEN_H - 120))

    def handle_event(self, event: pygame.event.Event) -> bool:
        """Returns True if should close."""
        if event.type == pygame.KEYDOWN:
            if event.key in (pygame.K_ESCAPE, pygame.K_l, pygame.K_BACKSPACE):
                return True
            elif event.key == pygame.K_UP:
                self.scroll_y = max(0, self.scroll_y - self.line_height * 3)
            elif event.key == pygame.K_DOWN:
                self.scroll_y = min(self._max_scroll(),
                                    self.scroll_y + self.line_height * 3)
            elif event.key == pygame.K_HOME:
                self.scroll_y = 0
            elif event.key == pygame.K_END:
                self.scroll_y = self._max_scroll()

        elif event.type == pygame.MOUSEBUTTONDOWN:
            if event.button == 1:
                if self._close_rect.collidepoint(event.pos):
                    return True
            if event.button == 4:
                self.scroll_y = max(0, self.scroll_y - self.line_height * 3)
            elif event.button == 5:
                self.scroll_y = min(self._max_scroll(),
                                    self.scroll_y + self.line_height * 3)

        elif event.type == pygame.MOUSEWHEEL:
            self.scroll_y = max(0, self.scroll_y - event.y * self.line_height * 2)

        # Touch swipe for Android (FINGERMOTION gives dx/dy as fractions of screen)
        elif event.type == pygame.FINGERMOTION:
            dy_px = int(-event.dy * SCREEN_H)
            self.scroll_y = max(0, min(self._max_scroll(),
                                       self.scroll_y + dy_px))

        return False

    def update(self, dt: float):
        self.renderer.update(dt)

    def draw(self):
        r = self.renderer
        r.clear()
        r.draw_header()

        y = 42
        r.text_center("[ DIRECTOR'S OPERATIONAL LOG ]", y, AMBER, r.font_title)
        y += 40
        r.divider(y, GREEN_DIM)
        y += 8

        # Build log content
        entries = self.narrative.get_log_entries()
        if not entries:
            r.text_center("NO LOGGED OPERATIONS.", SCREEN_H // 2, GREEN_DIM, r.font_med)
            r.text_center("[ ESC ] RETURN", SCREEN_H // 2 + 40, GREEN_DIM, r.font_small)
            r.apply_crt()
            return

        # Render to off-screen surface
        content_start_y = y
        visible_h = SCREEN_H - content_start_y - 50

        # Calculate total height
        total_h = 0
        for entry in entries:
            total_h += self.line_height * 2  # turn header + choice
            total_h += 36  # result text (estimated)
            total_h += 10  # separator

        self._total_height = total_h

        # Draw visible entries
        draw_y = content_start_y - self.scroll_y
        r.screen.set_clip(pygame.Rect(0, content_start_y, SCREEN_W, visible_h))

        for entry in entries:
            if draw_y > content_start_y + visible_h:
                break
            if draw_y + 60 > content_start_y:
                # Turn header
                r.text(f"WEEK {entry.turn:02d}", 30, draw_y, AMBER, r.font_med)
                r.text(f"EVENT: {entry.event_title}", 110, draw_y, CYAN, r.font_small)
                draw_y += self.line_height + 2

                # Action taken
                r.text(f"  >> {entry.choice_label}", 30, draw_y, GREEN, r.font_small)
                draw_y += self.line_height

                # Resource changes
                if entry.resource_changes:
                    change_parts = []
                    for res, delta in entry.resource_changes.items():
                        sign = "+" if delta >= 0 else ""
                        change_parts.append(f"{sign}{delta} {res}")
                    r.text("     " + " | ".join(change_parts), 30, draw_y,
                           WHITE, r.font_small)
                    draw_y += self.line_height

                # Narrative result
                draw_y = r.text_wrap(
                    f'  "{entry.narrative_result}"',
                    30, draw_y, SCREEN_W - 60,
                    GREEN_DIM, r.font_small, 15
                )
                draw_y += 4

                # Separator
                pygame.draw.line(r.screen, (30, 40, 30),
                                  (30, draw_y), (SCREEN_W - 30, draw_y), 1)
                draw_y += 10
            else:
                draw_y += self.line_height * 2 + 36 + 10

        r.screen.set_clip(None)

        # Scroll indicator
        if self._total_height > visible_h:
            sb_x = SCREEN_W - 15
            sb_top = content_start_y
            sb_h = visible_h
            pygame.draw.rect(r.screen, DARK_GRAY, (sb_x, sb_top, 8, sb_h))
            thumb_h = max(20, int(sb_h * visible_h / self._total_height))
            thumb_y = sb_top + int(self.scroll_y / self._total_height * sb_h)
            pygame.draw.rect(r.screen, GREEN_DIM, (sb_x, thumb_y, 8, thumb_h))

        # Footer
        if _IS_ANDROID:
            self._close_rect = r.button(8, SCREEN_H - 60, SCREEN_W - 16, 52,
                                         "CLOSE LOG", color=GREEN_DIM)
        else:
            self._close_rect = pygame.Rect(0, 0, 1, 1)  # invisible on desktop
            r.text("[ ESC / L ] CLOSE  |  ARROWS / SCROLL: NAVIGATE",
                   30, SCREEN_H - 28, GREEN_DIM, r.font_small)

        r.apply_crt()
