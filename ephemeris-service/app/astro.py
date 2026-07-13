"""Pure Vedic-astronomy math over a sidereal longitude.

Deliberately free of any Swiss Ephemeris dependency so every function here is
unit-testable without ephemeris data files. The service's ephemeris wrapper
produces sidereal longitudes; this module turns a longitude into Rashi,
Nakshatra, Pada and the raw boundary distances.

Boundary *classification* (is this "close enough to a transition to warn?") is
intentionally NOT done here — that threshold is a business-policy decision owned
by the Java backend. This module only reports the raw distances.
"""

from __future__ import annotations

from dataclasses import dataclass

# 12 Rashis (zodiac signs), index 0 = Mesha, starting at sidereal longitude 0.
RASHIS = [
    "Mesha", "Vrishabha", "Mithuna", "Karka", "Simha", "Kanya",
    "Tula", "Vrishchika", "Dhanu", "Makara", "Kumbha", "Meena",
]

# 27 Nakshatras, index 0 = Ashwini, starting at sidereal longitude 0.
NAKSHATRAS = [
    "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra",
    "Punarvasu", "Pushya", "Ashlesha", "Magha", "Purva Phalguni",
    "Uttara Phalguni", "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha",
    "Jyeshtha", "Mula", "Purva Ashadha", "Uttara Ashadha", "Shravana",
    "Dhanishta", "Shatabhisha", "Purva Bhadrapada", "Uttara Bhadrapada",
    "Revati",
]

DEGREES_IN_CIRCLE = 360.0
RASHI_SPAN = DEGREES_IN_CIRCLE / 12          # 30°
NAKSHATRA_SPAN = DEGREES_IN_CIRCLE / 27      # 13°20' = 13.3333…°
PADA_SPAN = NAKSHATRA_SPAN / 4               # 3°20' = 3.3333…°


def normalize_longitude(lon: float) -> float:
    """Wrap a longitude into [0, 360)."""
    return lon % DEGREES_IN_CIRCLE


@dataclass(frozen=True)
class PlacementIn:
    """Position of a longitude within one segment type (Rashi or Nakshatra).

    ``index`` is 0-based; ``number`` is the conventional 1-based ordinal.
    ``degrees_in`` is how far into the current segment the point sits.
    ``degrees_to_boundary`` is the distance to the *nearest* segment edge
    (entering or leaving) — the quantity the boundary-warning policy keys off.
    """

    index: int
    number: int
    name: str
    degrees_in: float
    degrees_to_boundary: float


def _placement(lon: float, span: float, names: list[str]) -> PlacementIn:
    lon = normalize_longitude(lon)
    index = int(lon // span)
    # Guard the exact-360 edge that % should already prevent, defensively.
    if index >= len(names):
        index = len(names) - 1
    degrees_in = lon - index * span
    degrees_to_boundary = min(degrees_in, span - degrees_in)
    return PlacementIn(
        index=index,
        number=index + 1,
        name=names[index],
        degrees_in=degrees_in,
        degrees_to_boundary=degrees_to_boundary,
    )


def rashi_of(lon: float) -> PlacementIn:
    """Rashi (sign) placement of a sidereal longitude."""
    return _placement(lon, RASHI_SPAN, RASHIS)


def nakshatra_of(lon: float) -> PlacementIn:
    """Nakshatra placement of a sidereal longitude."""
    return _placement(lon, NAKSHATRA_SPAN, NAKSHATRAS)


def pada_of(lon: float) -> int:
    """Pada (1-4) within the current Nakshatra."""
    lon = normalize_longitude(lon)
    into_nakshatra = lon - (int(lon // NAKSHATRA_SPAN) * NAKSHATRA_SPAN)
    return int(into_nakshatra // PADA_SPAN) + 1
