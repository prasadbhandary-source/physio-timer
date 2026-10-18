# Physio Timer

A simple Android timer for home physiotherapy, built for a Pixel phone.

## What it does
- Preloaded exercises:
  - Chin tucks — 10 reps × 10 s hold
  - Left head turn — 5 reps × 10 s hold
  - Left head tilt — 5 reps × 10 s hold
  - Left nose to armpit — 5 reps × 10 s hold
- **Start session** runs every ticked exercise in order; ▶ on a card runs just that one.
- Get-ready countdown before each exercise, then HOLD / RELAX for each rep with a big countdown ring.
- Beeps, spoken prompts ("Rep 3. Hold." / "Relax.") and vibration, so you don't need to watch the screen.
- Pause, Skip step, Next exercise. Screen stays on while the timer is open.
- Add, edit, delete and reorder exercises (name, reps, hold seconds, rest seconds).
- Logs completed sessions (today / last 7 days / last session).

## Build
Push to `main` on GitHub. The **Build APK** workflow builds the app and publishes
`PhysioTimer.apk` as a GitHub Release (and as a workflow artifact).

## Install on the Pixel
Open the latest release on your phone, download `PhysioTimer.apk`, tap it and allow
"Install unknown apps" for your browser when asked. Later builds install over the
top and keep your exercises.

Tip: beeps and voice use **media volume**.
