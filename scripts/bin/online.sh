#!/bin/sh

echo main: `netstat -n | grep ESTABLISHED | grep :7081 | wc -l`

