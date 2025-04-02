package wormix.services

import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.annotation.Resource

import com.pragmatix.app.common.{MoneyType, Race, ShopResultEnum, TeamMemberType}
import com.pragmatix.app.controllers.ShopController
import com.pragmatix.app.messages.client._
import com.pragmatix.app.messages.server._
import com.pragmatix.app.messages.structures.ShopItemStructure
import com.pragmatix.app.model.UserProfile
import com.pragmatix.app.model.group.MercenaryTeamMember
import com.pragmatix.app.services.GroupService.MAX_EXTRA_GROUP_SLOTS
import com.pragmatix.app.services.{GroupService, ShopService}
import com.pragmatix.app.settings.{ItemRequirements, RacePriceSettings}
import com.pragmatix.common.utils.AppUtils
import com.pragmatix.gameapp.common.SimpleResultEnum
import org.junit.{After, Test}
import wormix.BaseTest

/**
  *
  * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
  *         Created: 17.09.2015 16:20
  */
class ShopControllerTest extends BaseTest {

  @Resource
  var racePriceSettings: RacePriceSettings = _

  @Resource
  var renamePriceSettings: ItemRequirements = _

  @Resource
  var shopService: ShopService = _

  @Resource
  var shopController: ShopController = _

  @Resource
  var groupService: GroupService = _

  @Test
  def buyComplexWeaponTest(): Unit = {
    val profile = getProfile(testerProfileId)
    profile.setBackpack(Collections.emptyList())
    val weaponId = 37 // Коктейль Молотова maxWeaponLevel="4"
    val price = 40 // 40 рубинов

    profile.setRealMoney(price)
    var shopResult = shopController.onBuyShopItems(new BuyShopItems(new ShopItemStructure(weaponId, -1, 0)), profile)
    println(shopResult)
    profile.getRealMoney shouldBe 0
    profile.getBackpackItemByWeaponId(weaponId).getCount shouldBe -11

    profile.setRealMoney(price * 4)
    shopResult = shopController.onBuyShopItems(new BuyShopItems(new ShopItemStructure(weaponId, 4, 0)), profile)
    println(shopResult)
    profile.getRealMoney shouldBe price
    profile.getBackpackItemByWeaponId(weaponId).getCount shouldBe -14

    shopResult = shopController.onBuyShopItems(new BuyShopItems(new ShopItemStructure(weaponId, -1, 0)), profile)
    println(shopResult)
    profile.getRealMoney shouldBe price
    profile.getBackpackItemByWeaponId(weaponId).getCount shouldBe -14
  }

  @Test
  def buyInfiniteWeaponTest(): Unit = {
    val profile = getProfile(testerProfileId)
    profile.setBackpack(Collections.emptyList())
    val weaponId = 9//Мортира

    profile.setRealMoney(12)
    var shopResult = shopController.onBuyShopItems(new BuyShopItems(new ShopItemStructure(weaponId, -1, 0)), profile)
    println(shopResult)
    profile.getRealMoney shouldBe 0
    profile.getBackpackItemByWeaponId(weaponId).getCount shouldBe -1

    profile.setRealMoney(12)
    shopResult = shopController.onBuyShopItems(new BuyShopItems(new ShopItemStructure(weaponId, -1, 0)), profile)
    println(shopResult)
    profile.getRealMoney shouldBe 12
    profile.getBackpackItemByWeaponId(weaponId).getCount shouldBe -1
  }

  @Test
  def buyConsumableWeaponTest(): Unit = {
    val profile = getProfile(testerProfileId)
    profile.setBackpack(Collections.emptyList())
    val weaponId = 72//Перевязка

    profile.setRealMoney(10)
    var shopResult = shopController.onBuyShopItems(new BuyShopItems(new ShopItemStructure(weaponId, 10, 0)), profile)
    println(shopResult)
    profile.getRealMoney shouldBe 0
    profile.getBackpackItemByWeaponId(weaponId).getCount shouldBe 10

    profile.setRealMoney(10)
    shopResult = shopController.onBuyShopItems(new BuyShopItems(new ShopItemStructure(weaponId, -1, 0)), profile)
    println(shopResult)
    profile.getRealMoney shouldBe 9
    profile.getBackpackItemByWeaponId(weaponId).getCount shouldBe 11
  }

  @Test
  def buySeasonalWeaponTest(): Unit = {
    val profile = getProfile(testerProfileId)
    profile.setBackpack(Collections.emptyList())
    val weaponId = 75//Буран

    profile.setRealMoney(10)
    var shopResult = shopController.onBuyShopItems(new BuyShopItems(new ShopItemStructure(weaponId, 1, 0)), profile)
    println(shopResult)
    profile.getRealMoney shouldBe 10
    profile.getBackpackItemByWeaponId(weaponId) shouldBe null

    shopResult = shopController.onBuyShopItems(new BuyShopItems(new ShopItemStructure(weaponId, -1, 0)), profile)
    println(shopResult)
    profile.getRealMoney shouldBe 10
    profile.getBackpackItemByWeaponId(weaponId) shouldBe null
  }

