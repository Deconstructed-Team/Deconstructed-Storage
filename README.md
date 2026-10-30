# Deconstructed Storage

Barrels, limited barrels, chests and shulker boxes of six tiers with upgrades; the controller, storage link and storage I/O that join them; the decoration table and paintbrush. For Minecraft 1.12.2.

Needs [Deconstructed Core](https://github.com/Deconstructed-Team/Deconstructed-Core) of the same version or newer. Works alongside [Deconstructed Backpacks](https://github.com/Deconstructed-Team/Deconstructed-Backpacks), which it doesn't need.

## Origin and license

This is a port to Minecraft 1.12.2 of [Sophisticated Storage](https://github.com/P3pp3rF1y/SophisticatedStorage) by P3pp3rF1y, made from
[`5c3bb643`](https://github.com/P3pp3rF1y/SophisticatedStorage/tree/5c3bb6433966851313649991654fa4e38e891d7b), the last commit of that repository published under the
GNU General Public License v3, on 2025-02-10. Its later commits are under All Rights Reserved; nothing in this
port comes from them.

The GPL v3 under which that commit was published lets anyone use, change and share the code, and the rights it
gives can't be taken back for what was released under it: a later change of license applies only to what comes
after. So this port:

- is under the same GNU General Public License v3 (see [LICENSE](LICENSE)), as a work based on a GPL one must be;
- keeps P3pp3rF1y's copyright of the original code, and states that the code is changed: it is a port to an
  older game version, with much rewritten for it;
- is published with its full source, as the GPL asks;
- doesn't use the original's name for the mod, and isn't made, endorsed or supported by P3pp3rF1y. Don't report
  problems with it to the original mod.

The textures are by Ridanisaurus, made for the original mod and contributed to its repository while it was under
the GPL v3, so they are under it as well.

## Building

```bash
./gradlew build
```

The jar to install is `build/libs/deconstructedstorage-<version>.jar` (not `-dev` or `-sources`).

Deconstructed Core (and Backpacks, for the compatibility) are taken from their GitHub releases, of the versions in `gradle.properties`. To build
against a local copy instead, run `./gradlew publishToMavenLocal -Pmod_version=1.0.1` in it (any version that
isn't released), and pass that version here, as `./gradlew build -Pcore_version=1.0.1`.

Another mod can be built against this one, from its GitHub releases:

```groovy
repositories {
    ivy {
        url = 'https://github.com/Deconstructed-Team/'
        patternLayout {
            artifact '[organisation]/releases/download/v[revision]/[module]-[revision](-[classifier]).[ext]'
        }
        metadataSources { artifact() }
        content { includeGroup 'Deconstructed-Storage' }
    }
}
dependencies {
    implementation rfg.deobf('Deconstructed-Storage:deconstructedstorage:1.0.0')
}
```
