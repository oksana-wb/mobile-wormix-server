#!/bin/bash

if [ "$(date +%H':'%M)" == '00:00' ]
then
  exit 0
fi

server=android
target=/home/user/bin/munin/tmp/wormswar_${server}_pvp_stat.log
targetDump=/home/user/bin/munin/tmp/wormswar_${server}_stat.dump

yesterday=`date +%Y'-'%m'-'%d -d "1 day ago"`
today=`date +%Y'-'%m'-'%d`

zcat /home/user/server_${server}/logs/cdr/pvp_stat-${yesterday}.log.zip | awk -v date=${yesterday} -- '{print date"_"$0}' > ${target}
cat /home/user/server_${server}/logs/cdr/pvp_stat.log | awk -v date=${today} -- '{print date"_"$0}' >> ${target}

cat /home/user/server_${server}/logs/statistics/stat.dump > ${targetDump}

echo -n "online.pvp " >> ${targetDump}
netstat -n | grep ESTABLISHED | grep :6042 | wc -l >> ${targetDump}

echo -n "dao.value " >> ${targetDump}
echo "select count(*) from wormswar.user_profile where last_login_time > now() - interval '1 DAY'" | psql -d wormswar_android -U smos -t >> ${targetDump}

scp ${target} user@aurora.rmart.ru:/home/user/munin/var/
scp ${targetDump} user@aurora.rmart.ru:/home/user/munin/var/

server=mobile
target=/home/user/bin/munin/tmp/wormswar_${server}_pvp_stat.log
targetDump=/home/user/bin/munin/tmp/wormswar_${server}_stat.dump

zcat /home/user/server_${server}/logs/cdr/pvp_stat-${yesterday}.log.zip | awk -v date=${yesterday} -- '{print date"_"$0}' > ${target}
cat /home/user/server_${server}/logs/cdr/pvp_stat.log | awk -v date=${today} -- '{print date"_"$0}' >> ${target}

cat /home/user/server_${server}/logs/statistics/stat.dump > ${targetDump}

echo -n "online.pvp " >> ${targetDump}
netstat -n | grep ESTABLISHED | grep :6025 | wc -l >> ${targetDump}

echo -n "dao.value " >> ${targetDump}
echo "select count(*) from wormswar.user_profile where last_login_time > now() - interval '1 DAY'" | psql -d wormswar_mobile -U smos -t >> ${targetDump}

scp ${target} user@aurora.rmart.ru:/home/user/munin/var/
scp ${targetDump} user@aurora.rmart.ru:/home/user/munin/var/
