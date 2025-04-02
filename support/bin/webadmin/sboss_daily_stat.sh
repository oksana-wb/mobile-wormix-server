#!/bin/sh

unzip -p logs/cdr/cdr-pvp/pvp_stat-$1.log.zip | awk '
$4 ~ /\[[1-9][0-9]?[0-9]?,/ && $8 == 1 && $20 == 1 {print $4" NOT_WINNER"}
$4 ~ /\[[1-9][0-9]?[0-9]?,/ && ($8 == 0 || $20 == 0) {print $4" WINNER"}
' | sort | uniq -c
