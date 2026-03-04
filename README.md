# FlowTree

A thin wrapper for the [FlowTree library](https://github.com/almostrealism/common), which is now part of [Almost Realism Common](https://github.com/almostrealism/common). This repository provides packaging, configuration, and utility scripts for running FlowTree services.

## Tools

The `bin/` directory contains the following scripts:

### server.sh

Launches the FlowTree server using the shaded JAR.

```bash
bin/server.sh
```

### slack.sh

Starts the Slack bot controller with the workstreams configuration.

```bash
bin/slack.sh
```

### claude-tracker

A wrapper for [Claude Code](https://claude.ai/claude-code) that records session agent time and API time into a local SQLite database (`~/.claude/usage.db`). Use this in place of the `claude` command.

```bash
bin/claude-tracker              # interactive session
bin/claude-tracker -p "prompt"  # pass arguments through
```

The database location can be overridden with the `CLAUDE_USAGE_DB` environment variable.

### claude-usage

Queries the SQLite database populated by `claude-tracker` and displays a per-day breakdown of agent hours, API hours, and total hours.

```bash
bin/claude-usage      # last 30 days
bin/claude-usage 7    # last 7 days
```
