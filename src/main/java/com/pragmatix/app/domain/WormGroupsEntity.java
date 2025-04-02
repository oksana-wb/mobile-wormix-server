package com.pragmatix.app.domain;

import com.pragmatix.gameapp.common.Identifiable;

import javax.validation.constraints.Null;
import java.util.Arrays;
import java.util.Map;

import static com.pragmatix.app.services.GroupService.MAX_TEAM_MEMBERS;

/**
 * Комманда игрока
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 15.08.12 17:25
 */
public class WormGroupsEntity implements Identifiable<Integer> {

    private int profileId;

    /**
     * Массив id-шников членов команды.
     * Реальная длинна массива определяется количеством не равных 0 элементов и равна количеству заполненных слотов
     */
    private transient int[] teamMembers = new int[MAX_TEAM_MEMBERS];

    public int[] getTeamMembers() {
        return teamMembers;
    }

    public void setTeamMembers(int[] teamMembers) {
        this.teamMembers = teamMembers;
    }

    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    /**
     * Количество _купленных_ дополнительных слотов для TeamMember'ов (0...3)
     */
    private short extraGroupSlotsCount;

    public Short getExtraGroupSlotsCount() {
        return extraGroupSlotsCount;
    }

    public void setExtraGroupSlotsCount(Short extraGroupSlotsCount) {
        if(extraGroupSlotsCount != null) {
            this.extraGroupSlotsCount = extraGroupSlotsCount;
        }
    }

    public int getTeamMember1() {
        return teamMembers[0];
    }

    public void setTeamMember1(int teamMember1) {
        teamMembers[0] = teamMember1;
    }

    public int getTeamMember2() {
        return teamMembers[1];
    }

    public void setTeamMember2(int teamMember2) {
        teamMembers[1] = teamMember2;
    }

    @Null
    public Integer getTeamMember3() {
        return teamMembers[2] != 0 ? teamMembers[2] : null;
    }

    public void setTeamMember3(Integer teamMember3) {
        if(teamMember3 != null) {
            teamMembers[2] = teamMember3;
        }
    }

    @Null
    public Integer getTeamMember4() {
        return teamMembers[3] != 0 ? teamMembers[3] : null;
    }

    public void setTeamMember4(Integer teamMember4) {
        if(teamMember4 != null) {
            teamMembers[3] = teamMember4;
        }
    }

    @Null
    public Integer getTeamMember5() {
        return teamMembers[4] != 0 ? teamMembers[4] : null;
    }

    public void setTeamMember5(Integer teamMember5) {
        if(teamMember5 != null) {
            teamMembers[4] = teamMember5;
        }
    }

    @Null
    public Integer getTeamMember6() {
        return teamMembers[5] != 0 ? teamMembers[5] : null;
    }

    public void setTeamMember6(Integer teamMember6) {
        if(teamMember6 != null) {
            teamMembers[5] = teamMember6;
        }
    }

    @Null
    public Integer getTeamMember7() {
        return teamMembers[6] != 0 ? teamMembers[6] : null;
    }

    public void setTeamMember7(Integer teamMember7) {
        if(teamMember7 != null) {
            teamMembers[6] = teamMember7;
        }
    }

    // сколько у нас реально есть непустых teamMember'ов
    public int getTeamMembersCount() {
       return (int) Arrays.stream(teamMembers).filter(t -> t != 0).count();
    }

    /**
     * Имена (переименованных) членов команды: id -> name
     */
    @Null
    transient private Map<Integer, String> teamMemberNamesMap;

    /**
     * Имена (переименованных) членов команды (в базе хранятся в JSON мапе)
     */
    @Null
    private String teamMemberNamesJson;

    @Null
    public /*JsonObject*/ String getTeamMemberNames() {
        return teamMemberNamesJson;
    }

