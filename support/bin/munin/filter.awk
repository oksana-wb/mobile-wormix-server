#17:36:01        425294  LEAVE_LOBBY     05.441  0(1/0)0/2/3/1:0/5/1/0   WAGER_PvP_DUEL(4)       15      0       5/[5,5]/302/0   3       11:1638627      0,66/0,20
BEGIN{
  hour = 60 * 60
  day = hour * 24
  month = day * 31
  year = month * 12

  start = getTime(from)
}

getTime($1) >= start {
  print $0
}

function getTime(line){
  split(line, arr_dt, "_")
  split(arr_dt[1], arr_d, "-")
  split(arr_dt[2], arr_t, ":")
  
  secs = arr_t[3]
  mins = arr_t[2]
  hours = arr_t[1]
  
  days = arr_d[3]
  months = arr_d[2]
  years = arr_d[1]
  
  return (years * year) + (months * month) + (days * day) + (hours * hour) + (mins * 60) + secs
}
 