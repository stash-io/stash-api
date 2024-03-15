#!/bin/bash
# This is a simple script to manage an instance of a Java application
# It can be used to start, stop and restart the application
#
# This is needed because I can't get the application to run as a systemd service,
# it keeps failing.
#
# Many thanks to:
# https://medium.com/@ameyadhamnaskar/running-java-application-as-a-service-on-centos-599609d0c641

TARGET_DIR=$HOME/stash/publish
SERVICE_NAME=stash
PATH_TO_JAR=$TARGET_DIR/eu.tortitas.stash.stash-api-all.jar

PID_PATH_NAME=/tmp/$SERVICE_NAME-pid
ERROR_LOG_PATH_NAME=/var/log/$SERVICE_NAME-error.log
LOG_PATH_NAME=/var/log/$SERVICE_NAME.log

cd $TARGET_DIR

case $1 in
start)
  echo "Starting $SERVICE_NAME ..."
  if [ ! -f $PID_PATH_NAME ]; then
    nohup java -jar $PATH_TO_JAR /tmp 2>> $ERROR_LOG_PATH_NAME >>$LOG_PATH_NAME &
    echo $! > $PID_PATH_NAME

    echo "$SERVICE_NAME started ..."
  else
    echo "$SERVICE_NAME is already running ..."
  fi
;;
stop)
  if [ -f $PID_PATH_NAME ]; then
    PID=$(cat $PID_PATH_NAME);
    echo "$SERVICE_NAME stoping ..."
    kill $PID;
    echo "$SERVICE_NAME stopped ..."
    rm $PID_PATH_NAME
  else
    echo "$SERVICE_NAME is not running ..."
  fi
;;
restart)
  if [ -f $PID_PATH_NAME ]; then
    PID=$(cat $PID_PATH_NAME);
    echo "$SERVICE_NAME stopping ...";
    kill $PID;
    echo "$SERVICE_NAME stopped ...";
    rm $PID_PATH_NAME
    echo "$SERVICE_NAME starting ..."
    nohup java -jar $PATH_TO_JAR /tmp 2>> /dev/null >> /dev/null &
    echo $! > $PID_PATH_NAME
    echo "$SERVICE_NAME started ..."
  else
    echo "$SERVICE_NAME is not running ..."
  fi
;;
esac
