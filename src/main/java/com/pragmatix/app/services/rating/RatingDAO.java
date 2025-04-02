package com.pragmatix.app.services.rating;

import com.pragmatix.app.messages.structures.RatingProfileStructure;
import com.pragmatix.app.services.RaceService;
import com.pragmatix.app.services.SkinService;
import com.pragmatix.server.Server;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.05.2016 14:53
 */
@Service
public class RatingDAO {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private SkinService skinService;

    /**
     * вернет список рейтингоов лучших игроков в лиге
     *
     * @param league         лига
     * @param overheadFactor множитель больше единицы - с каким запасом выбирать игроков. Влияен на наполняемость лиг
     * @param lastLoginDays  учитывать в топе только тех игроков, которые заходили хотя бы lastLoginDays дней назад
     * @return список структур
     */

    public List<RatingProfileStructure> getTopPlayers(League league, double overheadFactor, int lastLoginDays, int MAX_TOP) {
        final List<RatingProfileStructure> result = new ArrayList<RatingProfileStructure>();
        String query = "SELECT a.id, armor, attack, level, money, realmoney, reaction_rate, rating, hat, coalesce(race, 0), coalesce(kit, 0), name," +
                " (CASE WHEN wg.profile_id IS NOT NULL THEN 2 ELSE 1 END) + (CASE WHEN wg.team_member_3 IS NOT NULL THEN 1 ELSE 0 END) + (CASE WHEN wg.team_member_4 IS NOT NULL THEN 1 ELSE 0 END) AS group_size  FROM \n" +
                " (SELECT  a.id, armor, attack, level, money, realmoney, reaction_rate, rating,  hat, race, kit, name\n" +
                " FROM wormswar.user_profile a \n" +
                "  WHERE a.id NOT IN (SELECT profile_id \n" +
                "                     FROM wormswar.ban_list \n" +
                "                     WHERE end_date IS NULL OR end_date > now() \n" +
                "     ) \n" +
                " AND rating >= #min# AND rating < #max# #lastLoginDaysCond#\n" +
                "ORDER BY a.rating DESC \n" +
                "LIMIT #limit# ) AS a\n" +
                "LEFT JOIN wormswar.worm_groups wg ON a.id=wg.profile_id ";

        int maxInTop = MAX_TOP * league.getDivisionCount();
        int limit = league.getDivisionCount() > 1 ? (int) (maxInTop * overheadFactor) : maxInTop;
        query = query
                .replaceFirst("#min#", "" + league.getMin())
                .replaceFirst("#max#", "" + league.getMax())
                .replaceFirst("#limit#", "" + limit)
        ;
        if(lastLoginDays > 0) {
            query = query.replaceFirst("#lastLoginDaysCond#", "AND last_login_time > now() - INTERVAL '" + lastLoginDays + " DAYS'");
        } else {
            query = query.replaceFirst("#lastLoginDaysCond#", "");
        }
        Server.sysLog.info(query);
        jdbcTemplate.query(query, res -> {
            RatingProfileStructure structure = new RatingProfileStructure();
            int i = 1;
            structure.id = res.getLong(i++);
            structure.armor = res.getShort(i++);
            structure.attack = res.getShort(i++);
            structure.level = res.getShort(i++);
            structure.money = res.getInt(i++);
            structure.realMoney = res.getInt(i++);
            structure.reactionRate = res.getInt(i++);
            structure.rating = res.getInt(i++);
            structure.hat = res.getShort(i++);
            short race = res.getShort(i++);
            if(race == 0) {
                // значит раса закодирована в hat
                structure.race = RaceService.getRaceId(structure.hat);
                structure.hat = RaceService.getHatId(structure.hat);
            } else {
                structure.race = race;
            }
            structure.kit = res.getShort(i++);
            structure.name = res.getString(i++);
            structure.groupCount = res.getInt(i++);
            result.add(structure);
        });
        return result;
    }

    public List<RatingProfileStructure> getTopPlayers(int MAX_TOP) {
        final List<RatingProfileStructure> result = new ArrayList<>();
        String query = "SELECT a.id, armor, attack, level, money, realmoney, reaction_rate, rating, hat, coalesce(race, 0), skins, coalesce(kit, 0), name," +
                " (CASE WHEN wg.profile_id IS NOT NULL THEN 2 ELSE 1 END) + (CASE WHEN wg.team_member_3 IS NOT NULL THEN 1 ELSE 0 END) + (CASE WHEN wg.team_member_4 IS NOT NULL THEN 1 ELSE 0 END) AS group_size  FROM \n" +
                " (SELECT  a.id, armor, attack, level, money, realmoney, reaction_rate, rating,  hat, race, skins, kit, name\n" +
                " FROM wormswar.user_profile a \n" +
                "  WHERE a.id NOT IN (SELECT profile_id \n" +
                "                     FROM wormswar.ban_list \n" +
                "                     WHERE end_date IS NULL OR end_date > now())" +
                "     AND last_login_time > now() - INTERVAL '6 MONTHS' \n" +
                "ORDER BY a.rating DESC \n" +
                "LIMIT " + MAX_TOP + " ) AS a\n" +
                "LEFT JOIN wormswar.worm_groups wg ON a.id=wg.profile_id ";
        Server.sysLog.info(query);
        jdbcTemplate.query(query, res -> {
            RatingProfileStructure structure = new RatingProfileStructure();
            int i = 1;
            structure.id = res.getLong(i++);
            structure.armor = res.getShort(i++);
            structure.attack = res.getShort(i++);
            structure.level = res.getShort(i++);
            structure.money = res.getInt(i++);
            structure.realMoney = res.getInt(i++);
            structure.reactionRate = res.getInt(i++);
            structure.rating = res.getInt(i++);
            structure.hat = res.getShort(i++);
            short race = res.getShort(i++);
            if(race == 0) {
                // значит раса закодирована в hat
                structure.race = RaceService.getRaceId(structure.hat);
                structure.hat = RaceService.getHatId(structure.hat);
            } else {
                structure.race = race;
            }
            byte[] skins = res.getBytes(i++);
            structure.skin = skins != null ? skinService.getSkin(skins, structure.race) : 0;
            structure.kit = res.getShort(i++);
            structure.name = res.getString(i++);
            structure.groupCount = res.getInt(i++);
            result.add(structure);
        });
        return result;
    }

    public List<Long> getSeasonTopPlayers(int maxTop) {
        String query = "SELECT profile_id from wormswar.ranks " +
                "ORDER BY case when best_rank = 0 then 0 else 1 end, rank_points DESC " +
                "LIMIT " + maxTop;
        Server.sysLog.info(query);
        return jdbcTemplate.queryForList(query, Long.class);
    }

}
