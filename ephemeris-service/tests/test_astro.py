"""Unit tests for the pure Vedic-astronomy math.

No Swiss Ephemeris dependency here — these run anywhere, including CI without the
.se1 data files. The end-to-end accuracy regression suite (charts cross-checked
against Drik Panchang) is separate and requires the real ephemeris data.
"""

import math

import pytest

from app import astro


def test_normalize_wraps_into_range():
    assert astro.normalize_longitude(0.0) == 0.0
    assert astro.normalize_longitude(360.0) == 0.0
    assert astro.normalize_longitude(370.0) == pytest.approx(10.0)
    assert astro.normalize_longitude(-10.0) == pytest.approx(350.0)


def test_rashi_at_start_is_mesha():
    r = astro.rashi_of(0.0)
    assert r.number == 1
    assert r.name == "Mesha"
    assert r.degrees_in == pytest.approx(0.0)


def test_rashi_midpoint_of_second_sign():
    r = astro.rashi_of(45.0)  # 30-60 is Vrishabha; 45 is 15 deg in, dead center
    assert r.number == 2
    assert r.name == "Vrishabha"
    assert r.degrees_in == pytest.approx(15.0)
    assert r.degrees_to_boundary == pytest.approx(15.0)


def test_last_rashi_is_meena():
    r = astro.rashi_of(359.0)
    assert r.number == 12
    assert r.name == "Meena"


def test_nakshatra_at_start_is_ashwini():
    n = astro.nakshatra_of(0.0)
    assert n.number == 1
    assert n.name == "Ashwini"


def test_nakshatra_boundary_crossing():
    # Ashwini spans [0, 13.333...); Bharani begins at 13.333...
    span = astro.NAKSHATRA_SPAN
    just_before = astro.nakshatra_of(span - 0.001)
    just_after = astro.nakshatra_of(span + 0.001)
    assert just_before.name == "Ashwini"
    assert just_after.name == "Bharani"
    # The spec's core edge concern: a point ~0.001 deg from the edge reports a
    # tiny degrees_to_boundary, which the backend's policy turns into a warning.
    assert just_before.degrees_to_boundary == pytest.approx(0.001, abs=1e-6)


def test_last_nakshatra_is_revati():
    n = astro.nakshatra_of(359.9)
    assert n.number == 27
    assert n.name == "Revati"


@pytest.mark.parametrize(
    "lon,expected_pada",
    [
        (0.0, 1),                              # very start of Ashwini
        (astro.PADA_SPAN + 0.1, 2),            # into the 2nd pada
        (2 * astro.PADA_SPAN + 0.1, 3),
        (3 * astro.PADA_SPAN + 0.1, 4),
        (astro.NAKSHATRA_SPAN + 0.1, 1),       # first pada of the next nakshatra
    ],
)
def test_pada_boundaries(lon, expected_pada):
    assert astro.pada_of(lon) == expected_pada


def test_degrees_to_boundary_is_symmetric_min():
    # A point 2 deg into a 30-deg Rashi is 2 from the near edge, 28 from the far.
    r = astro.rashi_of(32.0)  # 2 deg into Vrishabha
    assert r.degrees_in == pytest.approx(2.0)
    assert r.degrees_to_boundary == pytest.approx(2.0)


def test_every_rashi_and_nakshatra_reachable():
    # Sanity: sweep the circle and confirm indices stay in range (no off-by-one
    # at segment edges), which protects the boundary math above.
    for step in range(0, 3600):
        lon = step / 10.0
        assert 1 <= astro.rashi_of(lon).number <= 12
        assert 1 <= astro.nakshatra_of(lon).number <= 27
        assert 1 <= astro.pada_of(lon) <= 4
