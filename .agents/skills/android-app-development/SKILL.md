---
name: android-app-development
description: Develop, improve, test, and release Android apps using the user's preferred workflow and UI style. Use whenever asked to build or modify an Android app, implement UI interactions, fix UI bugs, or update an APK.
---

# Android App Development Skill

Use this workflow whenever the user asks to develop or modify an Android app.

## 1. Inspect before editing

1. Identify the active repository, app language/framework, package name, current version, build system, release process, and the exact files involved.
2. Read the repository's `AGENTS.md`, `README.md`, and relevant implementation/build files before changing code.
3. **Search GitHub for mature, reusable projects before implementing a new feature.** Compare maintenance, license, API/behavior fit, Android compatibility, dependencies, and accessibility. Reuse only when it is a good fit; explain briefly if native implementation is safer or lighter.
4. Summarize the intended change and preserve existing behavior outside the requested scope.

## 2. User's UI and interaction preferences

Treat these as durable design preferences unless the user explicitly overrides them.

- **Minimalist UI:** prioritize clear hierarchy, ample breathing room, restrained color, a small number of elements, and concise labels. Avoid excessive explanatory copy, decorative blocks, nested cards, and unnecessary borders.
- **Interaction matters:** use intentional micro-interactions where they improve feedback: spring/bounce settling, jelly-like compression/stretch, shared motion, liquid fill, and smooth easing. Do not add animation merely for decoration; animations must not interfere with touch, scrolling, or readability.
- **Capsule-first layout:** rounded capsule controls are preferred for settings, thresholds, actions, and highlighted rows. Use consistent radii, subtle shadows, and coherent transparency.
- **Fluid threshold control:** when a percent/price-change threshold is requested, prefer a draggable liquid-filled capsule with a moving/waving boundary and settling motion instead of a conventional SeekBar. Make the number readable over both filled and unfilled areas, support touch and accessibility, clamp the range, and save the selected value.
- **Navigation:** keep all bottom navigation icons aligned to the same visual baseline/height. A selected floating white circular icon may rise and spring, but its parent must not clip it. Keep the selected notch in the black navigation bar and any related card notch synchronized through one shared animation value.
- **Icon design:** prefer simple geometric symbols. Do not add text badges/subscripts such as “G” or “+” to distinguish ChatGPT plans. Do not assume hollow-vs-filled alone is an acceptable plan distinction; follow the user's explicitly selected icon designs or ask before changing them. In this project, the selected ChatGPT icon direction is a simplified four-leaf clover: use the currently approved Go/Plus variants and preserve the navigation animation.
- **Color language for this project:** warm pale yellow background, muted olive/deep green main surface, black bottom bar, off-white/white capsules. For surfaces requested at 70% opacity, use alpha 179/255 consistently rather than mixing opacity values.
- **Consistency:** the main ranking page and settings page should share layout language. Keep content inside the rounded card contour; avoid opaque child backgrounds that square off the card's rounded top edge.
- **Favorite interaction:** this project uses reliable tap-to-toggle favorites with visible star state, capsule highlight, local persistence and shared favorite state across plans. Do not revert to row swipe gestures unless the user explicitly asks.
- Do not assume that a screenshot has been tested on a physical device. Distinguish source-level checks, CI build success, and real-device visual validation.

## 3. Implementation principles

1. Prefer the simplest implementation that meets the intent; keep dependencies minimal.
2. When custom animation is appropriate, use existing platform animation/Canvas/Path primitives where they provide the required feel without a large dependency.
3. Make gestures unambiguous. Check nested scrolling, click handling, touch interception, clipping, content descriptions, contrast, and small-screen sizing.
4. Preserve existing preferences/data when changing settings UI. Do not silently change defaults, persistence keys, monitoring behavior, endpoints, permissions, or release signing.
5. Keep UI labels concise, but keep controls accessible with meaningful content descriptions and sufficient touch targets.
6. If repository work is requested, make the code change rather than only explaining a plan, unless blocked by missing access or essential information.
7. Avoid multiple overlapping commits or concurrent release races where possible. Ensure a release corresponds to the intended source commit.

## 4. Verification and delivery

1. Inspect the diff/source after editing for duplicate statements, stale code, incorrect listener references, accidental behavior changes, and clipping/measurement issues.
2. Run the available tests/build. If the only accessible build is GitHub Actions, check the exact workflow run and source SHA.
3. When updating an APK, verify both build and signed release workflows, then confirm that the newest GitHub Release contains the APK asset. Never infer success from a queued or in-progress run.
4. Provide a direct APK link only after confirming that the exact release asset exists. Include the commit and build/run links.
5. Clearly state what is implemented, what passed automated build checks, and what still requires physical-device testing. Never claim real-device validation unless it happened.
6. Keep user-facing progress and final responses concise, practical, and in Chinese unless the user switches languages.

## 5. Preferred working sequence

Inspect repository → search GitHub for reusable implementations → identify design/behavior constraints → implement the smallest coherent change → review diff → build/test → publish if requested → verify release asset → report exact outcome.
