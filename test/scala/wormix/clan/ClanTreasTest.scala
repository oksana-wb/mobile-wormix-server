package wormix.clan

import java.util.Date

import com.pragmatix.app.services.social.vkontakte.WormixVkPaymentProcessor
import com.pragmatix.clanserver.domain.{ClanMember, Price, Product}
import com.pragmatix.clanserver.messages.ServiceResult
import com.pragmatix.clanserver.messages.request._
import com.pragmatix.clanserver.services.{ClanRepoImpl, ClanServiceImpl, PriceServiceImpl}
import com.pragmatix.gameapp.social.service.vkontakte.VkPaymentRecord
import org.junit.{Before, Test}
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.transaction.annotation.Transactional
import wormix.{BaseTest, MainClient}

import scala.util.Random

class ClanTreasTest extends BaseTest {

  @Value("${connection.clan.port}") val clanPort = 0
  @Autowired val clanService: ClanServiceImpl = null
  @Autowired val clanRepo: ClanRepoImpl = null
  //  @Autowired val clanInteropService: ClanInteropServiceImpl = null
  @Autowired val priceService: PriceServiceImpl = null
  @Autowired val vkPaymentProcessor: WormixVkPaymentProcessor = null

  @Before
  def before() {
    System.err.println("################ BEFORE ########################")
    priceService.setCreateClanPrice(new Price(Price.CURRENCY_FUSY, 0, Product.CREATE_CLAN, null))
  }

  @Transactional
  def cleanClans(): Unit = {
    jdbcTemplate.update("delete from clan.clan")
    clanRepo.getMemberCache.clear()
  }

  @Test
  def createClan(): Unit = {
    cleanClans()
    val leaderMainClient: MainClient = loginMain(testerProfileId)
    val leaderClanConnection: ClanClient = createClan(leaderMainClient.sessionId, leaderMainClient.profileId)

    leaderClanConnection.disconnect()
    leaderMainClient.disconnect()
  }

  @Test
  def loginToClan(): Unit = {
    val leaderMainClient = loginMain(testerProfileId)
    val leaderClanConnection = login(leaderMainClient.sessionId, leaderMainClient.profileId)
  }

  //  @Test
  def expelFromClan() {
    val leaderMainClient = loginMain(testerProfileId)
    val leaderClanConnection = createClan(leaderMainClient.sessionId, leaderMainClient.profileId)
    val leader = getClanMember(leaderMainClient.profileId).get
    leader.clan.joinRating = 0

    val memberMainClient = loginMain(testerProfileId - 1)
    var memberClanConnection = join(leaderClanConnection.clan.id, memberMainClient.sessionId, memberMainClient.profileId)
    var member = getClanMember(memberMainClient.profileId).get
    var memberProfile = getProfile(memberMainClient.profileId)
    memberProfile.setRealMoney(0)

    donate34(memberMainClient.profileId)
    leader.clan.treas shouldBe 34

    assert(clanService.expelFromClan(new ExpelFromClanRequest(1, memberMainClient.profileId.toInt), leader).isOk)

    memberProfile.getRealMoney shouldBe 26
    leader.clan.treas shouldBe 0
    getClanMember(memberMainClient.profileId) shouldBe None
    memberClanConnection.connection.disconnect()

    //    memberClanConnection = join(leaderClanConnection.clan.id, memberMainClient.sessionKey, memberMainClient.profileId)
    //    member = getClanMember(memberMainClient.profileId).get
    //    member.seasonRating shouldBe 0
    //    member.donation shouldBe 0
    //    member.donationPrevSeason shouldBe 0
    //    member.donationCurrSeason shouldBe 0
    //    member.cashedMedals shouldBe 0
    //
    //    member.seasonRating = 2001
    //    clanService.expelFromClan(new ExpelFromClanRequest(1, memberMainClient.profileId.toInt), leader).serviceResult shouldBe ServiceResult.ERR_NOT_ENOUGH_TREAS

    clanService.onLogout(member, false)
  }

  //  @Test
  def quitClanBackupMember() {
    val leaderMainClient = loginMain(testerProfileId)
    val leaderClanConnection = createClan(leaderMainClient.sessionId, leaderMainClient.profileId)
    getClanMember(leaderMainClient.profileId).get.clan.joinRating = 0

    val memberMainClient = loginMain(testerProfileId - 1)
    var memberClanConnection = join(leaderClanConnection.clan.id, memberMainClient.sessionId, memberMainClient.profileId)
    var member = getClanMember(memberMainClient.profileId).get
    member.seasonRating = 1001
    member.donation = 100
    member.donationPrevSeason = 20
    member.donationCurrSeason = 10
    member.cashedMedals = 1

    assert(clanService.quitClan(new QuitClanRequest(), member).isOk)
    getClanMember(memberMainClient.profileId) shouldBe None
    memberClanConnection.connection.disconnect()

    memberClanConnection = join(leaderClanConnection.clan.id, memberMainClient.sessionId, memberMainClient.profileId)
    member = getClanMember(memberMainClient.profileId).get
    member.seasonRating shouldBe 1001
    member.donation shouldBe 100
    member.donationPrevSeason shouldBe 20
    member.donationCurrSeason shouldBe 10
    member.cashedMedals shouldBe 1

    clanService.onLogout(member, false)
  }

  //    @Test
  def deleteClanWithNoEmptyTreas() {
    loginMain()
    createClan()
    val profile = getProfile(testerProfileId)
    profile.setRealMoney(0)
    val member = getClanMember(testerProfileId).get
    member.clan.treas = 10

    assert(clanService.deleteClan(new DeleteClanRequest(), member).isOk)
    profile.getRealMoney shouldBe 10
  }

