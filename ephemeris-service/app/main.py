"""FastAPI ephemeris service.

Internal-only microservice: given a UTC instant and coordinates, return sidereal
(Lahiri) chart data. Called by the Spring Boot backend at profile-creation time,
never exposed to the internet.

Startup fails loudly if the service cannot confirm full Swiss Ephemeris precision,
so it can never come up in silent Moshier-fallback mode.
"""

from __future__ import annotations

from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException

from . import astro, ephemeris
from .config import settings
from .models import (
    Ascendant,
    ChartRequest,
    ChartResponse,
    Graha,
    MoonDetail,
    Placement,
)

PRECISION_FULL = "SWISS_EPHEMERIS_FULL"


@asynccontextmanager
async def lifespan(app: FastAPI):
    ephemeris.configure(settings.ephe_path)
    # Refuse to start in Moshier fallback — accuracy guarantee is non-negotiable.
    ephemeris.self_check()
    yield


app = FastAPI(title="Kundli Ephemeris Service", lifespan=lifespan)


def _placement(p: astro.PlacementIn) -> Placement:
    return Placement(
        number=p.number,
        name=p.name,
        degrees_in=round(p.degrees_in, 6),
        degrees_to_boundary=round(p.degrees_to_boundary, 6),
    )


@app.get("/health")
async def health() -> dict[str, str]:
    """Liveness probe. Re-asserts full precision so a degraded process reports unhealthy.

    ``async def`` is deliberate here and on /chart: sync endpoints run on FastAPI's
    threadpool, and Swiss Ephemeris state is thread-local in common pyswisseph builds
    (see app/ephemeris.py). Async endpoints run on the single event-loop thread — the
    same one startup configured. Calculations are ~1ms, so blocking the loop is fine
    at this service's scale.
    """
    try:
        ephemeris.self_check()
    except ephemeris.PrecisionError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    return {"status": "ok", "precision": PRECISION_FULL}


@app.post("/chart", response_model=ChartResponse)
async def chart(req: ChartRequest) -> ChartResponse:
    jd = ephemeris.julian_day_ut(
        req.utc.year,
        req.utc.month,
        req.utc.day,
        req.utc.hour + req.utc.minute / 60.0 + req.utc.second / 3600.0,
    )

    try:
        grahas = ephemeris.compute_grahas(jd)
        asc_lon = ephemeris.compute_ascendant(jd, req.latitude, req.longitude)
    except ephemeris.PrecisionError as exc:
        # Never degrade to a low-precision answer; surface it as a hard failure.
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except Exception as exc:
        # e.g. swisseph.Error from houses_ex — surface as JSON detail, not a bare 500.
        raise HTTPException(
            status_code=500, detail=f"chart computation failed: {exc}") from exc

    moon_pos = next(g for g in grahas if g.name == "Moon")
    moon = MoonDetail(
        longitude=round(moon_pos.longitude, 6),
        speed=round(moon_pos.speed, 6),
        rashi=_placement(astro.rashi_of(moon_pos.longitude)),
        nakshatra=_placement(astro.nakshatra_of(moon_pos.longitude)),
        pada=astro.pada_of(moon_pos.longitude),
    )

    return ChartResponse(
        ayanamsa=req.ayanamsa,
        precision=PRECISION_FULL,
        julian_day_ut=jd,
        moon=moon,
        ascendant=Ascendant(
            longitude=round(asc_lon, 6),
            rashi=_placement(astro.rashi_of(asc_lon)),
        ),
        grahas=[
            Graha(
                name=g.name,
                longitude=round(g.longitude, 6),
                speed=round(g.speed, 6),
                retrograde=g.speed < 0,
                rashi=_placement(astro.rashi_of(g.longitude)),
            )
            for g in grahas
        ],
    )
