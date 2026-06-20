# Homovore Public!

A Fabric utility mod for Minecraft 1.21.11, written by Leonetic and WiderThanEurasia.

## A note from the author

- Solid client base to skid from. Take what you want, it's a good starting point.
- Half the stuff in here is shitty vibecode, won't pretend otherwise. But the AutoCrystal and the like got some sick shit in it.
- This has some soul put into it and I like the client so I hope you get to enjoy it as much as I do.
- If anyone wants to continue my work, I recommend fixing the offhand module and just adding some cool shit to it. Go ham.

## Building

Requires JDK 21.

```bash
./gradlew build
```

The built jar is written to `build/libs/`.

## Running in a dev environment

```bash
./gradlew runClient
```

## Project layout

- `src/main/java/dev/leonetic/` — mod source
  - `features/modules/` — modules grouped by category (combat, movement, render, …)
  - `manager/` — core managers (modules, rotations, placement, swapping, …)
  - `event/` — the event bus and event definitions
  - `mixin/` — mixins into Minecraft classes
- `src/main/resources/` — `fabric.mod.json`, mixin configs, access widener, shaders, assets

## License

See [LICENSE](LICENSE).
