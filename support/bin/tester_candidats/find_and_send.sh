#!/bin/sh

HOME=/home/user/bin/tester_candidats

$HOME/find_candidats.sh $1 $2 $3 $4 $5 $6 $8

echo """From: robot@pragmatix-corp.com 
Subject: VK TESTERS CANDIDATS

""" > $HOME/out/tmp
cat $HOME/out/result >> $HOME/out/tmp

cat $HOME/out/tmp | ssmtp $7 
