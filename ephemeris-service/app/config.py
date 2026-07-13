"""Service configuration, sourced from the environment.

Only the ephemeris data path and the bind host/port are configurable; everything
about the astrology itself (Lahiri ayanamsa, full precision) is fixed in code so
it can never drift via config.
"""

from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    # Directory holding the Swiss Ephemeris .se1 data files.
    ephe_path: str = os.environ.get("EPHE_PATH", "./ephe")
    # Internal-only bind. The service must never be reachable from outside the host.
    host: str = os.environ.get("EPHEMERIS_HOST", "127.0.0.1")
    port: int = int(os.environ.get("EPHEMERIS_PORT", "8001"))


settings = Settings()
