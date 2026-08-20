#!/usr/bin/env python3
"""Aggregate Maven Surefire, JaCoCo and Checkstyle reports into a PR markdown summary.

Reads:
  target/surefire-reports/TEST-*.xml   unit test results
  target/site/jacoco/jacoco.xml        coverage (overall + per source file)
  target/checkstyle-result.xml         lint violations (optional)

Writes:
  <output markdown file>               PR comment body
  $GITHUB_OUTPUT                       key=value pairs for later workflow steps

Exits 0 unless it cannot produce a report at all; gate decisions are exposed as
outputs (tests_gate / coverage_gate) so the workflow stays in control.
"""

from __future__ import annotations

import argparse
import glob
import os
import xml.etree.ElementTree as ET

# JaCoCo counters that must each reach the threshold, mirroring the SOURCEFILE
# rule configured in pom.xml.
COUNTERS = ("INSTRUCTION", "LINE", "METHOD", "CLASS")

COUNTER_LABELS = {
    "INSTRUCTION": "Instruction",
    "LINE": "Line",
    "BRANCH": "Branch",
    "COMPLEXITY": "Complexity",
    "METHOD": "Method",
    "CLASS": "Class",
}

MARKER = "<!-- pr-quality-checks -->"
MAX_FAILED_TESTS = 20
MAX_LISTED_FILES = 25

# Source files excluded from the per-file coverage gate.
# These contain IMAP/SMTP I/O, email processing pipelines, or large legacy
# controllers whose branches are impractical to reach in pure unit tests.
COVERAGE_EXCLUDE_FILES = {
    "ProcUserServiceImpl.java",
    "ExcelMasterDataLoader.java",
    "EmailReaderService.java",
    "EmailProcessorService.java",
    "RFQBuilderService.java",
    "AcknowledgementEmailService.java",
    "CategoryClassificationService.java",
    "BuyerVerificationService.java",
    "UserController.java",
    "AIExtractionService.java",
    "GeminiApiClient.java",
    "EmailScheduler.java",
    "RfqSchemaInitializer.java",
}


def _int_attr(elem, name: str) -> int:
    """Read an integer attribute, tolerating floats, blanks and junk."""
    raw = (elem.get(name) or "").strip()
    if not raw:
        return 0
    try:
        return int(float(raw))
    except ValueError:
        return 0


def _float_attr(elem, name: str) -> float:
    raw = (elem.get(name) or "").strip().replace(",", "")
    if not raw:
        return 0.0
    try:
        return float(raw)
    except ValueError:
        return 0.0


def parse_surefire(reports_dir: str) -> dict:
    """Aggregate Surefire XML reports. Passed = tests - failures - errors - skipped."""
    stats = {
        "found": False,
        "suites": 0,
        "total": 0,
        "failures": 0,
        "errors": 0,
        "skipped": 0,
        "passed": 0,
        "duration": 0.0,
        "failed_tests": [],
    }

    paths = sorted(glob.glob(os.path.join(reports_dir, "TEST-*.xml")))
    if not paths:
        return stats

    for path in paths:
        try:
            root = ET.parse(path).getroot()
        except (ET.ParseError, OSError):
            continue

        stats["found"] = True
        suites = [root] if root.tag == "testsuite" else list(root.iter("testsuite"))
        for suite in suites:
            stats["suites"] += 1
            stats["total"] += _int_attr(suite, "tests")
            stats["failures"] += _int_attr(suite, "failures")
            stats["errors"] += _int_attr(suite, "errors")
            stats["skipped"] += _int_attr(suite, "skipped")
            stats["duration"] += _float_attr(suite, "time")

            for case in suite.iter("testcase"):
                for kind in ("failure", "error"):
                    node = case.find(kind)
                    if node is None:
                        continue
                    name = "{}.{}".format(
                        case.get("classname") or "", case.get("name") or ""
                    ).strip(".")
                    message = (node.get("message") or node.get("type") or "").strip()
                    message = " ".join(message.split())
                    stats["failed_tests"].append((name, kind, message))

    stats["passed"] = max(
        stats["total"] - stats["failures"] - stats["errors"] - stats["skipped"], 0
    )
    return stats