    public void setTeamMemberNames(@Null /*JsonObject*/ String teamMemberNames) {
        if(teamMemberNames != null) {
            this.teamMemberNamesJson = teamMemberNames;
        }
    }

    @Null
    public String getTeamMemberName(int teamMemberId) {
        if(teamMemberNamesMap == null) {
            return null;
        } else {
            return teamMemberNamesMap.get(teamMemberId);
        }
    }

    public Map<Integer, String> getTeamMemberNamesMap() {
        return teamMemberNamesMap;
    }

    public void setTeamMemberNamesMap(Map<Integer, String> teamMemberNamesMap) {
        this.teamMemberNamesMap = teamMemberNamesMap;
    }

    private byte[] teamMemberMeta1;
    private byte[] teamMemberMeta2;
    private byte[] teamMemberMeta3;
    private byte[] teamMemberMeta4;
    private byte[] teamMemberMeta5;
    private byte[] teamMemberMeta6;
    private byte[] teamMemberMeta7;

    @Null
    public byte[] getTeamMemberMeta1() {
        return teamMemberMeta1;
    }

    public void setTeamMemberMeta1(byte[] reamMemberMeta1) {
        this.teamMemberMeta1 = reamMemberMeta1;
    }

    @Null
    public byte[] getTeamMemberMeta2() {
        return teamMemberMeta2;
    }

    public void setTeamMemberMeta2(byte[] reamMemberMeta2) {
        this.teamMemberMeta2 = reamMemberMeta2;
    }

    @Null
    public byte[] getTeamMemberMeta3() {
        return teamMemberMeta3;
    }

    public void setTeamMemberMeta3(byte[] reamMemberMeta3) {
        this.teamMemberMeta3 = reamMemberMeta3;
    }

    @Null
    public byte[] getTeamMemberMeta4() {
        return teamMemberMeta4;
    }

    public void setTeamMemberMeta4(byte[] reamMemberMeta4) {
        this.teamMemberMeta4 = reamMemberMeta4;
    }

    @Null
    public byte[] getTeamMemberMeta5() {
        return teamMemberMeta5;
    }

    public void setTeamMemberMeta5(byte[] reamMemberMeta5) {
        this.teamMemberMeta5 = reamMemberMeta5;
    }

    @Null
    public byte[] getTeamMemberMeta6() {
        return teamMemberMeta6;
    }

    public void setTeamMemberMeta6(byte[] reamMemberMeta6) {
        this.teamMemberMeta6 = reamMemberMeta6;
    }

    @Null
    public byte[] getTeamMemberMeta7() {
        return teamMemberMeta7;
    }

    public void setTeamMemberMeta7(byte[] reamMemberMeta7) {
        this.teamMemberMeta7 = reamMemberMeta7;
    }

    @Override
    public Integer getId() {
        return profileId;
    }

    public byte[] acceptTeamMemberMeta(int index) {
        switch (index) {
            case 0:
                return teamMemberMeta1;
            case 1:
                return teamMemberMeta2;
            case 2:
                return teamMemberMeta3;
            case 3:
                return teamMemberMeta4;
            case 4:
                return teamMemberMeta5;
            case 5:
                return teamMemberMeta6;
            case 6:
                return teamMemberMeta7;
        }
        return teamMemberMeta1;
    }

    public void setTeamMemberMeta(byte[] teamMemberMeta, int index) {
        switch (index) {
            case 0:
                teamMemberMeta1 = teamMemberMeta;
                break;
            case 1:
                teamMemberMeta2 = teamMemberMeta;
                break;
            case 2:
                teamMemberMeta3 = teamMemberMeta;
                break;
            case 3:
                teamMemberMeta4 = teamMemberMeta;
                break;
            case 4:
                teamMemberMeta5 = teamMemberMeta;
                break;
            case 5:
                teamMemberMeta6 = teamMemberMeta;
                break;
            case 6:
                teamMemberMeta7 = teamMemberMeta;
                break;
        }
    }

}
