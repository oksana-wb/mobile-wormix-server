package com.pragmatix.achieve.domain;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 06.09.11 8:59
 */
public class GeneratorHelper {


    public static void main(String[] args) throws Exception {
//        generateAwards();
//        generateEnums();
//        generateStatEnums();
        generateGettersAndSetters();
//        generateDDL();
//        generateResultMap();
//        generateUpdate();
//        generateInsert();
    }

    private static void generateAwards() throws Exception {
        Map<WormixAchievements.AchievementName, String> map = new TreeMap<WormixAchievements.AchievementName, String>();

        List<String> strings = FileUtils.readLines(new File("D:\\Входящие\\AchievConfig.as"));
        WormixAchievements.AchievementName achieveEnum = null;
        int level=0;
        for(String line : strings) {
            line = line.trim();
            //new AchievRecord(Achievs.WAGER_WINNER, Messages.ACHIEV_WAGER_WINNER_DESCR, [
            if(line.startsWith("new AchievRecord(")) {
                line = line.replaceFirst("new AchievRecord\\(Achievs\\.","");
                line = line.substring(0, line.indexOf(",")).trim().toLowerCase();
                achieveEnum = WormixAchievements.AchievementName.valueOf(line);
//                System.out.println("");
//                System.out.println("                <!-- "+achieveEnum.getIndex()+" -->");
                level=0;
            } else if(line.startsWith("new AchievLevelRecord(")) {
                //new AchievLevelRecord(Messages.ACHIEV_WAGER_WINNER_1LEVEL_TITLE, {type:AchievAwardType.REACTION.value, count:5}, 50, Classes.WINNER, 60),
                String[] ss = line.split(",");
                int progress = Integer.valueOf(ss[3].trim());
                int points = Integer.valueOf(ss[5].replaceAll("[\\),]","").trim());
                String awardType = getFieldValue(line, "type").split("\\.")[1].toLowerCase();
                int awardCount=0;
                try {
                    awardCount = getFieldIntValue(line, "count");
                } catch (NumberFormatException e) {
                }
                String awardSetter = "";
                if(awardCount>0){
                  awardSetter="p:"+awardType+"=\""+awardCount+"\" ";
                }

                level++;
                String awardCode = ""+(level*1000+achieveEnum.getIndex());

                String block = map.get(achieveEnum) != null ? map.get(achieveEnum) : "";

                block += String.format("                <bean p:achievement=\"%s\" p:progress=\"%s\" p:points=\"%s\" %s p:awardType=\"%s\" parent=\"wormixAchieveAward\"/>\n",
                        achieveEnum, progress, points, awardSetter, awardCode);

                map.put(achieveEnum, block);
            }

        }

        for(WormixAchievements.AchievementName achievementName : map.keySet()) {
            System.out.println("");
            System.out.println("                <!-- "+achievementName.getIndex()+" -->");
            System.out.print(map.get(achievementName));
        }
    }

    private static String getFieldValue(String line, String fieldName) {
        int beginIndex = line.indexOf(fieldName + ":") + fieldName.length() + 1;
        int commaIndex = line.indexOf(",", beginIndex);
        int braceIndex = line.indexOf("}", beginIndex);
        int endIndex = commaIndex;
        if(commaIndex == -1 || commaIndex > braceIndex) {
            endIndex = braceIndex;
        }

        String s = line.substring(beginIndex, endIndex).trim();
        s = s.replaceAll("\"","");
        return s;
    }

    private static int getFieldIntValue(String line, String fieldName) {
        return Integer.parseInt(getFieldValue(line, fieldName));
    }

    private static void generateEnums() throws Exception {
        List<String> strings = FileUtils.readLines(new File("D:\\Входящие\\Achievs.as"));
        for(String line : strings) {
            line = line.trim();
            //public static const BURNED_ENEMIES:int = 0;
            if(line.startsWith("public static const")) {
                String enumName = line.replaceFirst("public static const ", "");
                String enumId = enumName.substring(enumName.lastIndexOf("=") + 1, enumName.lastIndexOf(";")).trim();
                enumName = enumName.substring(0, enumName.indexOf(":"));
                System.out.println(enumName.toLowerCase() + "(" + enumId + "),");
            }

        }
    }

