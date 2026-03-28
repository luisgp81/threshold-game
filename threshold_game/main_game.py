"""
THRESHOLD: Global Crisis
Main entry point — connects all modules
Supports desktop (mouse/keyboard) and Android (touch).
"""
import os
import sys
import time
import traceback
import pygame

# Ensure the threshold_game package is importable when run directly
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# Detect Android
try:
    import android  # noqa: F401
    IS_ANDROID = True
except ImportError:
    IS_ANDROID = False

# Safe K_AC_BACK constant — may not exist in all pygame-ce builds
_AC_BACK = getattr(pygame, 'K_AC_BACK', 270)

# ── Error helpers ─────────────────────────────────────────────────────────────

def _log_path():
    if IS_ANDROID:
        # Try multiple writable locations on Android
        for p in ("/sdcard/threshold_error.log",
                  "/sdcard/Android/data/org.threshold.thresholdglobalcrisis/threshold_error.log"):
            try:
                with open(p, "a") as f:
                    f.write("")
                return p
            except Exception:
                pass
    return os.path.join(os.path.dirname(os.path.abspath(__file__)), "threshold_error.log")


def _log_error(msg: str):
    try:
        with open(_log_path(), "a") as f:
            f.write(msg + "\n")
    except Exception:
        pass


def _show_error_screen(tb: str):
    """Display a traceback on-screen so the user can photograph it."""
    try:
        if not pygame.get_init():
            pygame.init()
        if pygame.display.get_surface() is None:
            try:
                scr = pygame.display.set_mode((0, 0), pygame.FULLSCREEN)
            except Exception:
                try:
                    scr = pygame.display.set_mode((800, 480))
                except Exception:
                    return
        else:
            scr = pygame.display.get_surface()

        font = pygame.font.Font(None, 22)
        scr.fill((10, 0, 0))

        header = font.render("THRESHOLD CRASH — PHOTOGRAPH THIS SCREEN", True, (255, 80, 80))
        scr.blit(header, (5, 5))

        lines = tb.replace("\r", "").split("\n")
        y = 32
        for line in lines:
            # Wrap long lines
            while len(line) > 58:
                surf = font.render(line[:58], True, (255, 200, 200))
                scr.blit(surf, (5, y))
                y += 20
                line = "  " + line[58:]
                if y > scr.get_height() - 30:
                    break
            if y > scr.get_height() - 30:
                break
            surf = font.render(line, True, (255, 200, 200))
            scr.blit(surf, (5, y))
            y += 20

        pygame.display.flip()

        # Keep error visible for up to 60 seconds (tap/click to dismiss)
        deadline = time.time() + 60
        while time.time() < deadline:
            for ev in pygame.event.get():
                if ev.type in (pygame.QUIT, pygame.MOUSEBUTTONDOWN,
                               pygame.FINGERDOWN, pygame.KEYDOWN):
                    return
            time.sleep(0.1)
    except Exception:
        pass


# ── Game imports (after error helpers are defined) ────────────────────────────

try:
    from game.engine import GameEngine, GameState
    from game.renderer import Renderer, SCREEN_W, SCREEN_H
    from game.screens.intro import IntroScreen
    from game.screens.main_hud import MainHUD
    from game.screens.event_screen import EventScreen
    from game.screens.game_over import GameOverScreen
    from game.screens.settings import SettingsScreen
    from game.screens.director_log import DirectorLogScreen
except Exception as _import_err:
    _tb = traceback.format_exc()
    _log_error("IMPORT ERROR:\n" + _tb)
    pygame.init()
    _show_error_screen("IMPORT ERROR:\n" + _tb)
    sys.exit(1)

DATA_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data")
TARGET_FPS = 60


