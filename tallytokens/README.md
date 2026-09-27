# Tally tokens

Resource-only Android library with the design tokens of Tally, the DiamaneOS interface: colours,
shapes, sizes, type, springs, timings and lamp forms. SystemUI, Launcher3 and the DiamaneOS
overlays read the same values by adding `tallytokens` to their `static_libs`.

These files are generated from the Tally token spec in the DiamaneOS design repository
(`design/tokens/`). Do not edit them here: change the spec and regenerate.

- Colours point at the framework's dynamic-colour roles and palette tones (`values/` uses the
  `*_light` roles, `values-night/` the `*_dark` roles), so the seed, the palette style and dark
  theme reach them. Only the sensor, capture and keycap relief colours are fixed values, each with
  a light and a night variant. `color/` holds roles at an alpha (scrims, the press layer).
- Every resource name starts with `tally_` (text appearances with `TextAppearance.Tally`).
- Springs and other ratios are float dimens: read them with `Resources.getFloat()`.
- Lamps are vector drawables `tally_lamp_<form>_<size>`; each is the lamp plus the live halo's
  reach, so centre it on the lamp's nominal size.
- Privacy-indicator timings and the chips' minimum sizes are not here on purpose: the shell keeps
  stock SystemUI's constants and resources for them.
- The library holds no code, permissions or components.
