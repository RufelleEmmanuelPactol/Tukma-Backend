#!/bin/sh
gunicorn --bind 0.0.0.0:8000 app:app &
nginx -g "daemon off;"