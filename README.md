# Logic

Fabric client mod with a zoomable grid editor for the circuits of the server plugin. Companion server plugin: `LogicPlugin`.

## Requirements
- Minecraft 1.21.11, Java 21, Fabric Loader 0.18.4 or newer, Fabric API 0.141.6+1.21.11
- Companion plugin `LogicPlugin` 1.0.0 (the mod does nothing without it)

## Build and run
```bash
./gradlew build      # build/libs/Logic-<version>.jar
./gradlew runClient  # Minecraft 1.21.11 test client
```

## Architecture
| Class | Responsibility |
|---|---|
| `LogicClient` | entry point: registers payloads, receivers and the disconnect hook |
| `network.LogicProtocol` | protocol version and limits, mirrors the plugin |
| `network.OpenPayload`, `SignalsPayload`, `ClosePayload` | S2C packets; the format is in each Javadoc |
| `network.SavePayload` | C2S packet with the edit result or a cancel |
| `editor.EditorController` | glue between receivers and the screen; sends the result |
| `editor.EditorModel` | cells being edited and the signals shown |
| `editor.EditorScreen` | grid, palette, input handling and drawing |
| `editor.CellType`, `editor.Cells` | cell kinds, colours and the one-byte packing |

Threading: receivers hop to the client thread with `context.client().execute(...)`; every other class runs on the client thread.

The screen draws everything with `DrawContext.fill` and text, so the mod has no textures. Closing the screen with Escape or the save button sends the edited cells; the cancel button sends a cancel; a server-side close sends nothing. The editor state is advisory: the server validates every save.

## Public API
None.

## Protocol
| Channel | Direction | Protocol | Format |
|---|---|---|---|
| `dvmn2:lg_open` | S2C | 1 | `OpenPayload` Javadoc |
| `dvmn2:lg_signals` | S2C | 1 | `SignalsPayload` Javadoc |
| `dvmn2:lg_close` | S2C | 1 | `ClosePayload` Javadoc |
| `dvmn2:lg_save` | C2S | 1 | `SavePayload` Javadoc |

`LogicProtocol.PROTOCOL` MUST equal `EditorNetwork.PROTOCOL` of the plugin. A packet with another version is decoded to an empty payload and ignored. Registering the S2C receivers makes the client announce `dvmn2:lg_open`, which the plugin uses to detect the mod.

## Data and files
None. The mod has no config file and stores nothing.

## Conventions and constraints
- All texts come from `assets/logic/lang/*.json`; both files MUST keep the same keys.
- The mod has no mixins and no server-side entry point.
- The mod changes no world rendering and downloads nothing, so it has no emergency off command (STYLE §5.5 applies to visual effects).
- Cells and signals use one byte or one bit per cell; the 128x128 limit comes from the 32 KB plugin message limit.

## License
All rights reserved.
