def boss = [
        1  : "Фермер",
        2  : "Охотник",
        3  : "Маньяки",
        4  : "Минёр",
        6  : "Сержант",
        7  : "Пылающие зомби",
        8  : "Блуждающие духи",
        9  : "Шаман Вуду",
        10 : "Иллюзионист",
        11 : "Викинги",
        12 : "Пираты",
        13 : "Мастер ветра",
        14 : "Якудза",
        15 : "Оживший капитан",
        16 : "Ромео и Джульетта",
        18 : "Древний призрак",
        101: "Паладин",
        104: "Телепат",
]
def find = false
new File("../../src/main/resources/battle-beans.xml").eachLine { line ->
    if(!find && line.contains("heroicMissionSettings")) find = true
    if(find && line.contains("<entry key=")) {
        def bossCouple = line.split("\"")[1].split("_")
        println("BossBattle.missionId.${bossCouple[0]}.${bossCouple[1]}=${boss.get(bossCouple[0] as int)} / ${boss.get(bossCouple[1] as int)}")
    }
    if(find && line.contains("</property>")) find = false
}
