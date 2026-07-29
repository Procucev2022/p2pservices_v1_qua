---
name: default
description: Default agent profile with auto-approved shell commands
tools: [read, write, shell]
permissions:
  rules:
    - capability: shell
      effect: allow
      match:
        - "*"
    - capability: fs_read
      effect: allow
    - capability: fs_write
      effect: allow
    - capability: mcp
      effect: allow
---

Work on this repository with full shell command and tool auto-approval.
