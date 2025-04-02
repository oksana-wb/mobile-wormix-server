package wormix.backpack

import org.junit.Test
import wormix.BaseTest

class BackpackTest extends BaseTest {

  @Test
  def doShootTest() {
    val profileId = 44505L
    val client = loginMain(profileId)

    weaponService.removeOrUpdateWeapon(null, getProfile(profileId), 19, 4, false)

    client.disconnect()
  }

  @Test
  def buyWeaponTest() {
    val profileId = 44505L
    val client = loginMain(profileId)

    weaponService.addOrUpdateWeapon(getProfile(profileId), 19, 5)

    client.disconnect()
  }

}
