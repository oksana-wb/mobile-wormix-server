$3 == "LEAVE_LOBBY" && $7 == wager {
  split($5, arr1, ")")
  split(arr1[2], arr2, ":")
  split(arr2[1], _1, "/")
  split(arr2[2], _2, "/")

  nm[1] = _1[1]+_2[1]
  nm[2] = _1[2]+_2[2]
  nm[3] = _1[3]+_2[3]
  nm[4] = _1[4]+_2[4]
  nm[5] = _1[5]+_2[5]
  nm[6] = _1[6]+_2[6]

  count(nm)
}
END{
  printf("sandbox.value %.0f\n", nmTotal[1]*1000/total)
  printf("level.value %.0f\n", nmTotal[2]*1000/total)
  printf("teamSize.value %.0f\n", nmTotal[3]*1000/total)
  printf("hp.value %.0f\n", nmTotal[4]*1000/total)
  printf("extra.value %.0f\n", nmTotal[5]*1000/total)
  printf("skill.value %.0f\n", nmTotal[6]*1000/total)
  printf("noWagers.value %.0f\n", nmTotal[7]*1000/total)
}
function count(arr){
  k = 0
  for(i in arr){
    if(arr[i] > 0){
      nmTotal[i] += arr[i]
      total += arr[i]
      k = 1
    }
  }
  if(k == 0){
    nmTotal[7]++
    total++
  }
}