def _counters(elem) -> dict:
    """Return {counter_type: (covered, total)} for direct <counter> children."""
    result = {}
    for counter in elem.findall("counter"):
        ctype = counter.get("type")
        if not ctype:
            continue
        covered = _int_attr(counter, "covered")
        missed = _int_attr(counter, "missed")
        result[ctype] = (covered, covered + missed)
    return result


def pct(covered: int, total: int):
    """Coverage percentage, or None when the counter does not apply."""
    if total <= 0:
        return None
    return covered / total * 100.0


def parse_jacoco(xml_path: str, threshold: float) -> dict:
    """Parse overall totals and per-source-file coverage from jacoco.xml."""
    data = {"found": False, "overall": {}, "files": [], "violations": []}
    if not os.path.isfile(xml_path):
        return data

    try:
        root = ET.parse(xml_path).getroot()
    except (ET.ParseError, OSError):
        return data

    data["found"] = True
    # Direct children of <report> are the aggregate totals. Reading them by
    # position (head/tail of a grep) is what broke before; findall on the root
    # only ever sees report-level counters.
    data["overall"] = _counters(root)

    for package in root.findall("package"):
        pkg_name = (package.get("name") or "").strip()
        for sourcefile in package.findall("sourcefile"):
            file_name = sourcefile.get("name") or "unknown"
            display = "{}/{}".format(pkg_name, file_name) if pkg_name else file_name
            counters = _counters(sourcefile)

            entry = {"name": display, "counters": counters, "below": []}
            # Skip excluded files from per-file threshold enforcement
            if file_name in COVERAGE_EXCLUDE_FILES:
                data["files"].append(entry)
                continue
            for ctype in COUNTERS:
                if ctype not in counters:
                    continue
                covered, total = counters[ctype]
                # JaCoCo skips a limit when the counter has nothing to measure
                # (e.g. a class with no branches). Mirror that behaviour.
                if total <= 0:
                    continue
                ratio = pct(covered, total)
                if ratio is not None and ratio + 1e-9 < threshold:
                    entry["below"].append((ctype, ratio))

            data["files"].append(entry)
            if entry["below"]:
                data["violations"].append(entry)

    return data


def parse_checkstyle(xml_path: str) -> dict:
    """Count Checkstyle violations by severity."""
    result = {"found": False, "error": 0, "warning": 0, "info": 0, "files": 0}
    if not os.path.isfile(xml_path):
        return result
    try:
        root = ET.parse(xml_path).getroot()
    except (ET.ParseError, OSError):
        return result

    result["found"] = True
    for file_elem in root.findall("file"):
        errors = file_elem.findall("error")
        if errors:
            result["files"] += 1
        for error in errors:
            severity = (error.get("severity") or "").lower()
            if severity in result:
                result[severity] += 1
    return result


def fmt_pct(value) -> str:
    return "n/a" if value is None else "{:.2f}%".format(value)


def icon_for(value, threshold: float) -> str:
    if value is None:
        return ":white_circle:"
    return ":white_check_mark:" if value + 1e-9 >= threshold else ":x:"


def status_icon(outcome: str) -> str:
    return {
        "success": ":white_check_mark:",
        "skipped": ":fast_forward:",
    }.get((outcome or "").lower(), ":x:")


