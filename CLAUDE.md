# CLAUDE.md

Eclipse IDE plugin providing syntax highlighting, content assist, and navigation for Velocity Template Language (VTL) files (`.vtl`, `.vm`). Built with Maven/Tycho 5 targeting Eclipse 2025-12, Java 21.

## Module Structure

```
eclipse-velocity-editor/
├── io.github.nebosuke.velocityeditor/         # Main OSGi plugin (Tycho)
├── io.github.nebosuke.velocityeditor.feature/ # Eclipse feature packaging (Tycho)
├── io.github.nebosuke.velocityeditor.updatesite/ # P2 update site (Tycho)
└── io.github.nebosuke.velocityeditor.tests/   # Unit tests (plain Maven JAR, NOT a Tycho module)
```

The tests module is **not** listed in the root `pom.xml` `<modules>` — it is a standalone Maven project compiled against the plugin source via `build-helper-maven-plugin`.

## Build Commands

```bash
# Full build — generates P2 update site
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -DskipTests clean verify

# Unit tests only (run from the tests module root)
cd io.github.nebosuke.velocityeditor.tests && mvn test

# Version bump (usually done by CI; run manually only if needed)
python3 scripts/bump_version.py
```

The generated update site lives at `io.github.nebosuke.velocityeditor.updatesite/target/repository/`.

## Key Source Paths

| Area | Path |
|---|---|
| Main editor class | `io.github.nebosuke.velocityeditor/src/.../editors/VelocityEditor.java` |
| Editor configuration (syntax, assist) | `.../editors/VelocityConfiguration.java` |
| Content assist proposals | `.../assist/VelocityContentAssistProcessor.java` |
| Macro resolution | `.../assist/MacroDefinitionResolver.java` |
| Document partitioning | `.../scanners/VelocityPartitionScanner.java` |
| Syntax highlighting rules | `.../scanners/VelocityCodeScanner.java` |
| Color/theme management | `.../preferences/ColorManager.java` |
| Preference constants | `.../preferences/PreferenceConstants.java` |
| Plugin lifecycle | `.../Activator.java` |
| Eclipse extension points | `io.github.nebosuke.velocityeditor/plugin.xml` |
| OSGi bundle metadata | `io.github.nebosuke.velocityeditor/META-INF/MANIFEST.MF` |

All Java sources are under `io.github.nebosuke.velocityeditor/src/io/github/nebosuke/velocityeditor/`.

## Versioning

Every push to `main` triggers `.github/workflows/publish.yml`, which:
1. Runs `scripts/bump_version.py` to increment the patch version across all files.
2. Commits the bump with `[skip ci]`.
3. Builds the update site and publishes to the `gh-pages` branch.

**Do not hand-edit version strings.** Always use `scripts/bump_version.py`. It updates these files atomically:
- `io.github.nebosuke.velocityeditor/META-INF/MANIFEST.MF` — `Bundle-Version`
- `io.github.nebosuke.velocityeditor.feature/feature.xml`
- `io.github.nebosuke.velocityeditor.updatesite/category.xml`
- All four `pom.xml` files

## Development Notes

- **Java 21 required.** On macOS, prefix Maven commands with `JAVA_HOME=$(/usr/libexec/java_home -v 21)` if the default JDK differs.
- **First build is slow.** Tycho resolves dependencies from the Eclipse P2 repository (`https://download.eclipse.org/releases/2025-12`) and caches them locally. A network connection is required.
- **Tests use Mockito with byte-buddy.** The tests module passes `-Dnet.bytebuddy.experimental=true` to surefire; this is required for Java 21 compatibility.
- **Eclipse PDE development.** Import the project into Eclipse and use `Run As > Eclipse Application` to test the plugin interactively without a full Maven build.
- **`.mvn/jvm.config`** disables XML entity size limits to allow Tycho to process large P2 metadata files.
