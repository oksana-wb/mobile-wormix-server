#!/bin/sh

echo main: `netstat -n | grep ESTABLISHED | grep :6000 | wc -l`
echo pvp: `netstat -n | grep ESTABLISHED | grep :6004 | wc -l`