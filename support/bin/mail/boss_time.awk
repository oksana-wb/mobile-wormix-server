$2 == "EndBattle" && $5 == "WINNER" && $6 == boss && getBattleTime($4) < 4*60 {
  printf("%s\t%s\t%s\n",getBattleTime($4), $4, $3)
}
function getBattleTime(line){
  split(line, arr2, ":")
  secs = arr2[3]
  mins = arr2[2]
  hours = arr2[1]
  return (hours * 60 * 60) + (mins * 60) + (secs) 
}