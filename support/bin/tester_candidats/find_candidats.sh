#!/bin/bash

CDR=/home/user/server/logs/cdr
OUT=/home/user/bin/tester_candidats/out

DAYS=$1
RATING_FROM=$2
RATING_TO=$3
LEVEL_FROM=$4
PVP_BATTLES_FROM=$5
BOSS_WIN_FROM=$6
LEVEL_TO=$7

echo DAYS=$DAYS > $OUT/result
echo RATING_FROM=${RATING_FROM}000 >> $OUT/result
echo RATING_TO=${RATING_TO}000 >> $OUT/result
echo LEVEL_FROM=$LEVEL_FROM >> $OUT/result
echo LEVEL_TO=$LEVEL_TO >> $OUT/result
echo PVP_BATTLES_FROM=$PVP_BATTLES_FROM >> $OUT/result
echo BOSS_WIN_FROM=$BOSS_WIN_FROM >> $OUT/result
echo "=======================" >> $OUT/result

# PVP/PVE battles
echo -n > $OUT/pvp_stat.log
echo "concat pvp_stat files for $DAYS days ..."
find $CDR/pvp_stat-*.zip -mtime -$DAYS -exec echo {} \; -exec sh -c "zcat {} >> $OUT/pvp_stat.log" \;
echo "fill pvp_active_candidats ..."
awk '$3 > 3 {print $7"\n"$19"\n"$31"\n"$43"\n"}' $OUT/pvp_stat.log | awk -F: '$1 > 0 {print $2}' | sort | uniq -c | awk -v battles="$PVP_BATTLES_FROM" '$1 >= battles {print $2}' > $OUT/pvp_active_candidats
echo "fill pve_active ..."
awk '$3 == 1 && $8 == 0 {print $7} $3 == 1 && $20 == 0 {print $19}' $OUT/pvp_stat.log | awk -F: '$1 > 0 {print $2}' > $OUT/pve_active

# Single Boss battles
echo -n > $OUT/battles_stat.log
echo "concat battles_stat files for $DAYS days ..."
find $CDR/battles_stat-*.zip -mtime -$DAYS -exec echo {} \; -exec sh -c "zcat {} >> $OUT/battles_stat.log" \;
echo "append pve_active ..."
cat $OUT/battles_stat.log | awk '$2 == "EndBattle" && $5 == "WINNER" {print $3}' >> $OUT/pve_active

echo "fill pve_active_candidats ..."
cat $OUT/pve_active | sort | uniq -c | awk -v win="$BOSS_WIN_FROM" '$1 >= win {print $2}' > $OUT/pve_active_candidats

echo found `cat $OUT/pvp_active_candidats | wc -l` PVP active candidats
echo found `cat $OUT/pve_active_candidats | wc -l` PVE active candidats

echo "find both PVP and PVE active candidats ..."
cat $OUT/pve_active_candidats $OUT/pvp_active_candidats | sort | uniq -c | awk 'BEGIN{i=0} $1 == 2 && i == 0 {print $2; i++} $1 == 2 && i > 0 {print ","$2}' > $OUT/pvp_pve_candidats
echo found `cat $OUT/pvp_pve_candidats | wc -l` PVP and PVE active candidats

# Profiles
ACTIVE=`cat $OUT/pvp_pve_candidats`
INTERVAL=$(($DAYS+1))
echo "select (and filter by level and rating) candidats from DB ..."
echo """
  select id from wormswar.user_profile UP where rating >= $RATING_FROM * 1000 and rating <= $RATING_TO * 1000
    and level > $LEVEL_FROM and level <= $LEVEL_TO
    and last_login_time > now() - interval '$INTERVAL DAYS'
    and not exists (select * from wormswar.ban_list where profile_id = UP.id)
    and id in ($ACTIVE)
""" | psql -d wormswar -U smos -t > $OUT/candidats

echo found `cat $OUT/candidats | wc -l` candidats.

cat $OUT/candidats >> $OUT/result

rm $OUT/pvp_stat.log
rm $OUT/battles_stat.log
rm $OUT/pvp_active_candidats
rm $OUT/pve_active
rm $OUT/pve_active_candidats
rm $OUT/pvp_pve_candidats
rm $OUT/candidats