package com.pragmatix.app.settings;

import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 16.10.2017 15:44
 */
public class DailyBonusAwardSettings {

    // NB: week length устанавливается в setDailyBonusAward на основании конфигов, чтобы тот же код мог работать и для мобилок, где неполная неделя
    private int WEEK_LENGTH = 7;

    public static class WeeklyAward {

        private int needLevel = 1;
        private Map<Byte, GenericAward> dailyAward;

        public int getNeedLevel() {
            return needLevel;
        }

        public void setNeedLevel(int needLevel) {
            this.needLevel = needLevel;
        }

        public Map<Byte, GenericAward> getDailyAward() {
            return dailyAward;
        }

        public void setDailyAward(Map<Byte, GenericAward> dailyAward) {
            this.dailyAward = dailyAward;
        }
    }

    private Map<Byte, WeeklyAward> dailyBonusAward;

    public Map<Byte, WeeklyAward> getDailyBonusAward() {
        return dailyBonusAward;
    }

    public void setDailyBonusAward(Map<Byte, WeeklyAward> dailyBonusAward) {
        WeeklyAward firstWeek = dailyBonusAward.get((byte) 1);
        Objects.requireNonNull(firstWeek, "even first week not configured");
        WEEK_LENGTH = firstWeek.getDailyAward().size();

        this.dailyBonusAward = dailyBonusAward;
    }

    public GenericAward getAwardByWeekAndDay(int week, int dayInWeek) {
        return dailyBonusAward.get((byte) week).dailyAward.get((byte) dayInWeek);
    }

    public GenericAward getAwardByLoginSeq(int loginSequence) {
        Tuple2<Byte, Byte> weekAndDay = toWeekAndDayNumbers(loginSequence);
        return getAwardByWeekAndDay(weekAndDay._1, weekAndDay._2);
    }

    /**
     * Возвращает для данного уровня максимально доступную игроку неделю ежедневных бонусов
     *
     * @param level уровень профиля
     * @return максимальную при этом профиле неделю
     */
    public int getMaxCountedWeek(int level) {
        return dailyBonusAward.entrySet().stream()
                .filter(e -> level >= e.getValue().needLevel)
                .mapToInt(Map.Entry::getKey)
                .max().orElse(1);
    }

    /**
     * Возвращает для данного уровня максимально доступный игроку абсолютный день ежедневных бонусов
     *
     * @param level уровень профиля
     * @return максимальный при этом профиле день ежедневных наград
     */
    public int getMaxCountedLoginSequence(int level) {
        return getMaxCountedWeek(level) * WEEK_LENGTH;
    }

    public WeeklyAward getWeekSetting(int week) {
        return dailyBonusAward.get((byte) week);
    }

    // utils:

    /**
     * По абсолютному дню логина (1..28) определяет номер недели (1..4) и номер дня в пределах этой недели (1..7)
     * <pre><code>
     *     getWeekAndDayNumbers(1) = {1,1}
     *     ...
     *     getWeekAndDayNumbers(7) = {1,7}
     *     getWeekAndDayNumbers(8) = {2,1}
     *     ...
     *     getWeekAndDayNumbers(28) = {4,7}
     * </code></pre>
     * Обратно по действию {@link #toLoginSequence(int, int)}
     *
     * @param loginSequence абсолютный день 1..28 в последовательности логинов
     * @return пару {номер недели, номер дня}
     */
    public Tuple2<Byte, Byte> toWeekAndDayNumbers(int loginSequence) {
        if (loginSequence < 1) {
            throw new IllegalArgumentException("loginSequence should be >= 1, got " + loginSequence);
        }

        int week = (loginSequence - 1) / 7 + 1;
        int dayInWeek = (loginSequence - 1) % 7 + 1;

        return Tuple.of((byte) week, (byte) dayInWeek);
    }

    /**
     * По номеру недели (1..4) и номеру дня в пределах этой недели (1..7) формирует соответствующий им абсолютный день логина ({@link com.pragmatix.app.model.UserProfile#loginSequence})
     *
     * @param week      номер недели 1..4
     * @param dayInWeek день в пределах недели 1..7
     * @return абсолютный день в последовательности логинов
     */
    public byte toLoginSequence(int week, int dayInWeek) {
        if (week < 1) {
            throw new IllegalArgumentException("week number should be >= 1, got " + week);
        }
        if (dayInWeek < 1 || dayInWeek > 7) {
            throw new IllegalArgumentException("day in week number should be in [1..7], got " + dayInWeek);
        }

        int loginSeq = (week - 1) * WEEK_LENGTH + dayInWeek;
        return (byte) loginSeq;
    }

    /**
     * По абсолютному день в последовательности логинов возвращает также абсолютный день, соответствующий началу текущей недели
     *
     * @param loginSequence абсолютный день 1..28 в последовательности логинов
     * @return абсолютный день в начале недели, соответствующей данному значению {@code loginSequence}
     */
    public byte loginSeqToCurWeekStart(int loginSequence) {
        byte week = toWeekAndDayNumbers(loginSequence)._1;
        return toLoginSequence(week, 1);
    }

    public int getWeekLength() {
        return WEEK_LENGTH;
    }
}
