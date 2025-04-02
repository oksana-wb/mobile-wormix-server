#!/bin/bash

USER=wormix
SERVER=vkontakte
SERVER_DIR=/home/${USER}/server
SCREEN_NAME=vk

. /home/${USER}/bin/wormix-server-functions.sh

case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        stop
        start
        ;;
    *)
        echo "Usage: $0 {start|stop|restart}"
        exit 1
        ;;
esac

exit 0
