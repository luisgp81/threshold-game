"""
THRESHOLD: Global Crisis — DIAGNOSTIC BUILD
Tests pygame-ce + shows Android Toast at each step.
"""
import os
import sys
import time

# ── Step 1: write a file immediately so we know Python ran ────────────────────
_log_paths = [
    "/sdcard/Download/threshold_diag.log",
    "/sdcard/threshold_diag.log",
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "diag.log"),
]
_logfile = None
for _p in _log_paths:
    try:
        with open(_p, "w") as _f:
            _f.write(f"[{time.time():.1f}] Python started. sys.version={sys.version}\n")
        _logfile = _p
        break
    except Exception:
        pass


def log(msg):
    if _logfile:
        try:
            with open(_logfile, "a") as f:
                f.write(f"[{time.time():.1f}] {msg}\n")
        except Exception:
            pass


def toast(msg):
    """Show an Android Toast message — works even if pygame is broken."""
    try:
        from jnius import autoclass
        activity = autoclass("org.kivy.android.PythonActivity").mActivity
        Toast = autoclass("android.widget.Toast")
        t = Toast.makeText(activity, msg, Toast.LENGTH_LONG)
        t.show()
    except Exception as e:
        log(f"toast failed: {e}")


# ── Detect Android ─────────────────────────────────────────────────────────────
try:
    import android  # noqa: F401
    IS_ANDROID = True
except ImportError:
    IS_ANDROID = False

log(f"IS_ANDROID={IS_ANDROID}")
if IS_ANDROID:
    toast("Step 1: Python started OK")

# ── Force software renderer to avoid OpenGL/EGL crashes ───────────────────────
os.environ['SDL_RENDER_DRIVER'] = 'software'
os.environ['SDL_HINT_ANDROID_SEPARATE_MOUSE_AND_TOUCH'] = '0'
log("SDL env vars set")

# ── Import pygame ──────────────────────────────────────────────────────────────
try:
    import pygame
    log(f"pygame imported: version={pygame.version.ver}")
    if IS_ANDROID:
        toast(f"Step 2: pygame {pygame.version.ver} imported")
except Exception as e:
    log(f"FATAL: import pygame failed: {e}")
    if IS_ANDROID:
        toast(f"CRASH: import pygame failed: {e}")
    sys.exit(1)

# ── Init display only (skip mixer to avoid audio crash) ───────────────────────
try:
    pygame.display.init()
    log("pygame.display.init() OK")
    if IS_ANDROID:
        toast("Step 3: display.init OK")
except Exception as e:
    log(f"FATAL: display.init failed: {e}")
    if IS_ANDROID:
        toast(f"CRASH: display.init: {e}")
    sys.exit(1)

try:
    pygame.font.init()
    log("pygame.font.init() OK")
except Exception as e:
    log(f"font.init failed (non-fatal): {e}")

# ── Try display modes ──────────────────────────────────────────────────────────
screen = None
modes = [
    ((800, 480), pygame.FULLSCREEN,               "FULLSCREEN 800x480"),
    ((0, 0),     pygame.FULLSCREEN,               "FULLSCREEN (0,0)"),
    ((800, 480), pygame.FULLSCREEN | pygame.SCALED, "FULLSCREEN|SCALED"),
    ((800, 480), 0,                               "windowed 800x480"),
]
for size, flags, name in modes:
    try:
        screen = pygame.display.set_mode(size, flags)
        log(f"display OK: {name} -> actual {screen.get_size()}")
        if IS_ANDROID:
            toast(f"Step 4: display OK: {screen.get_size()}")
        break
    except Exception as e:
        log(f"display FAILED: {name}: {e}")

if screen is None:
    log("FATAL: all display modes failed")
    if IS_ANDROID:
        toast("CRASH: all display modes failed — check diag.log")
    sys.exit(1)

W, H = screen.get_width(), screen.get_height()
log(f"screen={W}x{H}")

# ── Fonts ──────────────────────────────────────────────────────────────────────
try:
    font_big = pygame.font.Font(None, max(32, H // 10))
    font_sm  = pygame.font.Font(None, max(20, H // 18))
    log("fonts OK")
except Exception as e:
    log(f"font create failed: {e}")
    font_big = font_sm = None

# ── Main loop ──────────────────────────────────────────────────────────────────
clock = pygame.time.Clock()
start = time.time()
running = True
log("entering main loop")
if IS_ANDROID:
    toast("Step 5: entering main loop — GREEN SCREEN SHOULD APPEAR")

while running:
    clock.tick(30)
    if time.time() - start > 30:
        running = False

    for event in pygame.event.get():
        if event.type == pygame.QUIT:
            running = False
        if event.type in (pygame.MOUSEBUTTONDOWN, pygame.FINGERDOWN, pygame.KEYDOWN):
            running = False

    screen.fill((0, 60, 0))
    lines = [
        "DIAGNOSTIC BUILD",
        f"Screen: {W}x{H}",
        f"Android: {IS_ANDROID}",
        f"pygame: {pygame.version.ver}",
        f"Renderer: software",
        "TAP TO QUIT",
    ]
    y = H // 8
    for line in lines:
        if font_big:
            try:
                s = font_big.render(line, True, (0, 255, 80))
                screen.blit(s, (W // 2 - s.get_width() // 2, y))
                y += s.get_height() + 8
            except Exception:
                pass
    pygame.display.flip()

log("clean exit")
pygame.quit()
sys.exit(0)
