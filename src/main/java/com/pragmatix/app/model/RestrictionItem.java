package com.pragmatix.app.model;

import com.google.gson.Gson;
import com.pragmatix.app.domain.RestrictionEntity;
import com.pragmatix.common.utils.AppUtils;
import org.apache.commons.lang3.ArrayUtils;

import javax.validation.constraints.Null;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 01.06.2016 16:58
 *         <p>
 * Запрет - наказание в виде частичного ограничения возможностей игры, более мягкий вариант бана
 * http://jira.pragmatix-corp.com/browse/WM-5084
 * @see BanItem - аналог в системе банов
 */
public class RestrictionItem {

    /**
     * id в базе
     */
    private int id;

    /**
     * id профиля
     */
    private long profileId;

    /**
     * какие именно возможности у игрока заблокированы
     * (либо 0xFFFF - если это обычный, полный бан)
     */
    private short blocks = BlockFlag.TOTAL;

    /**
     * дата начала действия ограничения
     */
    private long startDate;

    /**
     * окончание действия ограничения
     */
    private Long endDate;

    /**
     * причина: один из BanType или другой, дополнительный код
     * @see com.pragmatix.app.common.BanType
     */
    private int reason;

    /**
     * история изменений (создание запрета, изменение длительности, снятие), которая будет сохранена в json
     */
    private HistoryItem[] history;

    public static class HistoryItem {

        /**
         * @see Operation
         */
        public byte operation;

        /**
         * дата операции
         */
        public long date;

        /**
         * новая дата окончания, которая была установлена
         */
        public Long newEndDate;

        /**
         * краткая информация об ограничении от админа
         */
        public String note;

        /**
         * логин админа, который наложил ограничение
         */
        public String admin;

        public HistoryItem(Operation operation, String note, String admin, Long newEndDate) {
            this(operation, System.currentTimeMillis(), note, admin, newEndDate);
        }

        public HistoryItem(Operation operation, long date, String note, String admin, Long newEndDate) {
            this.operation = operation.code;
            this.date = date;
            this.note = note;
            this.admin = admin;
            this.newEndDate = newEndDate;
        }

        @Override
        public String toString() {
            return "{" +
                    AppUtils.formatDateInSeconds((int) (date / 1000L)) +
                    " " + Operation.valueOf(operation) +
                    " till " + endDateToString(newEndDate) +
                    " by [" + admin + ']' +
                    " (" + note + ')' +
                    '}';
        }
    }

    private static Gson gson = new Gson();

    // флаги для blocks:
    public enum BlockFlag {
        CHAT        (1 << 0), // блокирует общий чат в бою
        TEAM_CHAT   (1 << 1), // блокирует командный чат в бою
        GLOBAL_CHAT (1 << 2), // блокирует глобальный чат
        RENAME      (1 << 3), // блокирует смену имени
//        PVP_SOFT    (1 << 4), // ограничивает подбор в PVP команда расширения диапазона подбора WidenSearch обрабатывается единожды
//        PVP_HARD    (1 << 5), // ограничивает подбор в PVP по мастерству в 0.8 (игнорируется команда расширения диапазона подбора WidenSearch)
//        PVP_BLOCK   (1 << 6), // блокирует доступ к PVP - аналог blacklist
//        SKILL_FREEZE(1 << 7), // запрет должен фиксировать мастерство игрока на том уровне, какой имелся в момент выдачи запрета
        CLAN_CHAT   (1 << 8), // запрет кланового чата 
//        BOSS      (1 << 3), // блокирует доступ к боссам - если сильно много ломает именно боссов, например
//        TOP       (1 << 4), // игрок не получает рейтинг и не попадает в топ за победы
        // ...
        ;
        public static short TOTAL = (short) 0xFFFF; // блокируется всё, в том числе возможность зайти в игру - аналог бана

        private short flag;

        BlockFlag(int flag) {
            this.flag = (short) flag;
        }

        public short getFlag() {
            return flag;
        }

        public boolean isAppliedTo(short blocks) {
            return (blocks & flag) != 0;
        }

        public boolean isExactly(short blocks) {
            return blocks == flag;
        }

        public short with(BlockFlag another) {
            return (short) (this.flag | another.flag);
        }

        /**
         * Форматирует в красивую строку все ограничения, наложенные на игрока заданным сочетанием флагов
         * @param blocks сочетание флагов
         * @return строку вида "CHAT|BOSS" либо "TOTAL"
         */
        public static String mkString(final short blocks) {
            if (blocks == TOTAL) {
                return "TOTAL";
            } else {
                return Arrays.stream(BlockFlag.values())
                        .filter(f -> f.isAppliedTo(blocks))
                        .map(BlockFlag::name)
                        .collect(Collectors.joining("|"));
            }
        }

        @Override
        public String toString() {
            return name() + "(0x" + Integer.toHexString(flag) + ')';
        }

        public static byte[] expand(short blocks){
            int i = 0;
            BlockFlag[] values = BlockFlag.values();
            byte[] result = new byte[values.length];
            for(BlockFlag blockFlag : values) {
               if(blockFlag.isAppliedTo(blocks)){
                   result[i] = (byte)blockFlag.ordinal();
                   i++;
               }
            }
            return Arrays.copyOf(result, i);
        }
    }

    public enum Operation {
        UNCHANGED(0),
        CREATE(+2),
        PROLONG(+1),
        REDUCE(-1),
        CANCEL(-2),
        ;
        private byte code;

