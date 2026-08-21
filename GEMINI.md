# Quality Check Configuration & Assistant Instructions

This document defines the mandatory Quality Check protocol for AI coding agents operating on this workspace (including Antigravity, Kiro, GitHub Copilot, Claude, Codex, Cursor, and others).

---

## Mandatory Post-Change Quality Check Protocol

After performing code modifications (such as implementing features, bug fixes, refactoring, or updating configurations), AI coding agents **MUST** execute the appropriate quality check commands before declaring a task completed.

---

## 1. Unit Test Coverage & Global Timeout Requirements

1. **Per-File 90% Code Coverage Benchmark (MANDATORY)**:
   - Every single source file in the application must maintain at least **90% unit test code coverage** across all parameters:
     - **Lines** (`LINE`) >= 90%
     - **Statements / Instructions** (`INSTRUCTION`) >= 90%
     - **Branches** (`BRANCH`) >= 90%
     - **Functions / Methods** (`METHOD`) >= 90%
     - **Complexity** (`COMPLEXITY`) >= 90%
     - **Classes** (`CLASS`) >= 90%
   - No source file may be skipped or excluded from coverage checks.
   - If any file falls below 90% coverage on any parameter, the build MUST fail (`<haltOnFailure>true</haltOnFailure>`).

2. **Global Unit Test Timeout**:
   - All unit tests must be configured with a global timeout (default 30 seconds per test) via `junit.jupiter.execution.timeout.default` or Surefire configuration to prevent hanging executions.

3. **Mandatory Post-Change Verification**:
   - Whenever ANY change is made to the codebase, AI coding agents MUST execute the build & unit test coverage verification suite (`mvn clean test jacoco:report`) and verify per-file coverage compliance before completing the task.

---

## 2. Execution Priority Order

Quality checks **MUST** be executed in the following order:

### Priority 1: Build Verification & Unit Test Code Coverage (REQUIRED FIRST)
1. **Compilation / Build Check**: Verify that the code compiles without errors or warnings.
2. **Unit Test Suite & Code Coverage**: Run unit tests (`mvn clean test jacoco:report`) and verify that every single file reaches >= 90% unit test code coverage across all metrics (lines, statements, branches, methods, complexity, class).

### Priority 2: Typechecking & Static Analysis
1. **Type Safety**: Execute strict typechecking (e.g. `javac`, `tsc --noEmit`, `mypy`, `go vet`).
2. **Static Bug Detection**: Run static analysis tools (e.g. `SpotBugs`, `PMD`, `Clippy`).

### Priority 3: Linting & Code Formatting
1. Validate code style and linting rules (e.g. `Checkstyle`, `ESLint`, `Flake8`, `Prettier`).

### Priority 4: Pending Database Schema Migrations
1. Check for and apply any pending database schema migrations (e.g. `Flyway`, `Liquibase`, `Prisma`, `Alembic`, `Rails`).

---

## 3. Multi-Project & Global Workspace Scope

When working in a workspace with multiple projects, submodules, or microservices:
- AI agents **MUST NOT** only run checks in a single sub-directory if root or global commands exist.
- Use **global workspace commands** from the root to run build, test coverage, typecheck, and lint across **ALL** projects in the workspace.

---

## 4. Command Matrix by Technology Stack

### Java / Maven (Current Workspace Standard)
- **Fast Incremental Check (Dirty / Modified Files Only)**:
  ```powershell
  # Fast compilation check
  mvn test-compile
  
  # Fast test execution for modified test files only
  mvn test-compile -Dtest=EmailProcessorServiceTest,RFQBuilderServiceTest
  ```
- **Step 1: Build & Unit Test Coverage Verification (FIRST & MANDATORY)**:
  ```powershell
  mvn clean test jacoco:report
  ```
- **Step 2 & 3: Typecheck, Static Analysis & Linting**:
  ```powershell
  mvn checkstyle:check
  ```
- **Step 4: Database Schema Migrations**:
  ```powershell
  mvn flyway:migrate
  ```

---

## 5. Fast Check Mode for Iterative Development

During rapid iterative development (before running the complete workspace validation suite):
- Run the **Fast Incremental Check** command targeting modified files to quickly catch compilation errors or broken tests.
- **Rule**: Prior to declaring full task completion to the user, the full **Step 1 (Build & Unit Test Coverage)** through **Step 4** check commands MUST be executed, verifying 90% per-file coverage across all parameters.

