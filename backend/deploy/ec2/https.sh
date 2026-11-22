#!/bin/bash
set -euo pipefail
IP="${1:?Usage: https.sh elastic-ip}"
[[ "$IP" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]
mountpoint -q /srv/g6
install -d -m 0755 /srv/g6/acme
install -d -m 0700 /srv/g6/letsencrypt
cat > /etc/nginx/nginx.conf <<'NGINX'
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /run/nginx.pid;
events { worker_connections 1024; }
http {
    include /etc/nginx/mime.types;
    server {
        listen 80 default_server;
        server_name _;
        location /.well-known/acme-challenge/ { root /srv/g6/acme; }
        location / { return 503; }
    }
}
NGINX
nginx -t
systemctl enable --now nginx
docker pull certbot/certbot:v5.4.0
docker run --rm -v /srv/g6/letsencrypt:/etc/letsencrypt -v /srv/g6/acme:/var/www/certbot \
    certbot/certbot:v5.4.0 certonly --non-interactive --agree-tos --register-unsafely-without-email \
    --preferred-profile shortlived --webroot --webroot-path /var/www/certbot --ip-address "$IP"
cat > /etc/nginx/nginx.conf <<NGINX
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /run/nginx.pid;
events { worker_connections 1024; }
http {
    include /etc/nginx/mime.types;
    server {
        listen 80 default_server;
        server_name $IP;
        location /.well-known/acme-challenge/ { root /srv/g6/acme; }
        location / { return 308 https://$IP\$request_uri; }
    }
    server {
        listen 443 ssl default_server;
        server_name $IP;
        ssl_certificate /srv/g6/letsencrypt/live/$IP/fullchain.pem;
        ssl_certificate_key /srv/g6/letsencrypt/live/$IP/privkey.pem;
        ssl_protocols TLSv1.2 TLSv1.3;
        client_max_body_size 1m;
        location / {
            proxy_pass http://127.0.0.1:8080;
            proxy_http_version 1.1;
            proxy_set_header Host \$host;
            proxy_set_header X-Forwarded-For \$remote_addr;
            proxy_set_header X-Forwarded-Proto https;
            proxy_connect_timeout 3s;
            proxy_read_timeout 30s;
        }
    }
}
NGINX
nginx -t
systemctl reload nginx
cat > /etc/systemd/system/g6-cert-renew.service <<'UNIT'
[Unit]
Description=Renew G6 IP certificate and reload HTTPS
After=docker.service nginx.service
Requires=docker.service
RequiresMountsFor=/srv/g6
[Service]
Type=oneshot
ExecStart=/usr/bin/docker run --rm -v /srv/g6/letsencrypt:/etc/letsencrypt -v /srv/g6/acme:/var/www/certbot certbot/certbot:v5.4.0 renew --quiet
ExecStart=/usr/sbin/nginx -t
ExecStart=/usr/bin/systemctl reload nginx
UNIT
cat > /etc/systemd/system/g6-cert-renew.timer <<'UNIT'
[Unit]
Description=Check G6 short-lived certificate twice daily
[Timer]
OnCalendar=*-*-* 00,12:00:00
RandomizedDelaySec=900
Persistent=true
[Install]
WantedBy=timers.target
UNIT
cat > /etc/systemd/system/g6-backup.service <<'UNIT'
[Unit]
Description=G6 PostgreSQL logical backup
After=docker.service
Requires=docker.service
RequiresMountsFor=/srv/g6
[Service]
Type=oneshot
ExecStart=/bin/bash /opt/g6/backup.sh
UNIT
cat > /etc/systemd/system/g6-backup.timer <<'UNIT'
[Unit]
Description=Daily G6 database backup
[Timer]
OnCalendar=*-*-* 04:00:00
RandomizedDelaySec=600
Persistent=true
[Install]
WantedBy=timers.target
UNIT
systemctl daemon-reload
systemctl enable --now g6-cert-renew.timer g6-backup.timer
