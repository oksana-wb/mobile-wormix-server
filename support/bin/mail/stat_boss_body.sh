#!/bin/bash

server=/home/user/server
home=/home/user/bin/mail

yesterday=`date -d '1 day ago' +'%Y-%m-%d'`
stat="${server}/logs/cdr/battles_stat-${yesterday}.log.zip"
pvpStat="${server}/logs/cdr/pvp_stat-${yesterday}.log.zip"

echo From: pragmatix.robot@gmail.com
echo Subject: $1 BOSS STAT by ${yesterday}
echo
echo FAST WIN:
for i in 20 21
do
  echo $i:
  unzip -p $stat | awk -v boss=$i -f $home/boss_time.awk | sort -n | awk '{print $2" "$3}'
done
echo
echo Heroic BOSS STAT:
unzip -p $pvpStat | awk '
$4 ~ /\[[1-9][0-9]?[0-9]?,/ && $8 == 1 && $20 == 1 {print $4": NOT_WINNER"}
$4 ~ /\[[1-9][0-9]?[0-9]?,/ && ($8 == 0 || $20 == 0) {print $4": WINNER"}
' | sort | uniq -c
#echo
#echo ALL BOOSS STAT:
#for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21
#do
#  unzip -p $stat | awk -v boss=$i -f $home/boss_time_stat.awk 
#done
