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
- Eclipse 2024-03 or later
- Java 17 or later

## Installation

### Option 1: Build and Install

```bash
cd jp.co.dreamarts.velocity.editor
mvn clean package
```

Copy `target/jp.co.dreamarts.velocity.editor-1.0.0-SNAPSHOT.jar` to your
Eclipse `dropins` folder, then restart Eclipse.

### Option 2: Install via Eclipse PDE

1. In Eclipse, choose `File > Import > Existing Projects into Workspace`
2. Import this project
3. Right-click the project and choose `Run As > Eclipse Application`

## Usage

1. Open a `.vtl` or `.vm` file
2. The file opens automatically in Velocity Editor
3. Content Assist starts automatically when typing `#`, `$`, or `<`, or manually with `Ctrl+Space`

### Customize `$` Completion Proposals

In `Preferences > Velocity Editor`, edit **Content Assist variables for '$'**
to configure the variable names suggested when typing `$` (one name per line).

## Project Structure

```
jp.co.dreamarts.velocity.editor/
├── META-INF/
│   └── MANIFEST.MF          # OSGi bundle configuration
├── plugin.xml                # Eclipse extension points
├── build.properties
├── pom.xml                   # Maven/Tycho build configuration
└── src/jp/co/dreamarts/velocity/editor/
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

This project is licensed under the [MIT License](../LICENSE).

## Author

DreamArts Corporation
