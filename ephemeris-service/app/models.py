"""API request/response schemas for the ephemeris service.

Contract notes:
- The request carries a *UTC instant*. Local-time-and-timezone resolution
  (including pre-1950 India historical offsets) is the Java backend's job; this
  service knows nothing about timezones.
- Ayanamsa is required on every request, never defaulted server-side, so a future
  change to it is always a visible contract change.
- The Moon block reports raw ``degrees_to_boundary``; the decision to *warn* about
  a near-boundary chart is a policy threshold applied by the backend, not here.
"""

from __future__ import annotations

from datetime import datetime
from enum import Enum

from pydantic import BaseModel, Field


class Ayanamsa(str, Enum):
    LAHIRI = "LAHIRI"


class ChartRequest(BaseModel):
    utc: datetime = Field(..., description="Birth instant in UTC (ISO 8601, e.g. 1990-05-15T09:23:00Z)")
    latitude: float = Field(..., ge=-90.0, le=90.0)
    longitude: float = Field(..., ge=-180.0, le=180.0)
    ayanamsa: Ayanamsa = Field(..., description="Explicit ayanamsa; Lahiri for Vedic matching")


class Placement(BaseModel):
    number: int = Field(..., description="1-based ordinal (Rashi 1-12 or Nakshatra 1-27)")
    name: str
    degrees_in: float = Field(..., description="Degrees into the current segment")
    degrees_to_boundary: float = Field(..., description="Degrees to the nearest segment edge")


class Graha(BaseModel):
    name: str
    longitude: float = Field(..., description="Sidereal ecliptic longitude, degrees [0,360)")
    speed: float = Field(..., description="Degrees/day; negative = retrograde")
    retrograde: bool
    rashi: Placement


class MoonDetail(BaseModel):
    longitude: float
    speed: float
    rashi: Placement
    nakshatra: Placement
    pada: int = Field(..., ge=1, le=4)


class Ascendant(BaseModel):
    longitude: float
    rashi: Placement


class ChartResponse(BaseModel):
    ayanamsa: Ayanamsa
    precision: str = Field(..., description="Ephemeris actually used; SWISS_EPHEMERIS_FULL when full precision")
    julian_day_ut: float
    moon: MoonDetail
    ascendant: Ascendant
    grahas: list[Graha]