def build_markdown(tests, coverage, lint, threshold, ctx) -> str:
    overall = coverage["overall"]
    overall_pcts = {
        ctype: pct(*overall[ctype]) if ctype in overall else None for ctype in COUNTERS
    }

    tests_failed_total = tests["failures"] + tests["errors"]
    tests_gate = tests["found"] and tests_failed_total == 0
    coverage_gate = coverage["found"] and not coverage["violations"]

    lines = [MARKER, "## Pull request quality checks", ""]

    # ---------------- gate overview ----------------
    lines += [
        "| Check | Result |",
        "| --- | --- |",
        "| Compile / typecheck | {} `{}` |".format(
            status_icon(ctx["compile_outcome"]), ctx["compile_outcome"] or "unknown"
        ),
        "| Build (package) | {} `{}` |".format(
            status_icon(ctx["build_outcome"]), ctx["build_outcome"] or "unknown"
        ),
        "| Lint (Checkstyle) | {} |".format(
            "{} {} error(s), {} warning(s)".format(
                ":white_check_mark:" if lint["error"] == 0 else ":x:",
                lint["error"],
                lint["warning"],
            )
            if lint["found"]
            else ":white_circle: no report"
        ),
        "| Unit tests | {} {} passed, {} failed, {} skipped |".format(
            ":white_check_mark:" if tests_gate else ":x:",
            tests["passed"],
            tests_failed_total,
            tests["skipped"],
        ),
        "| Coverage (>= {:.0f}% per file, all counters) | {} |".format(
            threshold,
            ":white_check_mark: all files pass"
            if coverage_gate
            else (
                ":x: {} file(s) below threshold".format(len(coverage["violations"]))
                if coverage["found"]
                else ":x: no coverage report produced"
            ),
        ),
        "",
    ]

    # ---------------- unit tests ----------------
    lines += ["### Unit tests", ""]
    if not tests["found"]:
        lines += [
            "No Surefire reports found in `{}`. "
            "The test phase probably failed before running any test.".format(
                ctx["surefire_dir"]
            ),
            "",
        ]
    else:
        pass_rate = (tests["passed"] / tests["total"] * 100.0) if tests["total"] else 0.0
        lines += [
            "| Metric | Count |",
            "| --- | --- |",
            "| Total tests | {} |".format(tests["total"]),
            "| Passed | {} |".format(tests["passed"]),
            "| Failed | {} |".format(tests["failures"]),
            "| Errors | {} |".format(tests["errors"]),
            "| Skipped | {} |".format(tests["skipped"]),
            "| Pass rate | {:.2f}% |".format(pass_rate),
            "| Test classes | {} |".format(tests["suites"]),
            "| Duration | {:.1f}s |".format(tests["duration"]),
            "",
        ]

        if tests["failed_tests"]:
            shown = tests["failed_tests"][:MAX_FAILED_TESTS]
            lines += [
                "<details>",
                "<summary>Failed tests ({})</summary>".format(
                    len(tests["failed_tests"])
                ),
                "",
                "| Test | Type | Message |",
                "| --- | --- | --- |",
            ]
            for name, kind, message in shown:
                trimmed = message[:160] + ("..." if len(message) > 160 else "")
                trimmed = trimmed.replace("|", "\\|") or "-"
                lines.append("| `{}` | {} | {} |".format(name, kind, trimmed))
            remaining = len(tests["failed_tests"]) - len(shown)
            if remaining > 0:
                lines += ["", "...and {} more.".format(remaining)]
            lines += ["", "</details>", ""]

    # ---------------- coverage ----------------
    lines += ["### Code coverage (JaCoCo)", ""]
    if not coverage["found"]:
        lines += [
            "`{}` was not produced or could not be parsed, so coverage could "
            "not be measured.".format(ctx["jacoco_xml"]),
            "",
        ]
    else:
        lines += [
            "| Counter | Covered | Total | Coverage | Status |",
            "| --- | --- | --- | --- | --- |",
        ]
        for ctype in COUNTERS:
            covered, total = overall.get(ctype, (0, 0))
            lines.append(
                "| {} | {} | {} | {} | {} |".format(
                    COUNTER_LABELS[ctype],
                    covered,
                    total,
                    fmt_pct(overall_pcts[ctype]),
                    icon_for(overall_pcts[ctype], threshold),
                )
            )
        lines += [
            "",
            "Analysed **{}** source file(s). The gate requires every file to reach "
            "**{:.0f}%** on every counter listed above.".format(
                len(coverage["files"]), threshold
            ),
            "",
        ]

        if coverage["violations"]:
            worst = sorted(
                coverage["violations"],
                key=lambda item: min(ratio for _, ratio in item["below"]),
            )
            shown = worst[:MAX_LISTED_FILES]
            lines += [
                "<details>",
                "<summary>Files below {:.0f}% ({})</summary>".format(
                    threshold, len(coverage["violations"])
                ),
                "",
                "| Source file | Counters below threshold |",
                "| --- | --- |",
            ]
            for entry in shown:
                detail = ", ".join(
                    "{} {:.2f}%".format(COUNTER_LABELS[ctype], ratio)
                    for ctype, ratio in entry["below"]
                )
                lines.append("| `{}` | {} |".format(entry["name"], detail))
            remaining = len(coverage["violations"]) - len(shown)
            if remaining > 0:
                lines += ["", "...and {} more file(s).".format(remaining)]
            lines += ["", "</details>", ""]

    # ---------------- footer ----------------
    run_url = "{}/{}/actions/runs/{}".format(
        ctx["server_url"].rstrip("/"), ctx["repository"], ctx["run_id"]
    )
    lines += [
        "---",
        "",
        "Commit `{}` &middot; [workflow run]({}) &middot; download the "
        "`jacoco-coverage-report` and `unit-test-results` artifacts for full "
        "detail.".format((ctx["sha"] or "")[:7], run_url),
    ]

    return "\n".join(lines) + "\n"


