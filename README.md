# Velocity Template Editor for Eclipse

An Eclipse editor plugin for Velocity Template Language (VTL).

## Features

### Syntax Highlighting
- VTL directives (`#if`, `#foreach`, `#set`, `#macro`, etc.)
- VTL variables (`$variable`, `${variable}`, `$!variable`)
- VTL comments (`##` single-line, `#* *#` multi-line)
- HTML tags, attributes, and attribute values

### Content Assist
- Show VTL directive proposals with `#`
- Show VTL variable templates with `$`
- Show HTML tag proposals with `<`
- Manual trigger with `Ctrl+Space`
- Customize variable proposal names for `$` in Preferences (one variable name per line)

### Double-Click Selection
- Select the entire VTL variable
- Select HTML tag names
- Select regular words

### Light/Dark Theme Support
- Colors are automatically adjusted based on the Eclipse theme

## Supported Files
- `.vtl` - Velocity Template
- `.vm` - Velocity Macro

## Requirements
- Eclipse 2025-12 or later
- Java 21 or later
- Maven build must run with Java 21 or later

## Installation

### Option 1: Install from Update Site (recommended)

Install from `Help > Install New Software...` with this update site URL:

`https://nebosuke.github.io/eclipse-velocity-editor/`

### Option 2: Build and Install

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -DskipTests clean verify
```

The generated Update Site is:

`io.github.nebosuke.velocityeditor.updatesite/target/repository`

You can add this directory in `Help > Install New Software... > Add... > Local...`.

### Option 3: Install via Eclipse PDE

1. In Eclipse, choose `File > Import > Existing Projects into Workspace`
2. Import this project
3. Right-click the project and choose `Run As > Eclipse Application`

## Automated Update Site Publishing

Every push to `main` runs `.github/workflows/publish.yml`, which:
1. Increments the patch version across all project files (`scripts/bump_version.py`) and pushes that bump back to `main`.
2. Builds the update site with Maven/Tycho.
3. Replaces the contents of the `gh-pages` branch with the newly built repository and pushes it.

No manual steps are needed for a normal release. Use the manual steps below only if you need to publish outside of CI (e.g. to test a build locally or recover from a broken workflow run).

## Manual Update Site Publishing

1. Increment version numbers before publishing.

Update all of these files to the next version (example: `1.0.1` -> `1.0.2`):
- `io.github.nebosuke.velocityeditor/META-INF/MANIFEST.MF`
`Bundle-Version: 1.0.2.qualifier`
- `io.github.nebosuke.velocityeditor.feature/feature.xml`
`version="1.0.2.qualifier"`
- `io.github.nebosuke.velocityeditor.updatesite/category.xml`
`version="1.0.2.qualifier"` and feature URL `...feature_1.0.2.qualifier.jar`
- `pom.xml`
`<version>1.0.2-SNAPSHOT</version>`
- `io.github.nebosuke.velocityeditor/pom.xml`
`<version>1.0.2-SNAPSHOT</version>` (in `<parent>`)
- `io.github.nebosuke.velocityeditor.feature/pom.xml`
`<version>1.0.2-SNAPSHOT</version>` (in `<parent>`)
- `io.github.nebosuke.velocityeditor.updatesite/pom.xml`
`<version>1.0.2-SNAPSHOT</version>` (in `<parent>`)

2. Build the update site with Java 17+:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -DskipTests clean verify
```

3. Copy generated repository files to a temporary directory:

```bash
rm -rf /tmp/eclipse-velocity-editor-site
mkdir -p /tmp/eclipse-velocity-editor-site
cp -R io.github.nebosuke.velocityeditor.updatesite/target/repository/. /tmp/eclipse-velocity-editor-site/
```

4. Switch to `gh-pages` branch and replace contents:

```bash
git checkout gh-pages
find . -mindepth 1 -maxdepth 1 ! -name ".git" -exec rm -rf {} +
cp -R /tmp/eclipse-velocity-editor-site/. .
touch .nojekyll
```

5. Commit and push:

```bash
git add -A
git commit -m "Publish update site"
git push origin gh-pages
```

6. Return to development branch:

```bash
git checkout main
```

## Usage

1. Open a `.vtl` or `.vm` file
2. The file opens automatically in Velocity Editor
3. Content Assist starts automatically when typing `#`, `$`, or `<`, or manually with `Ctrl+Space`

### Customize `$` Completion Proposals

In `Preferences > Velocity Editor`, edit **Content Assist variables for '$'**
to configure the variable names suggested when typing `$` (one name per line).

## Project Structure

```
io.github.nebosuke.velocityeditor/
├── META-INF/
│   └── MANIFEST.MF          # OSGi bundle configuration
├── plugin.xml                # Eclipse extension points
├── build.properties
├── pom.xml                   # Maven/Tycho build configuration
└── src/io/github/nebosuke/velocityeditor/
    ├── Activator.java
    ├── editors/
    │   ├── VelocityEditor.java
    │   ├── VelocityConfiguration.java
    │   ├── VelocityDocumentProvider.java
    │   └── VelocityDoubleClickStrategy.java
    ├── scanners/
    │   ├── VelocityPartitionScanner.java
    │   ├── VelocityCodeScanner.java
    │   ├── VelocityCommentScanner.java
    │   ├── VelocityVariableRule.java
    │   ├── HTMLTagScanner.java
    │   └── HTMLTagRule.java
    ├── assist/
    │   └── VelocityContentAssistProcessor.java
    └── preferences/
        ├── ColorManager.java
        └── VelocityPreferencePage.java
```

## License

This project is licensed under the [MIT License](LICENSE).

## Author

Yatsuke Works
