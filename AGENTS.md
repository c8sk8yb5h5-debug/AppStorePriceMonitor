# Repository development instructions

## Project
Native Android price-monitoring app for AppStorePrice.org. Preserve existing pricing, favorite persistence, notification scheduling, update-checking, signing, and release workflows unless a request explicitly changes them.

## Required workflow
- Before implementing a feature, search GitHub for mature projects that can be reused; check license, maintenance, compatibility, and whether their behavior fits this app. Prefer a small native implementation when that is cleaner than adding a dependency.
- Read the relevant source and build/release configuration before editing. Make the requested code change rather than only proposing a plan.
- Keep changes scoped; avoid unrelated refactors and avoid concurrent/redundant release races.
- After editing, inspect the actual diff/source and check for duplicated statements, stale code, broken listeners, gesture conflicts, clipping, and persistence regressions.
- Verify the exact GitHub Actions build/release runs and source commit SHA. Only provide an APK link after the release asset exists.
- Separate CI/build success from real-device validation. Do not claim device testing unless performed.

## User's design preferences
- Strong preference for **minimalist interfaces** and **meaningful interactive motion**: jelly/spring feedback, liquid fill, fluid transitions, and smooth synchronized animations where useful.
- Prefer compact capsule controls and concise text; avoid large decorative backgrounds, over-explained settings pages, and excess card layers.
- Keep the warm yellow, muted olive/deep green, black, and off-white palette coherent. Respect explicitly requested transparency (70% = alpha 179/255).
- Keep bottom navigation icons aligned to the same visual height. The selected white circular bubble may rise with a spring effect, but must not be clipped. The black navigation notch and main-card notch should share the same animation value.
- The threshold preference is a draggable liquid capsule with a flowing boundary, not a standard slider; keep the percent readable and persist the selected value.
- ChatGPT navigation icons should stay geometric and minimal. Do not add “G” or “+” text badges. The currently selected concept is a simplified four-leaf clover; follow the approved Go/Plus variants and don't change icon concepts without a user request.
- The favorite interaction is tap-to-toggle with star state, a capsule highlight, local persistence, and synchronized state across plans. Do not reintroduce swipe-to-favorite by default.
- Main content must sit within the rounded green-card contour; avoid opaque table backgrounds that protrude through its top corners.
