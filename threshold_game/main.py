"""
THRESHOLD: Global Crisis
Main entry point — connects all modules
Supports desktop (mouse/keyboard) and Android (touch).
"""
import os
import sys
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

from game.engine import GameEngine, GameState
from game.renderer import Renderer, SCREEN_W, SCREEN_H
from game.screens.intro import IntroScreen
from game.screens.main_hud import MainHUD
from game.screens.event_screen import EventScreen
from game.screens.game_over import GameOverScreen
from game.screens.settings import SettingsScreen
from game.screens.director_log import DirectorLogScreen

DATA_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data")

TARGET_FPS = 60


def _log_error(msg: str):
    """Write crash info to a file that can be retrieved for diagnosis."""
    try:
        if IS_ANDROID:
            log_path = "/sdcard/threshold_error.log"
        else:
            log_path = os.path.join(os.path.dirname(__file__), "threshold_error.log")
        with open(log_path, "a") as f:
            f.write(msg + "\n")
    except Exception:
        pass


def _init_display():
    """Initialise display surface with Android-safe fallback chain."""
    if IS_ANDROID:
        # First try: FULLSCREEN only (most compatible with pygame-ce on Android)
        try:
            screen = pygame.display.set_mode((0, 0), pygame.FULLSCREEN)
            return screen
        except Exception as e:
            _log_error(f"set_mode FULLSCREEN failed: {e}")
        # Second try: fixed size fullscreen
        try:
            screen = pygame.display.set_mode((SCREEN_W, SCREEN_H), pygame.FULLSCREEN)
            return screen
        except Exception as e:
            _log_error(f"set_mode fixed FULLSCREEN failed: {e}")
        # Last resort
        return pygame.display.set_mode((SCREEN_W, SCREEN_H))
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
        _log_error(tb)
        raise


def _run():
    pygame.init()
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

    renderer = Renderer(screen, settings)
    engine = GameEngine(DATA_PATH)
    engine.settings = settings

    # Load narrative data for intro
    import json
    try:
        with open(os.path.join(DATA_PATH, "narrative.json"), "r") as f:
            narrative_data = json.load(f)
    except Exception:
        narrative_data = {}

    # Screens
    intro_screen = IntroScreen(renderer, narrative_data)
    main_hud = MainHUD(renderer, engine)
    event_screen = EventScreen(renderer)
    game_over_screen = GameOverScreen(renderer, engine)
    settings_screen = SettingsScreen(renderer, settings)
    director_log_screen = DirectorLogScreen(renderer, engine.narrative)

    current_screen = "intro"
    prev_screen = "intro"

    # Start game immediately on first play
    engine.start_new_game(settings["difficulty"])
    # But show intro first
    engine.state = GameState.INTRO

    running = True
    while running:
        dt = clock.tick(TARGET_FPS) / 1000.0
        dt = min(dt, 0.1)  # Cap delta time

        # ─── Event handling ──────────────────────────────────────────────────
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

            # Android back button → ESC behaviour
            if IS_ANDROID and event.type == pygame.KEYDOWN and event.key == _AC_BACK:
                if current_screen in ("event_detail", "director_log", "settings"):
                    current_screen = "main_hud"
                    continue
                elif current_screen == "main_hud":
                    running = False
                    break

            # Map finger-touch to mouse events on Android
            if event.type == pygame.FINGERDOWN:
                # Convert normalised coords to actual screen pixels
                sw = screen.get_width()
                sh = screen.get_height()
                fx = int(event.x * sw)
                fy = int(event.y * sh)
                synth = pygame.event.Event(pygame.MOUSEBUTTONDOWN,
                                           button=1, pos=(fx, fy))
                pygame.event.post(synth)
                continue
            if event.type == pygame.FINGERUP:
                sw = screen.get_width()
                sh = screen.get_height()
                fx = int(event.x * sw)
                fy = int(event.y * sh)
                synth = pygame.event.Event(pygame.MOUSEBUTTONUP,
                                           button=1, pos=(fx, fy))
                pygame.event.post(synth)
                continue

            # ── Intro ────────────────────────────────────────────────────────
            if current_screen == "intro":
                if intro_screen.handle_event(event):
                    current_screen = "main_hud"
                    engine.state = GameState.MAIN_HUD

            # ── Main HUD ─────────────────────────────────────────────────────
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
                        pass  # handled inline via tooltip for now
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

            # ── Event Detail ─────────────────────────────────────────────────
            elif current_screen == "event_detail":
                result = event_screen.handle_event(event)
                if result:
                    action, data = result
                    if action == "choose" and data:
                        # Execute the choice
                        success, message = engine.take_action(
                            event_screen.event, data
                        )
                        if success:
                            event_screen.show_result_text(message, data)
                            # Check for immediate win/lose after action
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

            # ── Game Over / Victory ──────────────────────────────────────────
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

            # ── Settings ─────────────────────────────────────────────────────
            elif current_screen == "settings":
                result = settings_screen.handle_event(event)
                if result == "back":
                    # Apply difficulty change if game not started
                    renderer.settings = settings
                    engine.settings = settings
                    current_screen = "main_hud"

            # ── Director Log ─────────────────────────────────────────────────
            elif current_screen == "director_log":
                if director_log_screen.handle_event(event):
                    current_screen = "main_hud"

        # ─── Updates ─────────────────────────────────────────────────────────
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

        # ─── Drawing ─────────────────────────────────────────────────────────
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