    private static void generateStatEnums() throws Exception {
        List<String> strings = FileUtils.readLines(new File("D:\\Входящие\\Stats.as"));
        for(String line : strings) {
            line = line.trim();
            //public static const BURNED_ENEMIES:int = 0;
            if(line.startsWith("public static const")) {
                String enumName = line.replaceFirst("public static const ", "");
                String enumId = enumName.substring(enumName.lastIndexOf("=") + 1, enumName.lastIndexOf(";")).trim();
                enumName = enumName.substring(0, enumName.indexOf(":"));
                System.out.println("s_"+enumName.toLowerCase() + "(" + enumId + ", true),");
            }

        }
    }

    private static void generateInsert() {
        String fields = "           (profile_id, time_sequence, invested_award_points";
        String values = "           (#{profileId}, #{timeSequence}, #{investedAwardPoints}";
        for(WormixAchievements.AchievementName achievement : WormixAchievements.AchievementName.values()) {
            String achieveName = achievement.name();
            fields += ", " + achieveName;
            values += ", #{" + achieveName + "}";
        }
        System.out.println("        insert into achieve.worms_achievements");
        System.out.println(fields + ")");
        System.out.println("        values");
        System.out.println(values + ")");
    }

    private static void generateUpdate() {
        String pattern = "          #achievementName#=#{#achievementName#}";
        int i = 0;
        for(WormixAchievements.AchievementName achievement : WormixAchievements.AchievementName.values()) {
            String achieveName = achievement.name();
            System.out.print(pattern.replaceAll("#achievementName#", achieveName));
            i++;
            if(i < WormixAchievements.AchievementName.values().length) {
                System.out.println(",");
            }
        }
    }

    private static void generateResultMap() {
        String pattern = "<result property=\"#achievementName#\" column=\"#achievementName#\"/>";
        for(WormixAchievements.AchievementName achievement : WormixAchievements.AchievementName.values()) {
            String achieveName = achievement.name();
            System.out.println(pattern.replaceAll("#achievementName#", achieveName));
        }
    }

    private static void generateDDL() {
        String head =
                "-- DROP TABLE achieve.worms_achievements;\n" +
                        "\n" +
                        "CREATE TABLE achieve.worms_achievements\n" +
                        "(\n" +
                        "  profile_id character varying(32) NOT NULL,";
        String pattern = "  #achievementName# smallint NOT NULL DEFAULT 0,";
        String tail = "  CONSTRAINT worms_achievemens_pkey PRIMARY KEY (profile_id)\n" +
                ")\n" +
                "WITH (\n" +
                "  OIDS=FALSE\n" +
                ");\n" +
                "ALTER TABLE achieve.worms_achievements OWNER TO smos;";

        System.out.println(head);
        for(WormixAchievements.AchievementName achievement : WormixAchievements.AchievementName.values()) {
            String achieveName = achievement.name();
            System.out.println(pattern.replaceAll("#achievementName#", achieveName));
        }
        System.out.println(tail);
    }

    private static void generateGettersAndSetters() {
        String pattern =
                "    public short get#AchievementName#() {\n" +
                        "        return getAchievement(AchievementName.#achievementName#);\n" +
                        "    }\n" +
                        "\n" +
                        "    public void set#AchievementName#(short value) {\n" +
                        "        setAchievement(AchievementName.#achievementName#, value);\n" +
                        "    }\n";

        for(WormixAchievements.AchievementName achievement : WormixAchievements.AchievementName.values()) {
            String achieveName = achievement.name();
            System.out.println(pattern.replaceAll("#achievementName#", achieveName)
                    .replaceAll("#AchievementName#", achieveName.substring(0, 1).toUpperCase() + achieveName.substring(1)));
        }
    }
}