def _init_display():
    """Initialise display surface with Android-safe fallback chain."""
    if IS_ANDROID:
        # Try FULLSCREEN + SCALED (logical 1280×720 scaled to device)
        try:
            s = pygame.display.set_mode((SCREEN_W, SCREEN_H),
                                        pygame.FULLSCREEN | pygame.SCALED)
            _log_error(f"display OK: FULLSCREEN|SCALED {s.get_size()}")
            return s
        except Exception as e:
            _log_error(f"FULLSCREEN|SCALED failed: {e}")
        # Fallback: plain FULLSCREEN at fixed size
        try:
            s = pygame.display.set_mode((SCREEN_W, SCREEN_H), pygame.FULLSCREEN)
            _log_error(f"display OK: FULLSCREEN fixed {s.get_size()}")
            return s
        except Exception as e:
            _log_error(f"FULLSCREEN fixed failed: {e}")
        # Last resort
        s = pygame.display.set_mode((SCREEN_W, SCREEN_H))
        _log_error(f"display OK: no flags {s.get_size()}")
        return s
    else:
        try:
            return pygame.display.set_mode((SCREEN_W, SCREEN_H), pygame.SCALED)
        except Exception:
            return pygame.display.set_mode((SCREEN_W, SCREEN_H))


def main():
    try:
        _run()
    except Exception:
        tb = traceback.format_exc()
        _log_error("RUNTIME ERROR:\n" + tb)
        _show_error_screen("RUNTIME ERROR:\n" + tb)
        sys.exit(1)


