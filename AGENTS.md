# Quarkus CXF `AGENTS.md`

## Project overview

Quarkus CXF is a set of [Quarkus](https://quarkus.io/) extensions for developing SOAP web services and clients using [Apache CXF](https://cxf.apache.org/). 
It follows the Quarkus extension model (runtime + deployment split) and supports both JVM and GraalVM native compilation.

## Directory structure

```
├── bom/                       # Bill of Materials for dependency management
├── bom-test/                  # Test BOM
├── docs/                      # Antora documentation (AsciiDoc)
├── extensions/                # Quarkus extensions
├── integration-tests/         # Integration test modules (client, server, mTLS, WS-*, etc.)
├── perf-tests/                # Performance benchmarks (Hyperfoil)
├── test-util-parent/          # Shared test utilities
├── .github/                   # GitHub Actions workflows and actions
└── .claude/                   # Claude Code settings and skills
```

## Build the project

Fallback to `mvn` or `./mvnw` if `mvnd` is not installed.

Build all modules without tests

```bash
mvnd clean install -DskipTests -Dquarkus.build.skip
```

_Never_ build with all tests enabled like the following:

```bash
mvnd clean install
```

Always prefer running a specific test class or method covering the feature you work on.

Run specific test(s) in a specific test class(es) (JVM mode)

```bash
mvnd -f path/to/pom.xml clean test -Dtest=MyTest[#myTestMethod][,MyOtherTest[#myOtherTestMethod],...]
```

Run specific native test(s) (requires Docker or Podman)

```bash
mvnd -f path/to/pom.xml clean verify -Pnative -Dquarkus.native.container-build -Dtest=foo -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=MyIT[#myTestMethod][,MyOtherIT[#myOtherTestMethod],...]
```

## Documentation

The sources of Antora documentation site of Quarkus CXF are available in `docs/modules/ROOT` directory.

## Source code of Maven dependencies

1. Check whether `-sources.jar` for the given Maven dependency identified by `groupId`, `artifactId` and `version` was downloaded already:

```bash
grouIdPath="${<groupId>//./\/}"
ls -la ~/.m2/repository/$grouIdPath/<artifactId>/<version>/<artifactId>-<version>-sources.jar
```

2. If the `-sources.jar` file does not exist, download it using

```bash
mvnd dependency:get -Dartifact=<groupId>:<artifactId>:<version>:jar:sources
```

2. Print a single source file directly without extracting it:

```bash
unzip -p ~/.m2/repository/$grouIdPath/<artifactId>/<version>/<artifactId>-<version>-sources.jar "com/foo/bar/ClassName.java"
```

3. To list source files in a `-sources.jar` use:

```bash
unzip -l ~/.m2/repository/$grouIdPath/<artifactId>/<version>/<artifactId>-<version>-sources.jar
```

4. If sources for the given Maven artifact are not available, use this public-API view without sources:

```bash
javap -public -classpath <jar> com.foo.bar.ClassName
```

## AI Agent Rules of Engagement

<!-- Adapted from https://raw.githubusercontent.com/apache/camel/refs/heads/main/AGENTS.md -->

These rules apply to ALL AI agents working on this codebase.

### Attribution

- All AI-generated content (GitHub PR descriptions, review comments, JIRA comments) MUST clearly
  identify itself as AI-generated and mention the human operator.
  Example: "_Claude Code on behalf of [Human Name]_"

### PR Volume

- An agent MUST NOT open more than 10 PRs per day per operator to ensure human reviewers can keep up.
- Prioritize quality over quantity — fewer well-tested PRs are better than many shallow ones.

### Git branch

- An agent MUST NEVER push commits to a branch it did not create.
- If a contributor's PR needs changes, the agent may suggest changes via review comments,
  but must not push to their branch without explicit permission.
- An agent should prefer to use his operator's own fork to push branches instead of the main project repository.
- An agent must provide a useful name for the git branch. It should contain the global topic and issue number if possible.

### GitHub Issue Ownership

- An agent MUST ONLY pick up **Unassigned** GitHub issues.
- If a ticket is already assigned to a human, the agent must not reassign it or work on it.
- Before starting work, the agent must assign the ticket to its operator.

### PR Description Maintenance

When pushing new commits to a PR, **always update the PR description** (and title if needed) to
reflect the current state of the changeset. PRs evolve across commits — the description must stay
accurate and complete. Use `gh pr edit --title "..." --body "..."` after each push.

