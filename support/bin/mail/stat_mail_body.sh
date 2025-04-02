#!/bin/bash

server=/home/user/server
tmp=/home/user/bin/mail/tmp

yesterday=`date -d '1 day ago' +'%Y-%m-%d'`
pvpStat="${server}/logs/cdr/pvp_stat-${yesterday}.log.zip"

echo From: pragmatix.robot@gmail.com
echo Subject: $1 DAILY STAT by ${yesterday}
echo
echo PVP DESYNC:
unzip -p $pvpStat | awk '
$8 == 3 && $3 > 3 {
  print $7
}

$20 == 3 && $3 > 3 {
  print $19
}

$32 == 3 && $3 > 3 {
  print $31
}

$44 == 3 && $3 > 3 {
  print $43
}
' | awk -F: '{print $2}' | sort -n | uniq -dc | sort -nr | awk '
$1 >= 4 {
  print $0
}
'
echo
echo BOSS DESYNC:
unzip -p $pvpStat | awk '
$8 == 3 && $3 == 1{
  print $4"_"substr($7, 3)
}

$20 == 3 && $3 == 1 {
  print $4"_"substr($19, 3)
}
' | sort -n | uniq -dc | sort -nr 

rm $tmp/*.log
cp /home/user/server/logs/server/server-${yesterday}-*.log.zip $tmp/

for f in `ls $tmp/*.zip`
do
  unzip -q $f -d $tmp/
  rm $f
done
echo
echo REPLACE missionId in EndBattle:
cat $tmp/*.log | grep "hack detected! profile.missionId" | awk '{print $4}' | sort | uniq -c | sort -nr
echo
echo OLD CLIENT:
cat $tmp/*log | grep "Версия клиента устарела" | awk '{print $7" "$11}' | sort | uniq -c | sort -nr
