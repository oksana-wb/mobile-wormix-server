package com.pragmatix.webadmin.model;

import com.pragmatix.app.common.CheatTypeEnum;
import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.common.Race;
import com.pragmatix.app.common.TeamMemberType;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.model.BattleParticipant;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public interface PvpBattleStats {

    class Profile {
        public long profileId;
        public short socialNetId;
        public int rating;
        public int dailyRating;
        public int level;
        public int rankPoints;
        public List<Unit> units = List.of();

        @Override
        public String toString() {
            return "{" +
                    "profileId=" + profileId +
                    ", socialNetId=" + socialNetId +
                    ", rating=" + rating +
                    ", dailyRating=" + dailyRating +
                    ", level=" + level +
                    ", rankPoints=" + rankPoints +
                    ", units=" + units +
                    '}';
        }
    }

    class Unit {
        public long id;
        public TeamMemberType type;
        public int level;
        public int armor;
        public int attack;
        public int exp;
        public Race race;
        public short hat;
        public short kit;

        @Override
        public String toString() {
            return "{" +
                    "id=" + id +
                    ", level=" + level +
                    ", armor=" + armor +
                    ", attack=" + attack +
                    ", exp=" + exp +
                    ", race=" + race +
                    ", hat=" + hat +
                    ", kit=" + kit +
                    ", type=" + type +
                    '}';
        }
    }

    class PvpBattleLogRecord {
        public long battleId;
        public LocalDateTime start;
        public LocalDateTime finish;
        public Duration suspendTime = Duration.ZERO;
        public PvpBattleType battleType;
        public BattleWager wager_raw;
        public int wager;
        public List<Short> missionIds = List.of();
        public int mapId;
        public String special = "";
        public Duration duration = Duration.ZERO;
        public int turnCount;
        public List<Participant> participants = List.of();
        public int penalty;
        public PvpBattleLog battleLog;
        public List<ChatMessage> chatLog = List.of();

        @Override
        public String toString() {
            return "{" +
                    "battleId=" + battleId +
                    ", start=" + start +
                    ", finish=" + finish +
                    ", suspendTime=" + suspendTime +
                    ", battleType=" + battleType +
                    ", wager_raw=" + wager_raw +
                    ", wager=" + wager +
                    ", missionIds=" + missionIds +
                    ", mapId=" + mapId +
                    ", special='" + special + '\'' +
                    ", duration=" + duration +
                    ", turnCount=" + turnCount +
                    ", participants=" + participants +
                    ", penalty=" + penalty +
                    ", battleLog=" + battleLog +
                    ", chatLog=" + chatLog +
                    '}';
        }
    }

    class ChatMessage {
        public LocalDateTime date;
        public int profileId;
        public String message;
        public boolean teamsMessage;
    }

    class PvpBattleLog {
        public List<Turn> turns = List.of();

        @Override
        public String toString() {
            return String.valueOf(turns);
        }
    }

    class Turn {
        public int turnNum;
        public long start;
        public byte playerNum;
        public boolean valid = true;
        public CheatTypeEnum reason = CheatTypeEnum.UNCHECKED;
        /**
         * в случае valid=false - к каким последствиям для игрока приведет:
         * DISCARD - отмена боя
         * BAN - бан
         */
        public String consequence = "";
        public long cheatFrame;
        /**
         * Сколько выстрелов за этот ход сделано каждым оружием: {weaponId -> count}
         */
        public Map<Integer, Integer> shotsByWeapon = new HashMap<>();
        /**
         * Какое оружие сейчас выбрано
         */
        public int currentWeaponId;

        @Override
        public String toString() {
            return "{" +
                    "turnNum=" + turnNum +
                    ", start='" + AppUtils.formatDate(new Date(start)) + '\'' +
                    ", playerNum=" + playerNum +
                    ", valid=" + valid +
                    ", reason=" + reason +
                    ", consequence='" + consequence + '\'' +
                    ", cheatFrame=" + cheatFrame +
                    ", shotsByWeapon=" + shotsByWeapon +
                    ", currentWeaponId=" + currentWeaponId +
                    '}';
        }
    }

    class Award {
        public int rating;
        public int rankPoints;

        @Override
        public String toString() {
            return "{" +
                    "rating=" + rating +
                    ", rankPoints=" + rankPoints +
                    '}';
        }
    }

    class Participant {
        public Profile profile;
        public PvpBattleResult battleResult;
        public long offlineTime;
        public Duration setBattleResultAgo = Duration.ZERO;
        public int team;
        public int playerNum;
        public BattleParticipant.State lastState;
        public int battles;
        public double trueSkillMean;
        public double trueSkillSpread;
        public Award awarded;
        public Duration lobbyTime = Duration.ZERO;
        public int gridQuality;
        public int groupHp;
        public int matchedCandidats;
        public String notMatchByFor;
        public String notMatchByThis;
        public double matchQuality;
        public int[] reactionLevel;

        @Override
        public String toString() {
            return "{" +
                    "profile=" + profile +
                    ", battleResult=" + battleResult +
                    ", offlineTime=" + offlineTime +
                    ", setBattleResultAgo=" + setBattleResultAgo +
                    ", team=" + team +
                    ", playerNum=" + playerNum +
                    ", lastState=" + lastState +
                    ", battles=" + battles +
                    ", trueSkillMean=" + trueSkillMean +
                    ", trueSkillSpread=" + trueSkillSpread +
                    ", awarded=" + awarded +
                    ", lobbyTime=" + lobbyTime +
                    ", gridQuality=" + gridQuality +
                    ", groupHp=" + groupHp +
                    ", matchQuality=" + matchQuality +
                    ", reactionLevel=" + Arrays.toString(reactionLevel) +
                    '}';
        }
    }

}
