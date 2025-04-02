#!/bin/sh

netstat -n -p | grep SYN_REC | awk '{print $5}' | awk -F: '{print $1}' | sort -n | uniq -c | sort -nr | head -n10
