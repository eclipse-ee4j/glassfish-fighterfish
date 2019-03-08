## FighterFish

OSGi Application Development using GlassFish Server.

## Build

You must build Helidon using JDK 8. You also need Maven. We recommend 3.5 or
 newer.

**Full build**
```bash
$ mvn install
```

**Checkstyle**
```bash
$ mvn validate  -Pcheckstyle
```

**Copyright**

```bash
$ mvn validate  -Pcopyright
```

**Spotbugs**

```bash
$ mvn verify  -Pspotbugs
```