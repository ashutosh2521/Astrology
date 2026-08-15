# syntax=docker/dockerfile:1
#
# Ephemeris service image: pure Swiss Ephemeris calculator (pyswisseph). Built with the
# ephemeris-service/ directory as its context.
#
# The .se1 data files are deliberately NOT baked in (large AGPL binaries): they are mounted
# read-only at EPHE_PATH by the compose volume. Without them the service refuses to start,
# which is the intended "no silent Moshier fallback" precision guard, not a bug.

# ---- Build stage: install deps into a venv. pyswisseph has no wheel for this Python, so it
#      compiles from C source — build-essential provides gcc AND the libc headers (math.h etc.)
#      that a bare gcc lacks. None of this reaches the slim runtime image. ----
FROM python:3.12-slim AS build
RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential \
    && rm -rf /var/lib/apt/lists/*
RUN python -m venv /venv
ENV PATH=/venv/bin:$PATH
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# ---- Runtime stage: slim Python + the prebuilt venv + the app source ----
FROM python:3.12-slim AS runtime
COPY --from=build /venv /venv
ENV PATH=/venv/bin:$PATH
WORKDIR /app
COPY app app
RUN useradd --system --home-dir /app --shell /usr/sbin/nologin kundli
USER kundli
EXPOSE 8001
# Bind 0.0.0.0 INSIDE the container only. The port is never published to the host, so the
# service is reachable solely by the backend over the internal compose network.
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001"]