        Operation(int code) {
            this.code = (byte) code;
        }

        @Override
        public String toString() {
            return name() + '(' + code + ')';
        }

        // возвращает соответствующее enum'у значение enum Operation - либо сам код
        public static Serializable valueOf(final Byte code) {
            Optional<Operation> operation = Arrays.stream(values())
                    .filter(v -> v.code == code )
                    .findFirst();
            //noinspection OptionalIsPresent : branches of `if` return different types, this cannot be covered with .orElse
            if (operation.isPresent()) {
                return operation.get();
            } else {
                return code;
            }
        }
    }

    public RestrictionItem(long profileId, short blocks, long startDate, Long endDate, int reason, String note, String admin) {
        this.profileId = profileId;
        this.blocks = blocks;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.history = new HistoryItem[]{new HistoryItem(Operation.CREATE, startDate, note, admin, endDate)};
    }

    public RestrictionItem(RestrictionEntity e) {
        this.id = e.getId();
        this.profileId = e.getProfileId();
        this.blocks = e.getBlocks();
        this.startDate = e.getStartDate().getTime();
        if (e.getEndDate() != null) {
            this.endDate = e.getEndDate().getTime();
        }
        this.reason = e.getReason();
        this.setHistoryFromJson(e.getHistory());
    }

    /**
     * @return является ли этот запрет тотальным, т.е. аналогом бана
     */
    public boolean isTotal() {
        return blocks == BlockFlag.TOTAL;
    }

    /**
     * Заблокирована ли данная возможность этим запретом?
     * @param flag флаг возможности
     * @return true если возможность заблокирована, иначе false
     */
    public boolean isBlocking(BlockFlag flag) {
        return flag.isAppliedTo(blocks);
    }

    /**
     * @return является ли этот запрет истекшим на данный момент
     */
    public boolean isExpired() {
        return !isInfinite() && endDate < System.currentTimeMillis();
    }

    public boolean isInfinite() {
        return isInfinite(endDate);
    }

    public static boolean isInfinite(Long duration) {
        return duration == null || duration == 0;
    }

    /**
     * Проверяет, что новое ограничение содержится в уже существующем
     *
     * Т.е. блокирует на тот же (или меньший) срок те же возможности (или часть) у того же игрока => не привносит нового
     * @param another новое ограничение
     * @return true, если this уже содержит в себе ограничение another
     */
    public boolean includes(RestrictionItem another) {
        return includes(another.profileId, another.blocks, another.getEndDate());
    }

    /**
     * Проверяет, что новое ограничение содержится в уже существующем
     *
     * Т.е. блокирует на тот же (или меньший) срок те же возможности (или часть) у того же игрока => не привносит нового
     * @param profileId игрок нового ограничения
     * @param blocks блокировки нового ограничения
     * @param endDate дата окончания нового ограничения
     * @return true, если this уже содержит в себе ограничение another
     */
    public boolean includes(long profileId, short blocks, Long endDate) {
        return this.profileId == profileId &&
               (this.blocks & blocks) == blocks &&
               (this.endDate == null || (endDate != null && this.endDate > endDate));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getProfileId() {
        return profileId;
    }

    public void setProfileId(long profileId) {
        this.profileId = profileId;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    @Null
    public Long getEndDate() {
        return endDate;
    }

    public String endDateToString() {
        return endDateToString(endDate);
    }

    public static String endDateToString(Long endDate) {
        return isInfinite(endDate) ? "forever" : AppUtils.formatDateInSeconds((int) (endDate / 1000L));
    }

    public void setEndDate(Long endDate) {
        this.endDate = endDate;
    }

    public int getReason() {
        return reason;
    }

    public void setReason(int reason) {
        this.reason = reason;
    }

    public short getBlocks() {
        return blocks;
    }

    public void setBlocks(short blocks) {
        this.blocks = blocks;
    }

    public void setBlocks(BlockFlag block) {
        this.blocks = block.getFlag();
    }

    public void addBlock(BlockFlag block) {
        this.blocks |= block.getFlag();
    }

    public HistoryItem[] getHistory() {
        return history;
    }

    public void setHistory(HistoryItem[] history) {
        this.history = history;
    }

    /**
     * Добавляет в историю изменений новый пункт, сохраняя текущий snapshot состояния (а именно, endDate)
     *
     * NB: необходимо вызывать _после_ применения операции, чтобы сохранился snapshot _после_ операции
     * @param operation     операция
     * @param operationDate дата операции
     * @param note          комментарий админа
     * @param admin         логин админа, изменившего запрет
     */
    public void addHistory(Operation operation, long operationDate, String note, String admin) {
        history = ArrayUtils.add(history, new HistoryItem(operation, operationDate, note, admin, endDate));
    }

    public String getHistoryAsJson() {
        return gson.toJson(history);
    }

    public void setHistoryFromJson(String json) {
        this.history = gson.fromJson(json, HistoryItem[].class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RestrictionItem that = (RestrictionItem) o;

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "RestrictionItem{" +
                "id=" + id +
                ", profileId=" + profileId +
                ", blocks=" + BlockFlag.mkString(blocks) +
                ", startDate=" + AppUtils.formatDateInSeconds((int) (startDate / 1000L)) +
                ", endDate=" + endDateToString() +
                ", reason=" + reason +
                ", history=" + Arrays.toString(history) +
                '}';
    }
}
