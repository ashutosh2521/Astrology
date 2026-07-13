"""Swiss Ephemeris wrapper.

The single most important accuracy guarantee in the whole app lives here: if the
real ``.se1`` ephemeris data files are missing, Swiss Ephemeris silently falls
back to the lower-precision Moshier analytical model. The spec requires that this
be *caught, not silently accepted*.

Detection mechanism: every ``swe.calc_ut`` call returns the flags describing the
ephemeris that was *actually* used. We always request ``FLG_SWIEPH`` (the file-based
full-precision ephemeris). If the return flags come back with ``FLG_MOSEPH`` set,
the files were not found and Moshier was substituted — which we treat as an error.

Ayanamsa is set to Lahiri explicitly (never a hidden default), per the spec.
"""

from __future__ import annotations

import threading
from dataclasses import dataclass

import swisseph as swe

# Sidereal, file-based ephemeris. We never fall back to Moshier silently.
_CALC_FLAGS = swe.FLG_SWIEPH | swe.FLG_SIDEREAL | swe.FLG_SPEED

# Grahas we compute. v1 scoring only consumes the Moon, but the rest is nearly
# free in the same call and lets Lagna/Mangal-dosha features arrive later without
# a contract change or a recompute of stored charts.
GRAHAS: dict[str, int] = {
    "Sun": swe.SUN,
    "Moon": swe.MOON,
    "Mars": swe.MARS,
    "Mercury": swe.MERCURY,
    "Jupiter": swe.JUPITER,
    "Venus": swe.VENUS,
    "Saturn": swe.SATURN,
    "Rahu": swe.MEAN_NODE,   # Ketu is derived as Rahu + 180°.
}


class PrecisionError(RuntimeError):
    """Raised when Swiss Ephemeris served a Moshier-fallback result.

    Signals that the ``.se1`` files are missing or unreadable and the result is
    NOT full precision — must never be silently returned to callers.
    """


@dataclass(frozen=True)
class GrahaPosition:
    name: str
    longitude: float          # sidereal ecliptic longitude, degrees [0, 360)
    speed: float              # degrees/day; negative = retrograde


# Swiss Ephemeris state (ephe path, sid mode) is thread-local in common pyswisseph
# builds: configuration done in one thread is INVISIBLE to others. A calculation on
# an unconfigured thread silently falls back to Moshier with the default (non-Lahiri)
# ayanamsa — measured ~0.9 deg of silent error, enough to flip a Nakshatra. Every
# public function here re-asserts configuration on ITS OWN thread before computing.
_configured_path: str | None = None
_thread_state = threading.local()


def configure(ephe_path: str) -> None:
    """Record the data-file path and configure the calling thread.

    Call once at startup; per-thread (re)configuration then happens automatically
    inside every calculation via ``_ensure_thread_configured``.
    """
    global _configured_path
    _configured_path = ephe_path
    _ensure_thread_configured()


def _ensure_thread_configured() -> None:
    """Apply ephe path + Lahiri sid mode on the current thread, once per thread."""
    if getattr(_thread_state, "ready", False):
        return
    if _configured_path is None:
        raise PrecisionError("ephemeris not configured: call configure() at startup")
    swe.set_ephe_path(_configured_path)
    swe.set_sid_mode(swe.SIDM_LAHIRI, 0, 0)
    _thread_state.ready = True


def _calc(jd_ut: float, body: int) -> tuple[float, float]:
    """Return (sidereal longitude, speed) for one body, asserting full precision."""
    _ensure_thread_configured()
    values, ret_flags = swe.calc_ut(jd_ut, body, _CALC_FLAGS)
    if ret_flags < 0:
        raise PrecisionError(f"swe.calc_ut failed for body {body} (flags={ret_flags})")
    if ret_flags & swe.FLG_MOSEPH:
        raise PrecisionError(
            f"Moshier fallback detected for body {body}: .se1 ephemeris files "
            "missing or unreadable. Refusing to return low-precision result."
        )
    return values[0] % 360.0, values[3]


def julian_day_ut(jd_year: int, jd_month: int, jd_day: int, ut_hours: float) -> float:
    """Julian Day (UT) from a Gregorian calendar date and fractional UT hours."""
    return swe.julday(jd_year, jd_month, jd_day, ut_hours, swe.GREG_CAL)


def compute_grahas(jd_ut: float) -> list[GrahaPosition]:
    """Sidereal positions of all grahas (plus derived Ketu) at a Julian Day (UT)."""
    positions: list[GrahaPosition] = []
    rahu_lon: float | None = None
    for name, body in GRAHAS.items():
        lon, speed = _calc(jd_ut, body)
        positions.append(GrahaPosition(name=name, longitude=lon, speed=speed))
        if name == "Rahu":
            rahu_lon = lon
    if rahu_lon is not None:
        positions.append(
            GrahaPosition(name="Ketu", longitude=(rahu_lon + 180.0) % 360.0, speed=0.0)
        )
    return positions


def compute_ascendant(jd_ut: float, lat: float, lon: float) -> float:
    """Sidereal Ascendant (Lagna) longitude in degrees [0, 360).

    Uses whole-sign-friendly Placidus cusps; only the Ascendant point is consumed.
    """
    _ensure_thread_configured()
    _cusps, ascmc = swe.houses_ex(jd_ut, lat, lon, b"P", swe.FLG_SIDEREAL)
    return ascmc[0] % 360.0


def self_check() -> None:
    """Startup probe: compute the Moon at a fixed epoch and assert full precision.

    Raises ``PrecisionError`` if the service is running in Moshier fallback, so
    the process can refuse to start rather than serve wrong charts. J2000.0.
    """
    jd_j2000 = 2451545.0
    _calc(jd_j2000, swe.MOON)
