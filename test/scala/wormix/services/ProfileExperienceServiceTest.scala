package wormix.services

import java.util

import com.pragmatix.app.model.BackpackItem
import org.junit.Test
import wormix.BaseTest

/**
  *
  * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
  *         Created: 24.04.2015 11:05
  */
class ProfileExperienceServiceTest extends BaseTest {

  @Test
  def onLevelUpAwardItems(): Unit = {
    //<wormix:level level=" 3" nextLevelExp="  70" levelHp="120" maxWormsCount="1" awardItems="39 26:3 29:4 9:4"/>
    val profile = getProfile(testerProfileId)
    profile.setMoney(0)
    profile.setBattlesCount(0)
    profile.setLevel(2)
    val level = levelCreator.getLevels.get(2)
    profile.setExperience(level.getNextLevelExp - 1)

    val backpackItems = new util.ArrayList[BackpackItem]()
    //backpackItems.add(new BackpackItem(39, -1, false))
    profile.setBackpack(backpackItems)
    profileExperienceService.addExperience(profile, 2)

    profile.getLevel shouldBe 3
    profile.getExperience shouldBe 1
    profile.getBattlesCount shouldBe 5
    profile.getMoney shouldBe 150

    for((weaponId, weaponCount) <- Array((39, -1), (26, 3), (29, 4), (9, 4))) {
      profile.getBackpackItemByWeaponId(weaponId) should not be null
      profile.getBackpackItemByWeaponId(weaponId).getCount shouldBe weaponCount
    }
  }

}
