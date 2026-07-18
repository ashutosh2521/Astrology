#!/usr/bin/env bash
#
# Nightly SQLite backup to S3 (README: "Backed up by nightly copy to S3").
# Uses `sqlite3 .backup`, which is safe to run against the live DB — it takes a
# consistent snapshot without stopping the backend.
#
# Config via /etc/kundli/backup.env (see infra/env/backup.env.example):
#   KUNDLI_DB_PATH   path to the live SQLite file
#   KUNDLI_S3_BUCKET s3://bucket/prefix destination
# AWS credentials come from the instance's IAM role (preferred) or the standard
# AWS_* environment / ~/.aws config.
set -euo pipefail

: "${KUNDLI_DB_PATH:=/var/lib/kundli/kundli.db}"
: "${KUNDLI_S3_BUCKET:?set KUNDLI_S3_BUCKET (e.g. s3://my-bucket/kundli) in /etc/kundli/backup.env}"

STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

SNAP="$WORK/kundli-$STAMP.db"
sqlite3 "$KUNDLI_DB_PATH" ".backup '$SNAP'"
gzip "$SNAP"

aws s3 cp "$SNAP.gz" "${KUNDLI_S3_BUCKET%/}/kundli-$STAMP.db.gz"
echo "Backed up $KUNDLI_DB_PATH -> ${KUNDLI_S3_BUCKET%/}/kundli-$STAMP.db.gz"
