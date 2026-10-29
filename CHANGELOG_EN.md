# Changelog

[中文](CHANGELOG.md)

## 0.1.2 — 2026-10-27

### Added

- While the wearable toolbox interface is open, the player's arms extend forward as if operating the interface and the head tilts down, with a subtle sway.
- Other players can now see the wearer's arm pose, head tilt, and open-lid model while the toolbox interface is open.
- Added client-side options to disable the arm extension and head tilt animations independently.
- Added a configuration screen to the Mods menu.

### Fixed

- Fixed a trailing space in the mod metadata's `mod_id` that caused a crash on startup due to an invalid modId.

## 0.1.1 — 2026-10-26

### Added

- Added compatibility with Curios' hidden-slot toggle so the worn model is hidden together with the curio.

## 0.1.0 — Initial release

### Added

- Toolboxes can be equipped in the vanilla chest slot or, with Curios installed, in the body curio slot.
- The interface mirrors the placed toolbox's eight compartments and supports filters, quick move, stack sorting, and bulk deposit.
- Holding `Alt` opens the radial menu, where compartments can be bound to hotbar slots and automatically refilled or reclaimed.
- Bound hotbar slots display the same brass-colored border as the placed toolbox.
- The worn model uses a texture matching the toolbox color and switches to the open-lid model when the interface or radial menu is open.
- The worn toolbox takes priority when equipped, and `Shift`-clicking a toolbox in the inventory equips it quickly.
- Toolboxes can be placed in an armor stand's chest slot and render correctly on the stand.
- Curios is an optional dependency, and the chest-slot gameplay works without it.
