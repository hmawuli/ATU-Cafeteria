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

## Current audit (ratcheted)

- Token usages in `presentation/`: **161** · hardcoded colour literals: **82**
  (across 10 files).
- Worst offenders: `customer_experience_screen.dart` (23),
  `vendor_dashboard.dart` (18), `reference_design.dart` (10).
- Guard: `test/design_token_budget_test.dart` fails if the literal count
  exceeds the budget, so the drift cannot grow. The budget should be lowered
  as screens are migrated to tokens.

## Migration backlog (lower the budget as you go)

1. `customer_experience_screen.dart` — the customer home (flagship).
2. `vendor_dashboard.dart` — legacy vendor screen.
3. `reference_design.dart` / `professional_widgets.dart` — shared styling.

Verify visually on a device in **both** light and dark mode after migrating,
then reduce the `budget` in `design_token_budget_test.dart`.