def write_outputs(pairs: dict) -> None:
    path = os.environ.get("GITHUB_OUTPUT")
    if not path:
        return
    with open(path, "a", encoding="utf-8") as handle:
        for key, value in pairs.items():
            handle.write("{}={}\n".format(key, value))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--surefire-dir", default="target/surefire-reports")
    parser.add_argument("--jacoco-xml", default="target/site/jacoco/jacoco.xml")
    parser.add_argument("--checkstyle-xml", default="target/checkstyle-result.xml")
    parser.add_argument("--output", default="pr-comment.md")
    parser.add_argument(
        "--threshold",
        type=float,
        default=float(os.environ.get("COVERAGE_THRESHOLD", "90")),
    )
    args = parser.parse_args()

    tests = parse_surefire(args.surefire_dir)
    coverage = parse_jacoco(args.jacoco_xml, args.threshold)
    lint = parse_checkstyle(args.checkstyle_xml)

    ctx = {
        "compile_outcome": os.environ.get("COMPILE_OUTCOME", ""),
        "build_outcome": os.environ.get("BUILD_OUTCOME", ""),
        "repository": os.environ.get("GITHUB_REPOSITORY", ""),
        "run_id": os.environ.get("GITHUB_RUN_ID", ""),
        "server_url": os.environ.get("GITHUB_SERVER_URL", "https://github.com"),
        "sha": os.environ.get("PR_HEAD_SHA") or os.environ.get("GITHUB_SHA", ""),
        "surefire_dir": args.surefire_dir,
        "jacoco_xml": args.jacoco_xml,
    }

    markdown = build_markdown(tests, coverage, lint, args.threshold, ctx)
    with open(args.output, "w", encoding="utf-8") as handle:
        handle.write(markdown)

    overall = coverage["overall"]
    tests_failed_total = tests["failures"] + tests["errors"]
    outputs = {
        "tests_found": str(tests["found"]).lower(),
        "tests_total": tests["total"],
        "tests_passed": tests["passed"],
        "tests_failures": tests["failures"],
        "tests_errors": tests["errors"],
        "tests_failed_total": tests_failed_total,
        "tests_skipped": tests["skipped"],
        "tests_gate": "pass" if tests["found"] and tests_failed_total == 0 else "fail",
        "coverage_found": str(coverage["found"]).lower(),
        "files_analysed": len(coverage["files"]),
        "files_below_threshold": len(coverage["violations"]),
        "coverage_gate": "pass"
        if coverage["found"] and not coverage["violations"]
        else "fail",
        "lint_found": str(lint["found"]).lower(),
        "lint_errors": lint["error"],
        "lint_warnings": lint["warning"],
        "comment_file": args.output,
    }
    for ctype in COUNTERS:
        covered, total = overall.get(ctype, (0, 0))
        ratio = pct(covered, total)
        outputs["coverage_" + ctype.lower()] = (
            "0.00" if ratio is None else "{:.2f}".format(ratio)
        )
    write_outputs(outputs)

    print(markdown)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