  //  @Test
  def cashMedals() {
    loginMain()
    createClan()
    val member = getClanMember(testerProfileId).get
    member.clan.treas = 200
    member.clan.medalPrice = 10
    val profile = getProfile(testerProfileId)
    profile.setRealMoney(0)

    member.seasonRating = 20 * 1000
    clanService.cashMedals(20, member) shouldBe ServiceResult.OK
    member.clan.treas shouldBe 0
    member.cashedMedals shouldBe 20
    member.clan.cashedMedals shouldBe 20
    profile.getRealMoney shouldBe 200

    clanService.cashMedals(10, member) shouldBe ServiceResult.ERR_NOT_ENOUGH_RATING

    member.seasonRating += 20 * 1000
    clanService.cashMedals(10, member) shouldBe ServiceResult.ERR_NOT_ENOUGH_TREAS

    member.clan.medalPrice = 0
    clanService.cashMedals(10, member) shouldBe ServiceResult.ERR_INVALID_STATE

    clanService.onLogout(member, false)
  }

  //  @Test
  def setMedalsCount() {
    loginMain()
    createClan()
    val member = getClanMember(testerProfileId).get

    clanService.changeClanMedalPrice(10, member) shouldBe ServiceResult.ERR_NOT_ENOUGH_TREAS
    clanService.changeClanMedalPrice(0, member) shouldBe ServiceResult.ERR_INVALID_ARGUMENT
    clanService.changeClanMedalPrice(11, member) shouldBe ServiceResult.ERR_INVALID_ARGUMENT

    member.clan.treas = 200
    clanService.changeClanMedalPrice(10, member) shouldBe ServiceResult.OK
    member.clan.medalPrice shouldBe 10

    clanService.changeClanMedalPrice(10, member) shouldBe ServiceResult.ERR_INVALID_STATE

    clanService.onLogout(member, false)
  }

  //  @Test
  def donateVkSuccess() {
    loginMain()
    val profile = getProfile(testerProfileId)
    profile.setRealMoney(0)

    createClan()
    val member = getClanMember(testerProfileId).get
    member.clan.treas shouldBe 0

    donate34()

    member.clan.treas shouldBe 34
    member.donation shouldBe 34
    member.donationCurrSeason shouldBe 34
    member.donationCurrSeasonComeback shouldBe 26
    profile.getRealMoney shouldBe 0

    //    clanService.donate(member.profileId, 1, 10, 10)
    //    member.clan.treas shouldBe 10

    //    record = new VkPaymentRecord()
    //    record.setStatus(VkPaymentRecord.Status.chargeable)
    //    record.setReceiverId(testerProfileId.toInt)
    //    record.setItem("23")
    //    record.setItemPrice(10)
    //    record.setDate(new Date())
    //    record.setOrderId(new Random().nextInt(1000000))
    //
    //    vkPaymentProcessor.handleOrderStatusChange(record)
    //    member.clan.treas shouldBe 34
    //    member.donation shouldBe 34
    //    member.donationCurrSeason shouldBe 26
    //    profile.getRealMoney shouldBe 26

    clanService.onLogout(member, false)
  }

  def donate34(id: Long = testerProfileId): Unit = {
    var record = new VkPaymentRecord()
    record.setStatus(VkPaymentRecord.Status.chargeable.name())
    record.setReceiverId(id.toInt)
    record.setItem("41")
    record.setItemPrice(10)
    record.setDate(new Date())
    record.setOrderId(new Random().nextInt(1000000))

    vkPaymentProcessor.handleOrderStatusChange(record)
  }

  //    @Test
  def modifyClanFromTreas() {
    loginMain()
    val profile = getProfile(testerProfileId)
    profile.setRealMoney(100)

    createClan()
    val member = getClanMember(testerProfileId).get
    clanService.donate(member.profileId, 1, 100, 100) shouldBe ServiceResult.OK

    clanService.expandClan(new ExpandClanRequest(2, true), member)
    member.clan.level shouldBe 2
    member.clan.treas shouldBe 50

    clanService.renameClan(new RenameClanRequest("new name", true), member)
    member.clan.name shouldBe "new name"
    member.clan.treas shouldBe 45

    clanService.changeClanDescription(new ChangeClanDescriptionRequest("new desc", true), member)
    member.clan.description shouldBe "new desc"
    member.clan.treas shouldBe 40

    clanService.changeClanEmblem(new ChangeClanEmblemRequest(Array[Byte](1, 2, 3, 4, 5), true), member)
    member.clan.emblem shouldBe Array[Byte](1, 2, 3, 4, 5)
    member.clan.treas shouldBe 35

    clanService.onLogout(member, false)
  }

  def createClan(sessionId: String = clientMain.sessionId, profileId: Long = testerProfileId) = new ClanClient(binarySerializer, host, clanPort).createClan(sessionId, profileId)

  def join(clanId: Int, sessionId: String = clientMain.sessionId, profileId: Long = testerProfileId) = new ClanClient(binarySerializer, host, clanPort).join(sessionId, profileId, clanId)

  def login(sessionId: String = clientMain.sessionId, profileId: Long = testerProfileId) = new ClanClient(binarySerializer, host, clanPort).login(sessionId, profileId)

  def getClanMember(profileId: Long = testerProfileId): Option[ClanMember] = {
    Option(clanService.getClanMember(1, profileId.toInt))
  }
}


