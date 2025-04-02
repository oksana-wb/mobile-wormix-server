package wormix.services

import java.util.concurrent.TimeUnit
import javax.annotation.Resource

import com.pragmatix.app.common.{Race, TeamMemberType}
import com.pragmatix.app.controllers.ShopController
import com.pragmatix.app.messages.client._
import com.pragmatix.app.messages.server._
import com.pragmatix.app.model.UserProfile
import com.pragmatix.app.model.group.MercenaryTeamMember
import com.pragmatix.app.services.GroupService.MAX_EXTRA_GROUP_SLOTS
import com.pragmatix.app.services.{GroupService, PaymentService, ShopService}
import com.pragmatix.app.settings.{ItemRequirements, RacePriceSettings}
import com.pragmatix.clanserver.utils.Utils
import com.pragmatix.common.utils.AppUtils
import com.pragmatix.app.common.{MoneyType, ShopResultEnum}
import com.pragmatix.testcase.handlers.TestcaseSimpleMessageHandler
import org.junit.{After, Test}
import wormix.BaseTest

import scala.util.Random

/**
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.09.2015 16:20
 */
class PaymentServiceTest extends BaseTest {

  @Resource
  var paymentService: PaymentService = _

  @Test
  def buyVip7Test(): Unit = {
    val profile = getProfile(testerProfileId)
    profile.setUserProfileStructure(null)
    profile.setVipExpiryTime(0)
    val client = loginMain(testerProfileId)
    println(client.enterAccount.userProfileStructure.rentedItems)

    setExecutionContext(new TestcaseSimpleMessageHandler())
    val transactionId = Random.nextString(24)
    paymentService.confirmMobileVip(profile, transactionId, "vip7", System.currentTimeMillis(), Utils.currentTimeInSeconds() + TimeUnit.DAYS.toSeconds(7).toInt)

    println(profile.getUserProfileStructure.rentedItems)

    profile.getVipExpiryTime shouldBe (Utils.currentTimeInSeconds() + TimeUnit.DAYS.toSeconds(7))
    profile.getUserProfileStructure.rentedItems.activeUntil shouldBe profile.getVipExpiryTime
    profile.getUserProfileStructure.rentedItems.getWeapons.length should be > 0

    profile.getUserProfileStructure.rentedItems.activeUntil = Utils.currentTimeInSeconds() - 10
    println(profile.getUserProfileStructure.rentedItems)
    profile.getUserProfileStructure.rentedItems.getWeapons.length shouldBe 0

    paymentService.confirmMobileVip(profile, transactionId, "vip7", System.currentTimeMillis(), Utils.currentTimeInSeconds() + TimeUnit.DAYS.toSeconds(8).toInt)
  }

  @Test
  def buyVip30Test(): Unit = {
    val profile = getProfile(testerProfileId)
    profile.setUserProfileStructure(null)
    profile.setVipExpiryTime(0)
    profile.setRenameVipAct(0)
    val client = loginMain(testerProfileId)
    println(client.enterAccount.userProfileStructure.rentedItems)

    setExecutionContext(new TestcaseSimpleMessageHandler())
    val transactionId = Random.nextString(24)
    paymentService.confirmMobileVip(profile, transactionId, "vip30", System.currentTimeMillis(), Utils.currentTimeInSeconds() + TimeUnit.DAYS.toSeconds(30).toInt)

    println(profile.getUserProfileStructure.rentedItems)

    profile.getVipExpiryTime shouldBe (Utils.currentTimeInSeconds() + TimeUnit.DAYS.toSeconds(30))
    profile.getRenameVipAct shouldBe 2

    profile.setVipExpiryTime((System.currentTimeMillis() / 1000L).toInt - 1)

    client.disconnect()
  }

}