  @Test
  def mobile_buyReactionRaceTest(): Unit = {
    val profile = getProfile(testerProfileId)
    profile.setLevel(25)
    profile.setRealMoney(1)
    profile.setReactionRate(0)

    val cmd = new BuyReactionRate()
    cmd.reactionRateCount = 3
    val result = shopController.onBuyReactionRate(cmd, profile)

    result.result shouldBe ShopResultEnum.SUCCESS
    profile.getRealMoney shouldBe 0
    profile.getReactionRate shouldBe 3
  }

  @Test
  def mobile_buyRaceTest(): Unit = {
    val profile = getProfile(testerProfileId)
    profile.setLevel(25)
    profile.setRealMoney(16)
    profile.setRace(Race.BOXER)

    var result = shopController.onBuyRace(new BuyRace(Race.RABBIT, MoneyType.REAL_MONEY), profile)

    result.result shouldBe ShopResultEnum.SUCCESS
    profile.getRealMoney shouldBe 0
    profile.getRace shouldBe Race.RABBIT.getType

    result = shopController.onBuyRace(new BuyRace(Race.RABBIT, MoneyType.REAL_MONEY), profile)

    result.result shouldBe ShopResultEnum.ERROR
    profile.getRealMoney shouldBe 0
    profile.getRace shouldBe Race.RABBIT.getType
  }

  @Test
  def selectRaceTwiceTest(): Unit = {
    val profile = getProfile(testerProfileId)
    profile.setRace(Race.BOXER)
    profile.setRaces(Race.setRaces(Race.BOXER, Race.ZOMBIE))
    profile.setSelectRaceTime(0)

    profileService.selectRaceAndSkin(profile, Race.BOXER, 0) shouldBe true
    profile.getSelectRaceTime shouldBe AppUtils.currentTimeSeconds()

    profileService.selectRaceAndSkin(profile, Race.ZOMBIE, 0) shouldBe false
    profileService.getSelectRaceTimeLeft(profile) shouldBe TimeUnit.HOURS.toSeconds(12)
  }

  @Test
  def buySelectRaceTest(): Unit = {
    val profile = getProfile(testerProfileId)
    profile.setRace(Race.BOXER)
    profile.setRaces(Race.setRaces(Race.BOXER, Race.ZOMBIE))
    profile.setSelectRaceTime(0)
    profile.setRealMoney(5)

    profileService.selectRaceAndSkin(profile, Race.BOXER, 0) shouldBe true
    profile.getSelectRaceTime shouldBe AppUtils.currentTimeSeconds()

    shopService.buySelectRace(Race.ZOMBIE, profile) shouldBe ShopResultEnum.SUCCESS
    profile.getRace shouldBe Race.ZOMBIE.getType
    profile.getRealMoney shouldBe 0
  }

  @Test
  def buySelectRaceTest2(): Unit = {
    val profile = getProfile(testerProfileId)
    profile.setRace(Race.BOXER)
    profile.setRaces(Race.setRaces(Race.BOXER, Race.ZOMBIE))
    profile.setSelectRaceTime(0)
    profile.setRealMoney(5)

    profileService.selectRaceAndSkin(profile, Race.BOXER, 0) shouldBe true
    profile.getSelectRaceTime shouldBe AppUtils.currentTimeSeconds()

    profile.setSelectRaceTime(AppUtils.currentTimeSeconds() - TimeUnit.HOURS.toSeconds(25).toInt)

    shopService.buySelectRace(Race.ZOMBIE,  profile) shouldBe ShopResultEnum.ERROR
    profile.getRace shouldBe Race.BOXER.getType
    profile.getRealMoney shouldBe 5
  }

  @Test
  def buyRaceTest(): Unit = {
    val profile = getProfile(testerProfileId)
    profile.setLevel(30)
    profile.setRace(Race.RHINO)
    profile.setRaces(Race.setRaces(Race.BOXER, Race.RHINO))
    profile.setRealMoney(racePriceSettings.getPriceMap.get(Race.CAT.getType).needRealMoney())

    loginMain()

    val response = clientMain.request[BuyRaceResponse](new BuyRace(Race.CAT, MoneyType.REAL_MONEY))
    response.result shouldBe ShopResultEnum.SUCCESS
    response.races shouldBe Race.setRaces(Race.BOXER, Race.RHINO, Race.CAT)
    profile.getRealMoney shouldBe 0
  }

