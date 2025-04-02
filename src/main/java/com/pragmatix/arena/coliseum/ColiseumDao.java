package com.pragmatix.arena.coliseum;

import com.pragmatix.common.utils.VarObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.*;

public class ColiseumDao {

    @Resource
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private ColiseumService coliseumService;

    private Set<Short> hats;

    @Resource(name = "ColiseumService.hats")
    public void setHats(List<Short> hats) {
        this.hats = new HashSet<>(hats);
    }

    private Set<Short> kits;

    @Resource(name = "ColiseumService.kits")
    public void setKits(List<Short> kits) {
        this.kits = new HashSet<>(kits);
    }

    private String table = "wormswar.coliseum";

    public ColiseumEntity find(int profileId) {
        Map<String, Object> params = new HashMap<>();
        params.put("profile_id", profileId);
        final VarObject<ColiseumEntity> result = new VarObject<>();
        namedParameterJdbcTemplate.query("select * from " + table + " where profile_id = :profile_id", params, res -> {
            byte[] data = res.getBytes("data");

            DataUnapply dataUnapply = new DataUnapply(this, data).invoke();
            byte win = dataUnapply.getWin();
            byte defeat = dataUnapply.getDefeat();
            byte draw = dataUnapply.getDraw();
            GladiatorTeamMemberStructure[] candidats = dataUnapply.getCandidats();
            GladiatorTeamMemberStructure[] team = dataUnapply.getTeam();

            Date date = res.getDate("start_series");
            result.value = new ColiseumEntity(
                    res.getInt("profile_id"),
                    res.getBoolean("open"),
                    res.getInt("num"),
                    win,
                    defeat,
                    draw,
                    candidats,
                    team,
                    false,
                    false,
                    date != null ? (int) (date.getTime() / 1000L) : 0
            );
        });
        return result.value;
    }

    private boolean validateBoar(GladiatorTeamMemberStructure boar) {
        int sum = boar.armor + boar.attack;
        return (sum > 17*2 && sum <= 21*2 /*уровни гладиаторов были 21->18->20*/) && hats.contains(boar.hat) && kits.contains(boar.kit);
    }

    public boolean persist(ColiseumEntity entity) {
        return persist(entity, false);
    }

    private static void writeTeamMember(ByteBuf buf, GladiatorTeamMemberStructure teamMember) {
        buf.writeByte(teamMember.race);
        buf.writeByte(teamMember.armor);
        buf.writeByte(teamMember.attack);
        buf.writeShort(teamMember.hat);
        buf.writeShort(teamMember.kit);
    }

    public static byte[] serialize(ColiseumEntity entity) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(entity.win);
        buf.writeByte(entity.defeat);
        buf.writeByte(entity.draw);
        if(entity.candidats != null) {
            for(GladiatorTeamMemberStructure candidate : entity.candidats) {
                writeTeamMember(buf, candidate);
            }
        } else {
            buf.writeByte(-1);
        }
        for(GladiatorTeamMemberStructure member : entity.team) {
            if(member != null)
                writeTeamMember(buf, member);
            else
                buf.writeByte(-1);
        }
        return Arrays.copyOf(buf.array(), buf.writerIndex());
    }

    private int execUpdate(ColiseumEntity entity, final String query, boolean inTransaction) {
        final Map<String, Object> params = new HashMap<>();
        params.put("profile_id", entity.profileId);
        params.put("open", entity.open);
        params.put("num", entity.num);
        params.put("start_series", entity.startSeries > 0 ? new Date(entity.startSeries * 1000L) : null);
        params.put("data", serialize(entity));

        if(inTransaction)
            return namedParameterJdbcTemplate.update(query, params);
        else
            return transactionTemplate.execute(transactionStatus -> namedParameterJdbcTemplate.update(query, params));
    }

    public boolean persist(ColiseumEntity entity, boolean inTransaction) {
        if(entity.newly) {
            String query = "insert into " + table + "  (profile_id, open, num, data) values (:profile_id, :open, :num, :data)";
            int result = execUpdate(entity, query, inTransaction);
            entity.dirty = false;
            entity.newly = false;
            return result > 0;
        } else if(entity.dirty) {
            String query = "update " + table + " set open = :open, num = :num, start_series = :start_series, data = :data where profile_id = :profile_id";
            int result = execUpdate(entity, query, inTransaction);
            entity.dirty = false;
            return result > 0;
        } else {
            return false;
        }
    }

    public boolean delete(int profileId) {
        Map<String, Object> params = new HashMap<>();
        params.put("profile_id", profileId);
        return namedParameterJdbcTemplate.update("delete from " + table + " where profile_id = :profile_id", params) > 0;
    }

    public static class DataUnapply {
        private byte[] data;
        private byte win;
        private byte defeat;
        private byte draw;
        private GladiatorTeamMemberStructure[] candidats;
        private GladiatorTeamMemberStructure[] team;

        private ColiseumDao dao;

        public DataUnapply( ColiseumDao dao, byte... data) {
            this.dao = dao;
            this.data = data;
        }

        public byte getWin() {
            return win;
        }

        public byte getDefeat() {
            return defeat;
        }

        public byte getDraw() {
            return draw;
        }

        public GladiatorTeamMemberStructure[] getCandidats() {
            return candidats;
        }

        public GladiatorTeamMemberStructure[] getTeam() {
            return team;
        }

        public DataUnapply invoke() {
            ByteBuf buf = Unpooled.copiedBuffer(data);
            win = buf.readByte();
            defeat = buf.readByte();
            draw = buf.readByte();

            candidats = null;
            GladiatorTeamMemberStructure firstTeamMember = readTeamMember(buf);
            if(firstTeamMember != null) {
                candidats = new GladiatorTeamMemberStructure[]{firstTeamMember, readTeamMember(buf), readTeamMember(buf)};
            }
            team = new GladiatorTeamMemberStructure[]{readTeamMember(buf), readTeamMember(buf), readTeamMember(buf), readTeamMember(buf)};
            return this;
        }

        // участник выбран если race > 0
        private GladiatorTeamMemberStructure readTeamMember(ByteBuf buf) {
            byte race = buf.readByte();
            if(race == 0) {
                //возможно это кабан
                if(buf.readableBytes() < 6) {
                    //уже точно не гладиатор
                    return null;
                } else {
                    buf.markReaderIndex();
                    GladiatorTeamMemberStructure boar = new GladiatorTeamMemberStructure(race, buf.readByte(), buf.readByte(), buf.readShort(), buf.readShort());
                    if(dao.validateBoar(boar)) {
                        return boar;
                    } else {
                        buf.resetReaderIndex();
                        return null;
                    }
                }
            } else if(race < 0)
                return null;
            else
                return new GladiatorTeamMemberStructure(race, buf.readByte(), buf.readByte(), buf.readShort(), buf.readShort());
        }
    }
}
