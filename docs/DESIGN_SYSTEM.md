# Design System

The UI standard for the Flutter client: one theme, one set of tokens, shared
components, and both light and dark modes.

## Tokens (`lib/core/theme/app_theme.dart`)

| Token | Use |
|---|---|
| `AppTheme.primary` / `primaryDark` / `primaryLight` | brand blue, headings, tints |
| `AppTheme.accent` | call-to-action highlights (amber) |
| `AppTheme.success` / `warning` / `danger` | status (credited, low stock, errors) |
| `AppTheme.background` / `surface` | page and card backgrounds |
| `AppTheme.textDark` / `textMuted` | primary and secondary text |
| `AppTheme.border` | dividers and outlines |

`AppTheme.light()` and `AppTheme.dark()` provide the two `ThemeData`s; the app
follows the system (MaterialApp `themeMode: ThemeMode.system`). Typography is
defined centrally in the shared `TextTheme`.

## Rules

1. **Use tokens, never literals.** New UI must reference `AppTheme.*` (or a
   `Theme.of(context)` colour/typography) — no `Color(0x…)` in screens.
2. **Dark-mode safe.** Because literals break dark mode, prefer theme-driven
   colours; the light/dark themes are the single source of truth.
3. **Reuse components.** Shared widgets live in `lib/presentation/widgets/`
   (`atu_ui.dart`, `professional_widgets.dart`, `home_header.dart`,
   `order_qr_card.dart`, `recharts_line_chart.dart`, …). Add to those rather
   than re-styling per screen.
4. **Accessibility.** Interactive icons carry `tooltip`/`Semantics` labels
   (e.g. the home logo), touch targets ≥ 48 dp, and text must pass contrast in
   both themes.
5. **Resilience.** Layouts must not overflow at large text scale
   (`MediaQuery.textScaler`); prefer `Expanded`/`Flexible`/`Wrap`.

## Enforcement (ratcheted to zero)

- **Every** colour in `presentation/` now references an `AppTheme` token — the
  budget test (`test/design_token_budget_test.dart`) enforces **0** hardcoded
  `Color(0x…)` literals, so the UI cannot drift back.
- The migration mapped all 49 distinct legacy shades to tokens **preserving
  their exact values** (no visual change): existing tokens were reused, and 44
  shades were added to the extended palette in `app_theme.dart` with
  family/weight names (e.g. `blue600`, `grey900`, `red700A`).
- New UI must use `AppTheme.*` (or `Theme.of(context)`) — never a literal.

## Still to verify on a device

- **Dark mode:** the palette is now centralised, but confirm each screen looks
  right in dark theme on a phone (the app follows the system setting).
- **Text scale & contrast:** check large font scaling for overflow and AA
  contrast in both themes.
- Prefer theme-driven colours over the legacy token aliases when touching a
  screen, so its appearance adapts to light/dark automatically.
