import contextlib
import io
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import coverage_summary as coverage


class CoverageSummaryTest(unittest.TestCase):
    def test_scope_uses_weighted_source_lines_and_keeps_uncovered_data(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for module in ("api-gateway", "users-service", "prospects-service"):
                report = root / "backend" / module / "target/site/jacoco/jacoco.xml"
                report.parent.mkdir(parents=True)
                report.write_text('<report><counter type="LINE" covered="8" missed="2"/></report>')
            sources = root / "android/app/src/main/java/io/github/g6"
            for file in ("domain/Rule.kt", "data/Store.kt", "ui/Screen.kt", "ui/StateViewModel.kt"):
                path = sources / file
                path.parent.mkdir(parents=True, exist_ok=True)
                path.touch()
            report = root / "android.xml"
            report.write_text('''<report>
              <package name="io/github/g6/domain"><sourcefile name="Rule.kt"><counter type="LINE" covered="1" missed="0"/></sourcefile></package>
              <package name="io/github/g6/data">
                <sourcefile name="Store.kt"><counter type="LINE" covered="0" missed="9"/></sourcefile>
                <sourcefile name="Store_Impl.java"><counter type="LINE" covered="100" missed="0"/></sourcefile>
              </package>
              <package name="io/github/g6/ui">
                <sourcefile name="Screen.kt"><counter type="LINE" covered="90" missed="0"/></sourcefile>
                <sourcefile name="StateViewModel.kt"><counter type="LINE" covered="0" missed="10"/></sourcefile>
              </package>
              <counter type="LINE" covered="191" missed="19"/>
            </report>''')
            with patch.object(coverage, "ROOT", root):
                result = coverage.summarize(report)["reports"]
            self.assertEqual(result["android_logic"], dict(covered=1, total=20, percent=5.0))
            self.assertEqual(result["android_generated"]["covered"], 100)
            self.assertEqual(result["android_rendering"]["covered"], 90)

    def gate(self, covered, total):
        reports = {name: coverage.metric(covered, total) for name in
                   ("api-gateway", "users-service", "prospects-service", "android_logic")}
        with patch.object(coverage, "summarize", return_value=dict(reports=reports)), \
             patch("sys.argv", ["coverage_summary.py"]), \
             contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
            return coverage.main()

    def test_exact_threshold_passes(self):
        self.assertEqual(self.gate(80, 100), 0)

    def test_rounding_to_eighty_does_not_hide_a_failure(self):
        self.assertEqual(coverage.metric(80000, 100001)["percent"], 80.0)
        self.assertEqual(self.gate(80000, 100001), 1)

    def test_empty_coverage_does_not_pass(self):
        self.assertEqual(self.gate(0, 0), 1)

    def test_missing_report_exits_with_error(self):
        with patch.object(coverage, "summarize", side_effect=FileNotFoundError("missing.xml")), \
             patch("sys.argv", ["coverage_summary.py"]), contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit) as caught:
                coverage.main()
        self.assertEqual(caught.exception.code, 2)


if __name__ == "__main__":
    unittest.main()
