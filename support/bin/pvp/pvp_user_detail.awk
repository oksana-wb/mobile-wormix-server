#usage: awk -v user=$1 -v detail=$3 -f script.awk $2
BEGIN {
    if (detail == 1 ){
        print "Время боя\tСтавка\tИгрок\t\tИтог\t+/-\tРейтинг"
    }
}
$7 == user { 
    if ( $8 == 1 ) {
	win++
    }
    if ( $8 == -1 ) {
	defeat++
    }
    if ( $8 == 0 ) {
	draw++
    }
    rating+=$9
    if (detail == 1 ){
        print $1"\t"$4"\t"$7"\t"$8"\t"$9"\t"rating"\t"$10 
    }
}
END {
    print user":"
    print "Побед: " win
    print "Поражений: " defeat
    print "Ничьих: " draw
    print "Всего: " win + defeat + draw
    print "Заработано рейтинга: " rating
}