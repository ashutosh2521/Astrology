"""Ephemeris chart regression: Moon Rashi/Nakshatra/Pada vs Drik Panchang.

This is the ephemeris-side half of the accuracy validation (the koota-side half lives
in the Java backend). It closes the loop the spec asks for: an ayanamsa change or a
pyswisseph upgrade that shifts the Moon's Nakshatra will fail here rather than silently
change match results.

Skips cleanly when the real ephemeris cannot run — no swisseph, or the .se1 files are
missing (the service refuses full precision) — and skips individual cases still pending
Drik Panchang values. So this file is green on a data-less CI box and only does real
work where full-precision ephemeris data is present.
"""

from __future__ import annotations

import json
import os
from pathlib import Path

import pytest

CASES_FILE = Path(__file__).with_name("chart_reference_cases.json")
FILLED = "FILLED"


def _load_cases() -> list[dict]:
    data = json.loads(CASES_FILE.read_text())
    return data["cases"]


def _ephemeris_ready():
    """Return the wired ephemeris modules, or skip if full precision is unavailable."""
    swisseph = pytest.importorskip("swisseph", reason="pyswisseph not installed")
    from app import astro, ephemeris  # imported lazily so collection works without swisseph

    ephe_path = os.environ.get("EPHE_PATH", "./ephe")
    ephemeris.configure(ephe_path)
    try:
        ephemeris.self_check()  # raises PrecisionError if .se1 files are missing
    except ephemeris.PrecisionError as exc:
        pytest.skip(f"Swiss Ephemeris not at full precision: {exc}")
    return astro, ephemeris, swisseph


def _moon_placement(astro, ephemeris, case: dict) -> dict:
    from datetime import datetime

    dt = datetime.fromisoformat(case["utc"].replace("Z", "+00:00"))
    jd = ephemeris.julian_day_ut(
        dt.year, dt.month, dt.day, dt.hour + dt.minute / 60.0 + dt.second / 3600.0
    )
    moon = next(g for g in ephemeris.compute_grahas(jd) if g.name == "Moon")
    return {
        "rashi": astro.rashi_of(moon.longitude).name,
        "nakshatra": astro.nakshatra_of(moon.longitude).name,
        "pada": astro.pada_of(moon.longitude),
    }


@pytest.mark.parametrize("case", _load_cases(), ids=lambda c: c["id"])
def test_chart_matches_reference(case: dict):
    if case.get("status") != FILLED:
        pytest.skip(
            f"PENDING: fill 'expected' for '{case['id']}' from Drik Panchang, then set status FILLED"
        )
    astro, ephemeris, _ = _ephemeris_ready()
    expected = case["expected"]
    assert expected is not None, f"case {case['id']} is FILLED but has no expected block"

    got = _moon_placement(astro, ephemeris, case)
    if "rashi" in expected:
        assert got["rashi"] == expected["rashi"], f"[{case['id']}] Moon Rashi"
    if "nakshatra" in expected:
        assert got["nakshatra"] == expected["nakshatra"], f"[{case['id']}] Moon Nakshatra"
    if "pada" in expected:
        assert got["pada"] == expected["pada"], f"[{case['id']}] Moon Pada"


def test_fixture_is_well_formed():
    """Runs green today: guards that every case has required inputs and a unique id."""
    cases = _load_cases()
    assert len(cases) >= 1
    ids = set()
    for c in cases:
        assert c["id"] not in ids, f"duplicate id {c['id']}"
        ids.add(c["id"])
        assert "utc" in c and "latitude" in c and "longitude" in c
        assert -90.0 <= c["latitude"] <= 90.0
        assert -180.0 <= c["longitude"] <= 180.0