  @Test
  def selectRaceTest(): Unit = {
    val profile = getProfile(testerProfileId)
    profile.setLevel(30)
    profile.setRace(Race.RHINO)
    profile.setRaces(Race.setRaces(Race.BOXER, Race.RHINO))
    profile.setRealMoney(racePriceSettings.getPriceMap.get(Race.CAT.getType).needRealMoney())

    loginMain()

    var response = clientMain.request[SelectRaceResult](new SelectRace(Race.CAT, 0))
    response.result shouldBe SimpleResultEnum.ERROR
    profile.getRace shouldBe Race.RHINO.getType

    response = clientMain.request[SelectRaceResult](new SelectRace(Race.BOXER, 0))
    response.result shouldBe SimpleResultEnum.SUCCESS
    profile.getRace shouldBe Race.BOXER.getType
  }

  @Test
  def buyRenameTest() {
    wipeProfile(testerProfileId)
    val profile = getProfile(testerProfileId)
    profile.setRealMoney(renamePriceSettings.needRealMoney())
    // initially name is not set
    profile.getName shouldBe empty
    // so let's set some
    profile.setName("Initial Name")

    loginMain()
    clientMain.enterAccount
      .userProfileStructure
      .wormsGroup.find { (w) => w.ownerId == testerProfileId }
      .get.name shouldBe "Initial Name"

    // successfully rename with REAL_MONEY
    val response = clientMain.request[BuyRenameResult](new BuyRename(profile.getId.toInt, "Mickey Mouse", MoneyType.REAL_MONEY))
    response.result shouldBe ShopResultEnum.SUCCESS
    response.name shouldBe "Mickey Mouse"
    profile.getName shouldBe "Mickey Mouse"
    profile.getRealMoney shouldBe 0

    // fail to rename again to the same name
    profile.setRealMoney(renamePriceSettings.needRealMoney())
    clientMain.request[BuyRenameResult](new BuyRename(profile.getId.toInt, "Mickey Mouse", MoneyType.REAL_MONEY))
      .result shouldBe ShopResultEnum.ERROR

    // fail to rename again with not enough MONEY
    profile.setMoney(renamePriceSettings.needMoney() - 1)
    clientMain.request[BuyRenameResult](new BuyRename(profile.getId.toInt, "Scooby Doo", MoneyType.MONEY))
      .result shouldBe ShopResultEnum.NOT_ENOUGH_MONEY
    profile.getName shouldBe "Mickey Mouse"

    // successfully rename again with MONEY
    profile.setMoney(renamePriceSettings.needMoney() + 1)
    clientMain.request[BuyRenameResult](new BuyRename(profile.getId.toInt, "Scooby Doo", MoneyType.MONEY))
      .result shouldBe ShopResultEnum.SUCCESS
    profile.getName shouldBe "Scooby Doo"
    profile.getMoney shouldBe 1

    // fail to rename with incorrect name length
    profile.setMoney(renamePriceSettings.needMoney() + 1)
    clientMain.request[BuyRenameResult](new BuyRename(profile.getId.toInt, " X ", MoneyType.MONEY))
      .result shouldBe ShopResultEnum.ERROR
    clientMain.request[BuyRenameResult](new BuyRename(profile.getId.toInt, "LongCat is sooooooooooooooooooooooooooo looooooooooong", MoneyType.MONEY))
      .result shouldBe ShopResultEnum.ERROR
    profile.getName shouldBe "Scooby Doo"
  }

  @Test
  def cannotRenameFriendTest() {
    wipeProfile(testerProfileId)
    val profile = getProfile(testerProfileId)
    profile.setLevel(15)
    profile.setMoney(200)
    val friendId = (testerProfileId - 1).toInt

    loginMain()
    clientMain.request[AddToGroupResult](new AddToGroup(friendId, MoneyType.MONEY, TeamMemberType.Friend))
      .result shouldBe ShopResultEnum.SUCCESS
    // друга уже переименовать не можем!
    profile.setRealMoney(renamePriceSettings.needRealMoney())
    clientMain.request[BuyRenameResult](new BuyRename(friendId, "BadBoy", MoneyType.REAL_MONEY))
      .result shouldBe ShopResultEnum.ERROR
  }

