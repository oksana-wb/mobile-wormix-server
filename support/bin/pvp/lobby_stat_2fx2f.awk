BEGIN{
  battles=0
  total=0
  max=0
  maxs=""
  battlesTime=0
  longestBattle=0
  longestBattleS=""
}
$3 == 7 {
  times[1] = $12
  times[2] = $36

  time = getMax(times)
  fun(time)
  battles++
  battleTime = getBattleTime($5)
  battlesTime += battleTime
  if(battleTime > longestBattle){
    longestBattle = battleTime
    longestBattleS = $5" "$6
  }
}
END{
  printf("%s\t battles=%d\t avgL=%.2f sec.\t max=%s\t q4=%.1f%% q6=%.1f%% q8=%.1f%% q10=%.1f%%\t avgB=%.2f min. longest='%s'\n", \
    FILENAME, battles, total/battles/1000, maxs, q_arr[4]*100/battles, q_arr[6]*100/battles, q_arr[8]*100/battles, q_arr[10]*100/battles, battlesTime/battles/1000/60, longestBattleS)
}
function fun(line){
  time = getTime(line)
  total += time
  if(time > max){
    max = time
    maxs = line
  }
  q = $15
  if(q < 0.4){
    q_arr[4]++
  }else if(q < 0.6){
    q_arr[6]++
  }else if(q < 0.8){
    q_arr[8]++
  }else if(q < 1){
    q_arr[10]++
  }
}
function getMax(times){
  maxTime = 0
  maxTimeS = ""
  for (i in times){
    time = getTime(times[i])
    if(time > maxTime){
      maxTime = time
      maxTimeS = times[i]
    }
  }
  return maxTimeS
}
function getTime(line){
  mils=0
  secs=0
  mins=0
  hours=0
  split(line, arr, ".")
  if(length(arr[1]) == 0){
    mils = arr[2]
  }else{
    mils = arr[2]
    split(arr[1],arr2,":")
    if(length(arr2) == 1){
      secs = arr[1]
    }else if (length(arr2) == 2){
      secs = arr2[2]
      mins = arr2[1]
    }else if (length(arr2) == 3){
      secs = arr2[3]
      mins = arr2[2]
      hours = arr2[1]
    }        
  }
  return (hours * 60 * 60 * 1000) + (mins * 60 * 1000) + (secs * 1000) + mils
}
function getBattleTime(line){
  split(line, arr2, ":")
  secs = arr2[3]
  mins = arr2[2]
  hours = arr2[1]
  return (hours * 60 * 60 * 1000) + (mins * 60 * 1000) + (secs * 1000) 
}