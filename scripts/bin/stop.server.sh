#!/bin/bash

USER=vdddslep
SERVER=x4server
SERVER_DIR=/home/${USER}/server/x4coin-server
SCREEN_NAME=production

. ${SERVER_DIR}/support/bin/server-functions.sh

stop
