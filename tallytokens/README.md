# Tally tokens

Resource-only Android library with the design tokens of Tally, the DiamaneOS interface: colours,
shapes, sizes, type, springs, timings and lamp forms. SystemUI, Launcher3, WindowManager-Shell and
Settings read the same values by adding `tallytokens` to their `static_libs`. Resource overlays
and SettingsLib do not link it; they get literal values generated from the same spec.

These files are generated from the Tally token spec in the DiamaneOS design repository
(`design/tokens/`, spec sha256 635e2284c0412702). Do not edit them here: change the spec and
regenerate. Only `lint-baseline.xml`, `OWNERS`, `METADATA` may be kept here by hand.

- Colours point at the framework's dynamic-colour roles and palette tones (`values/` uses the
  `*_light` roles, `values-night/` the `*_dark` roles), so the seed, the palette style and dark
  theme reach them. Palette tones and fixed roles also resolve per theme; the step 2 palette
  style makes the lamp one colour in both. Only the sensor, capture, keycap relief and app key
  colours are fixed values. `color/` holds roles at an alpha (scrims, the press layer).
- The status bar privacy and capture chips and the fullscreen dots use the fixed
  `tally_sensor_light`/`_dark` and `tally_capture_light`/`_dark` colours, chosen by the area under
  them (the light variants over a light area), never by night mode.
- The Colour icon style's app keys are three arrays in the same order: `tally_app_key_packages`
  (DiamaneOS's own apps), `tally_app_key_plates` (each key's colour) and `tally_app_key_glyphs`
  (its glyph's colour), the same in light and dark. Launcher3 and SystemUI read them; an app that
  is not listed, or not a system app, keeps its own icon.
- Every resource name starts with `tally_` (text appearances with `TextAppearance.Tally`).
- Springs and other ratios are float dimens: read them with `Resources.getFloat()`.
- Line heights are sp dimens: apply them with `android:lineHeight` in XML or
  `TextView.setLineHeight(TypedValue.COMPLEX_UNIT_SP, value)`, never with
  `getDimensionPixelSize()`, which stops them following non-linear font scaling.
- Lamps are vector drawables `tally_lamp_<form>_<size>` (10, 12, 14, 16 dp). Each is the lamp plus the
  live halo's reach, `tally_lamp_box_<size>` wide: offset the view by −(box − size) / 2 on each
  side. Tint the ring forms or the edge-less `tally_lamp_on_plain_<size>` and
  `tally_lamp_live_plain_<size>` for lamps drawn in one colour (sensor lamps, lit fields, toasts).
- Privacy-indicator timings and the chips' height and icon size are not here: stock SystemUI's
  constants and resources stay their owners, and Tally's tokens and overlays never set them.
- The library holds no code, permissions or components.
