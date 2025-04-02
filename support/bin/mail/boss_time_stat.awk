$2 == "EndBattle" && $5 == "WINNER" && $6 == boss {
  i = int(getBattleTime($4)/60) + 1
  timeArr[i] = timeArr[i] + 1
}
function getBattleTime(line){
  split(line, arr2, ":")
  secs = arr2[3]
  mins = arr2[2]
  hours = arr2[1]
  return (hours * 60 * 60) + (mins * 60) + (secs) 
}
END {
  print "boss:"boss 
  for (i = 1; i <= length(timeArr); i++)
     if(timeArr[i] > 0)
       print "<"i" "timeArr[i]
}