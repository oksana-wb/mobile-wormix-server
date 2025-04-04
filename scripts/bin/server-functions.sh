#!/bin/bash

# find pid of the server by SERVER marker
findServerPid(){
  PID=`ps ax | grep "server.id=${SERVER}" | grep -v grep | awk '{print $1}' | tail -1`
}
# find pid of the maven's launcher by SERVER profile name
findExecPid(){
  PID=`ps ax | grep "${SERVER}" | grep -v grep | awk '{print $1}' | tail -1`
}

start(){
  echo "Start server [${SERVER}] ..."

  if [ ${USER} = `whoami` ]; then
    LAST_PATH=`pwd`
    cd ${SERVER_DIR}
    screen -d -m -S ${SCREEN_NAME} ./start.sh
    #./start.sh
    cd ${LAST_PATH}
  else
    su - $USER -c "cd ${SERVER_DIR}; screen -d -m -S ${SCREEN_NAME} ./start.sh"
    #su - $USER -c "cd ${SERVER_DIR}; ./start.sh"
  fi

  sleep 10

  findExecPid

  if [ -n "${PID}" ]; then
    echo "Server [${SERVER}] started"
  else
    echo "Server start FAILURE!"
  fi

  if [ ${USER} = `whoami` ]; then
    echo "Type [screen -r $SCREEN_NAME] to resume a server screen session"
  fi
}

stop(){
  findServerPid

  if [ -n "$PID" ]; then
    echo "Try soft stop server [${SERVER}] with pid [${PID}] ..."
    kill ${PID}
    sleep 1
    findServerPid
    i=0
    while [ -n "$PID" ] && [ $i -lt 10 ]; do
      echo "Waiting for stopping server [${SERVER}] ..."
      sleep 1
      findServerPid
      i=$(($i + 1))
    done

    if [ -n "${PID}" ]; then
      echo "Kill server [${SERVER}] with pid [${PID}]"
      kill -9 ${PID}
    else
      echo "Server [${SERVER}] stopped"
    fi
  else
    echo "Server [${SERVER}] is not running"
  fi
}