  @Test
  def buySlotAndRenameTest() {
    wipeProfile(testerProfileId)
    val profile = getProfile(testerProfileId)
    profile.setLevel(5)
    profile.setRealMoney(shopService.getExtraGroupSlotRequirements(1).needRealMoney())
    val mercId = -2
    profile.setMoney(groupService.getMercenariesConf.get(mercId).needMoney())

    profile.getExtraGroupSlotsCount shouldBe 0

    loginMain()

    val userProfileStructure = clientMain.enterAccount.userProfileStructure
    userProfileStructure.extraGroupSlotsCount shouldBe 0
    userProfileStructure.wormsGroup.length shouldBe 2 // я и наемник

    // пытаемся добавить нового наёмника - безуспешно
    clientMain.request[AddToGroupResult](new AddToGroup(mercId, MoneyType.MONEY, TeamMemberType.Merchenary))
      .result shouldBe ShopResultEnum.MIN_REQUIREMENTS_ERROR

    // тогда сначала покупаем слот
    clientMain.request[BuyGroupSlotResult](new BuyGroupSlot(1, MoneyType.REAL_MONEY))
      .result shouldBe ShopResultEnum.SUCCESS
    profile.getExtraGroupSlotsCount shouldBe 1
    profile.getWormsGroup.length shouldBe 2 // пока не поменялась
    profile.getRealMoney shouldBe 0

    // потом заполняем его наёмником (неактивным)
    clientMain.request[AddToGroupResult](new AddToGroup(mercId, MoneyType.MONEY, TeamMemberType.Merchenary, AddToGroup.NO_PREV_TEAM_MEMBER, false))
      .result shouldBe ShopResultEnum.SUCCESS
    profile.getWormsGroup.length shouldBe 3
    profile.getWormsGroup()(2) shouldBe mercId
    profile.getTeamMembers()(2) shouldBe a[MercenaryTeamMember]
    profile.getTeamMembers()(2).isActive shouldBe false
    profile.getMoney shouldBe 0

    // и переименовываем
    profile.setRealMoney(renamePriceSettings.needRealMoney())
    val renameResult = clientMain.request[BuyRenameResult](new BuyRename(mercId, "Merc#2", MoneyType.REAL_MONEY))
    renameResult.result shouldBe ShopResultEnum.SUCCESS
    renameResult.name shouldBe "Merc#2"
    profile.getTeamMembers()(2).getName shouldBe "Merc#2"

    disconnectMain()
    // и проверяем, что всё правильно сохраняется и выдаётся
    softCache.remove(classOf[UserProfile], testerProfileId)
    loginMain()
    val newUserProfileStruct = clientMain.enterAccount.userProfileStructure
    newUserProfileStruct.extraGroupSlotsCount shouldBe 1
    newUserProfileStruct.wormsGroup.length shouldBe 3
    newUserProfileStruct.wormsGroup(2).teamMemberType shouldBe TeamMemberType.Merchenary
    newUserProfileStruct.wormsGroup(2).name shouldBe "Merc#2"
    newUserProfileStruct.wormsGroup.count(_.active) should be <= 2
  }

  @Test
  def clearNameTest() {
    wipeProfile(testerProfileId)
    val profile = getProfile(testerProfileId)
    profile.setLevel(5)
    // пусть игрок и его наёмник переименованы
    groupService.tryAddFreeMerchenary(profile, true)
    profileService.setName(profile, "My name")
    val mercId = profile.getWormsGroup()(1)
    profileService.setTeamMemberName(mercId, "Mercenary name", profile)
    profile.getName shouldBe "My name"
    profile.getTeamMembers()(1).getName shouldBe "Mercenary name"

    loginMain()

    // сбрасываем себе
    clientMain.request[ClearNameResult](new ClearName(profile.getProfileId.toInt)).result shouldBe SimpleResultEnum.SUCCESS
    profile.getName shouldBe ""
    profile.getWormStructure(profile.getProfileId.toInt).name shouldBe ""

    // сбрасываем наёмнику
    clientMain.request[ClearNameResult](new ClearName(mercId)).result shouldBe SimpleResultEnum.SUCCESS
    profile.getTeamMembers()(1).getName shouldBe empty
    profile.getWormStructure(mercId).name shouldBe empty
  }

  @Test
  def buyAllPossibleSlotsTest() {
    wipeProfile(testerProfileId)
    val profile = getProfile(testerProfileId)
    profile.setLevel(5)
    profile.setRealMoney(1000)

    loginMain()

    // покупаем все возможные, денег не жалеем
    1 to MAX_EXTRA_GROUP_SLOTS foreach { i =>
      clientMain.request[BuyGroupSlotResult](new BuyGroupSlot(i.toByte, MoneyType.REAL_MONEY))
        .result shouldBe ShopResultEnum.SUCCESS
    }

    // больше MAX_EXTRA_GROUP_SLOTS уже купить не можем ни за какие деньги
    clientMain.request[BuyGroupSlotResult](new BuyGroupSlot((MAX_EXTRA_GROUP_SLOTS + 1).toByte, MoneyType.REAL_MONEY))
      .result shouldBe ShopResultEnum.ERROR
  }

  @After
  def disconnectIfConnected() {
    if(clientMain != null) {
      disconnectMain()
    }
  }
}
