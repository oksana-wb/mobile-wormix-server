#!/bin/bash

file=lobby/pvp_stat.log

for h in 00 01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20 21 22 23
do
  a=`cat $file | grep -e "^$h:" | wc -l`
  d=`cat $file | grep -e "^$h:" | grep DRAW_DESYNC | wc -l`
  k=$(($d * 100 / $a))
  echo $h $a $d $k
done
