"""
THRESHOLD: Global Crisis — MINIMAL TEST BUILD
If you see a green screen with text, pygame-ce works on your device.
Tap anywhere to quit.
"""
import os
import sys
import time

# Force GLES2 renderer before anything else
os.environ['SDL_RENDER_DRIVER'] = 'opengles2'
os.environ['SDL_HINT_ANDROID_SEPARATE_MOUSE_AND_TOUCH'] = '0'

import pygame

# Detect Android
try:
    import android  # noqa: F401
    IS_ANDROID = True
except ImportError:
    IS_ANDROID = False

LOG = os.path.join(os.path.dirname(os.path.abspath(__file__)), "test.log")


def log(msg):
    try:
        with open(LOG, "a") as f:
            f.write(f"{time.time():.2f} {msg}\n")
    except Exception:
        pass


log(f"Python started. IS_ANDROID={IS_ANDROID}")

try:
    result = pygame.init()
    log(f"pygame.init() = {result}, version={pygame.version.ver}")
except Exception as e:
    log(f"pygame.init() FAILED: {e}")
    sys.exit(1)

# Try display modes in order of compatibility
screen = None
modes = [
    ((0, 0),       pygame.FULLSCREEN,              "FULLSCREEN (0,0)"),
    ((800, 480),   pygame.FULLSCREEN,              "FULLSCREEN 800x480"),
    ((800, 480),   pygame.FULLSCREEN | pygame.SCALED, "FULLSCREEN|SCALED"),
    ((800, 480),   0,                              "windowed 800x480"),
]
for size, flags, name in modes:
    try:
        screen = pygame.display.set_mode(size, flags)
        log(f"display OK: {name} -> {screen.get_size()}")
        break
    except Exception as e:
        log(f"display FAILED: {name} -> {e}")

if screen is None:
    log("ALL display modes failed. Giving up.")
    sys.exit(1)

W, H = screen.get_width(), screen.get_height()
log(f"screen size: {W}x{H}")

try:
    font_big = pygame.font.Font(None, max(32, H // 10))
    font_sm  = pygame.font.Font(None, max(20, H // 18))
    log("fonts OK")
except Exception as e:
    log(f"font FAILED: {e}")
    font_big = font_sm = None

clock = pygame.time.Clock()
start = time.time()
running = True

while running:
    dt = clock.tick(30)
    elapsed = time.time() - start

    for event in pygame.event.get():
        if event.type == pygame.QUIT:
            running = False
        if event.type in (pygame.MOUSEBUTTONDOWN, pygame.FINGERDOWN, pygame.KEYDOWN):
            running = False

    # Auto-quit after 30 seconds
    if elapsed > 30:
        running = False

    screen.fill((0, 60, 0))

    lines = [
        "PYGAME-CE TEST OK",
        f"Screen: {W}x{H}",
        f"Android: {IS_ANDROID}",
        f"pygame: {pygame.version.ver}",
        "TAP TO QUIT",
    ]
    y = H // 6
    for line in lines:
        try:
            surf = font_big.render(line, True, (0, 255, 80))
            screen.blit(surf, (W // 2 - surf.get_width() // 2, y))
            y += surf.get_height() + 10
        except Exception:
            pass

    pygame.display.flip()

log("clean exit")
pygame.quit()
sys.exit(0)
