package com.pragmatix.app.messages.structures;

import com.pragmatix.app.common.TeamMemberType;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.model.group.FriendTeamMember;
import com.pragmatix.app.model.group.MercenaryBean;
import com.pragmatix.app.model.group.MercenaryTeamMember;
import com.pragmatix.app.model.group.SoclanTeamMember;
import com.pragmatix.app.services.RaceService;
import com.pragmatix.arena.coliseum.GladiatorTeamMemberStructure;
import com.pragmatix.arena.mercenaries.messages.MercenariesTeamMember;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Resize;
import com.pragmatix.serialization.annotations.Structure;
import io.vavr.Function2;
import org.apache.commons.lang3.StringUtils;

/**
 * Структура для передачи на клиент червя
 */
@Structure
public class WormStructure {
    /**
     * соклановец: идентификатор на сервере хозяина(того кто его качает)
     * наемник: id конфигурации
     * друг: идентификатор на сервере (профиль склонирован и прокаяивается далее)
     */
    @Resize(TypeSize.UINT32)
    public long ownerId;
    /**
     * броня червя
     */
    public byte armor;
    /**
     * атака червя, увеличивает силу удара
     */
    public byte attack;
    /**
     * уровень червя
     */
    public byte level;
    /**
     * опыт червя
     */
    public int experience;
    /**
     * id шляпы которая на голове
     */
    public short hat;
    /**
     * раса
     */
    public byte race;
    /**
     * скин
     */
    public byte skin;
    /**
     * снаряжение
     */
    public short kit;

    /**
     * Строковый ID
     */
    public String ownerStringId;

    /**
     * Изменённое имя, отображаемое в PVP
     */
    public String name;

    public TeamMemberType teamMemberType;

    public boolean active = true;

    public WormStructure() {
    }

    public WormStructure(TeamMemberType teamMemberType, UserProfile userProfile, Function2<UserProfile, Byte, Byte> skinProducer) {
        this.teamMemberType = teamMemberType;

        this.ownerId = userProfile.getId();
        this.ownerStringId = userProfile.getProfileStringId();
        this.level = (byte) userProfile.getLevel();
        this.race = (byte) userProfile.getRace();
        this.skin = skinProducer.apply(userProfile, this.race);
        this.armor = (byte) userProfile.getArmor();
        this.attack = (byte) userProfile.getAttack();
        this.experience = userProfile.getExperience();
        this.hat = userProfile.getHat();
        this.kit = userProfile.getKit();
        this.name = userProfile.getName();
    }

    public WormStructure(TeamMemberType teamMemberType, UserProfile soclanProfile, SoclanTeamMember soclanTeamMember, Function2<UserProfile, Byte, Byte> skinProducer) {
        this(teamMemberType, soclanProfile, skinProducer);
        this.race = RaceService.getRaceExceptExclusive(soclanProfile);
        this.skin = skinProducer.apply(soclanProfile, this.race);
        this.active = soclanTeamMember.isActive();
    }

    public WormStructure(UserProfile friendProfile, FriendTeamMember teamMember) {
        teamMemberType = TeamMemberType.Friend;

        this.ownerId = friendProfile.getId();
        this.ownerStringId = friendProfile.getProfileStringId();
        this.level = (byte) friendProfile.getLevel();
        this.name = friendProfile.getName();

        this.race = teamMember.raceId;
        this.skin = teamMember.skinId;
        this.armor = teamMember.armor;
        this.attack = teamMember.attack;
        this.hat = teamMember.hat;
        this.kit = teamMember.kit;
        this.active = teamMember.isActive();
        this.name = friendProfile.getName();
    }

    public WormStructure(MercenaryBean mercenary, MercenaryTeamMember teamMember) {
        teamMemberType = TeamMemberType.Merchenary;

        this.ownerId = mercenary.id;
        this.level = mercenary.level;

        this.race = mercenary.raceId;
        this.skin = mercenary.skinId;
        this.armor = mercenary.armor;
        this.attack = mercenary.attack;
        this.hat = teamMember.hat > 0 ? teamMember.hat : mercenary.hatId;
        this.kit = teamMember.kit > 0 ? teamMember.kit : mercenary.kitId;
        this.active = teamMember.isActive();
        this.name = teamMember.getName();
    }

    public WormStructure(GladiatorTeamMemberStructure gladiator, byte gladiatorTeamMemberLevel) {
        teamMemberType = TeamMemberType.Gladiator;

        this.ownerId = 0;
        this.level = gladiatorTeamMemberLevel;

        this.race = gladiator.race;
        this.armor = gladiator.armor;
        this.attack = gladiator.attack;
        this.hat = gladiator.hat;
        this.kit = gladiator.kit;
        this.active = true;
    }

    public WormStructure(MercenariesTeamMember member) {
        teamMemberType = TeamMemberType.MerchenaryOther;

        this.ownerId = member.id;
        this.level = member.level;

        this.race = member.race;
        this.armor = member.armor;
        this.attack = member.attack;
        this.hat = member.hat;
        this.kit = member.kit;
        this.active = true;
    }

    public static boolean isActive(WormStructure wormStructure) {
        // double check: в корректном случае wormStructure и так должен быть не .active для неактивного TeamMemberType
        return wormStructure.active && wormStructure.teamMemberType.isActive();
    }

    @Override
    public String toString() {
        return "{" +
                ownerId + ":" + ownerStringId +
                ", " + teamMemberType +
                ", active=" + active +
                ", level=" + level +
                ", race=" + race +
                ", skin=" + skin +
                (StringUtils.isNotEmpty(name) ? ", name=" + name : "") +
//                ", ownerStringId='" + ownerStringId + '\'' +
//                ", armor=" + armor +
//                ", attack=" + attack +
//                ", experience=" + experience +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        WormStructure that = (WormStructure) o;

        if(ownerId != that.ownerId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (ownerId ^ (ownerId >>> 32));
    }

}
