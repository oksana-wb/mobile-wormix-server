import com.pragmatix.app.common.TeamMemberType
import com.pragmatix.app.model.UserProfile
import com.pragmatix.app.model.group.TeamMember

/**
  *
  * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
  *         Created: 16.02.2016 11:26
  */
package object wormix {

  implicit class UserProfileExt(profile: UserProfile) {

    def initTeamWithSize(teamSize: Int): UserProfile = {
      profile.setUserProfileStructure(null)
      while (profile.getWormsGroup.nonEmpty) {
        profile.removeFromTeam(profile.getWormsGroup.head)
      }
      profile.addInTeam(profile.getId.intValue(), null)
      (2 to 4).foreach { i =>
        val teamMember = TeamMember.newTeamMember(TeamMemberType.Merchenary, null, null)
        if(i > teamSize)
          teamMember.setActive(false)
        profile.addInTeam(-i, teamMember)
      }
      profile
    }

  }

}
