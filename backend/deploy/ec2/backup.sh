#!/bin/bash
set -euo pipefail
umask 077
mountpoint -q /srv/g6
install -d -m 0700 /srv/g6/backups
target="/srv/g6/backups/databases-$(date -u +%Y%m%dT%H%M%SZ).sql.gz"
docker exec g6-postgres pg_dumpall -U postgres | gzip > "$target.partial"
gzip -t "$target.partial"
mv -- "$target.partial" "$target"
find /srv/g6/backups -maxdepth 1 -type f -name 'databases-*.sql.gz' -mtime +8 -delete