def _run():
    pygame.init()
    _log_error(f"pygame.init OK, version={pygame.version.ver}")
    pygame.display.set_caption("THRESHOLD: Global Crisis")

    settings = {
        "volume": 70,
        "crt_effect": True,
        "difficulty": "DIRECTOR",
        "fullscreen": IS_ANDROID,
    }

    screen = _init_display()
    clock = pygame.time.Clock()

    # Try to set window icon (skip if assets missing)
    try:
        icon_path = os.path.join(os.path.dirname(__file__), "assets", "icon.png")
        if os.path.exists(icon_path):
            icon = pygame.image.load(icon_path)
            pygame.display.set_icon(icon)
    except Exception:
        pass

    _log_error("creating Renderer...")
    renderer = Renderer(screen, settings)
    _log_error("creating GameEngine...")
    engine = GameEngine(DATA_PATH)
    engine.settings = settings

    import json
    try:
        with open(os.path.join(DATA_PATH, "narrative.json"), "r") as f:
            narrative_data = json.load(f)
    except Exception:
        narrative_data = {}

    _log_error("creating screens...")
    intro_screen = IntroScreen(renderer, narrative_data)
    main_hud = MainHUD(renderer, engine)
    event_screen = EventScreen(renderer)
    game_over_screen = GameOverScreen(renderer, engine)
    settings_screen = SettingsScreen(renderer, settings)
    director_log_screen = DirectorLogScreen(renderer, engine.narrative)

    current_screen = "intro"
    prev_screen = "intro"

    engine.start_new_game(settings["difficulty"])
    engine.state = GameState.INTRO
    _log_error("entering game loop")

    running = True
    while running:
        dt = clock.tick(TARGET_FPS) / 1000.0
        dt = min(dt, 0.1)

        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                running = False
                break

            # Global fullscreen toggle (desktop only)
            if event.type == pygame.KEYDOWN and event.key == pygame.K_F11 and not IS_ANDROID:
                settings["fullscreen"] = not settings["fullscreen"]
                if settings["fullscreen"]:
                    screen = pygame.display.set_mode(
                        (SCREEN_W, SCREEN_H), pygame.FULLSCREEN | pygame.SCALED)
                else:
                    screen = pygame.display.set_mode((SCREEN_W, SCREEN_H), pygame.SCALED)
                renderer.screen = screen
                continue

            # Android back button
            if IS_ANDROID and event.type == pygame.KEYDOWN and event.key == _AC_BACK:
                if current_screen in ("event_detail", "director_log", "settings"):
                    current_screen = "main_hud"
                    continue
                elif current_screen == "main_hud":
                    running = False
                    break

            # Map touch to mouse
            if event.type == pygame.FINGERDOWN:
                sw, sh = screen.get_width(), screen.get_height()
                fx, fy = int(event.x * sw), int(event.y * sh)
                pygame.event.post(pygame.event.Event(
                    pygame.MOUSEBUTTONDOWN, button=1, pos=(fx, fy)))
                continue
            if event.type == pygame.FINGERUP:
                sw, sh = screen.get_width(), screen.get_height()
                fx, fy = int(event.x * sw), int(event.y * sh)
                pygame.event.post(pygame.event.Event(
                    pygame.MOUSEBUTTONUP, button=1, pos=(fx, fy)))
                continue

            if current_screen == "intro":
                if intro_screen.handle_event(event):
                    current_screen = "main_hud"
                    engine.state = GameState.MAIN_HUD

            elif current_screen == "main_hud":
                result = main_hud.handle_event(event)
                if result:
                    action, data = result
                    if action == "event_detail" and data:
                        choices = engine.event_manager.filter_choices(
                            data, engine.unlocked_abilities)
                        event_screen.set_event(data, choices, engine.resources)
                        prev_screen = "main_hud"
                        current_screen = "event_detail"
                    elif action == "region_detail":
                        pass
                    elif action == "next_turn":
                        result_state = engine.advance_turn()
                        engine.save_game()
                        if result_state == "GAME_OVER":
                            game_over_screen.setup(False)
                            current_screen = "game_over"
                        elif result_state == "VICTORY":
                            game_over_screen.setup(True)
                            current_screen = "game_over"
                    elif action == "director_log":
                        director_log_screen.scroll_y = 0
                        current_screen = "director_log"
                    elif action == "settings":
                        current_screen = "settings"

            elif current_screen == "event_detail":
                result = event_screen.handle_event(event)
                if result:
                    action, data = result
                    if action == "choose" and data:
                        success, message = engine.take_action(event_screen.event, data)
                        if success:
                            event_screen.show_result_text(message, data)
                            win_lose = engine.check_win_lose()
                            if win_lose == "GAME_OVER":
                                game_over_screen.setup(False)
                                current_screen = "game_over"
                            elif win_lose == "VICTORY":
                                game_over_screen.setup(True)
                                current_screen = "game_over"
                        else:
                            main_hud.set_status(message)
                            current_screen = "main_hud"
                    elif action == "back":
                        current_screen = "main_hud"

            elif current_screen == "game_over":
                result = game_over_screen.handle_event(event)
                if result == "restart":
                    engine.start_new_game(settings["difficulty"])
                    intro_screen = IntroScreen(renderer, narrative_data)
                    main_hud = MainHUD(renderer, engine)
                    event_screen = EventScreen(renderer)
                    director_log_screen = DirectorLogScreen(renderer, engine.narrative)
                    current_screen = "main_hud"
                elif result == "menu":
                    engine.start_new_game(settings["difficulty"])
                    intro_screen = IntroScreen(renderer, narrative_data)
                    main_hud = MainHUD(renderer, engine)
                    event_screen = EventScreen(renderer)
                    director_log_screen = DirectorLogScreen(renderer, engine.narrative)
                    current_screen = "intro"

            elif current_screen == "settings":
                result = settings_screen.handle_event(event)
                if result == "back":
                    renderer.settings = settings
                    engine.settings = settings
                    current_screen = "main_hud"

            elif current_screen == "director_log":
                if director_log_screen.handle_event(event):
                    current_screen = "main_hud"

        if current_screen == "intro":
            intro_screen.update(dt)
        elif current_screen == "main_hud":
            main_hud.update(dt)
        elif current_screen == "event_detail":
            event_screen.update(dt)
        elif current_screen == "game_over":
            game_over_screen.update(dt)
        elif current_screen == "settings":
            settings_screen.update(dt)
        elif current_screen == "director_log":
            director_log_screen.update(dt)

        if current_screen == "intro":
            intro_screen.draw()
        elif current_screen == "main_hud":
            main_hud.draw()
        elif current_screen == "event_detail":
            event_screen.draw()
        elif current_screen == "game_over":
            game_over_screen.draw()
        elif current_screen == "settings":
            settings_screen.draw()
        elif current_screen == "director_log":
            director_log_screen.draw()

        pygame.display.flip()

    pygame.quit()
    sys.exit(0)


if __name__ == "__main__":
    main()
