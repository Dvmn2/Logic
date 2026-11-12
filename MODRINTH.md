# Logic

Client mod that adds a large grid editor for logic circuits on servers running the LogicPlugin plugin.

## Features
- Zoomable and pannable grid for circuits up to 128x128 cells.
- Palette with wires, splitters, bridges and the gates AND, OR, NOT, XOR, NAND, NOR and XNOR.
- Cell direction control, painting by dragging and a lock tool for circuit creators.
- Live display of which cells carry a signal while you edit.
- English and Russian interface.

## Requirements
- Minecraft 1.21.11, Fabric Loader, Fabric API
- A server with the LogicPlugin plugin; the mod does nothing on other servers

## Installation
1. Install Fabric Loader for Minecraft 1.21.11.
2. Put `Logic-<version>.jar` and Fabric API into the `mods` folder.

## Commands
None.

## Configuration
None. The mod has no config file.

## Controls
| Action | Control |
|---|---|
| Open the editor | Right-click the circuit board in the world (sneak and right-click to set a circuit up if you own it) |
| Paint with the selected tool | Left mouse button, drag to draw lines |
| Rotate a cell | Right-click the cell |
| Pan | Drag with the right or middle mouse button |
| Zoom | Mouse wheel |
| Rotate the tool direction | `R` or the direction button |
| Save and close | Escape or the save button |
| Close without saving | Cancel button |

## Privacy and safety
The mod sends only the circuit you edited to the server you are playing on. It does not download anything, does not read your files and does not change how the game world looks.

## Troubleshooting
- **The server says the Logic mod is required**: check that the mod and Fabric API are in the `mods` folder and that the mod version matches the server's plugin.
- **Changes were not saved**: the server rejects cells that are locked or not allowed in this circuit. Ask the circuit owner.
- **The editor closed on its own**: another player took over, the circuit was deleted or the editing time ran out.

## License
All rights reserved.
