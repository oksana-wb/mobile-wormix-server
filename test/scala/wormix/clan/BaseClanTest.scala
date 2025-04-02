package wormix.clan

import com.pragmatix.app.model.UserProfile
import com.pragmatix.clanserver.domain.{ClanMember, Price, Product}
import com.pragmatix.clanserver.services.{ClanRepoImpl, ClanServiceImpl, PriceServiceImpl}
import org.springframework.beans.factory.annotation.{Autowired, Value}
import wormix.{BaseTest, MainClient}

class BaseClanTest extends BaseTest {

  @Value("${connection.clan.port}") val clanPort = 0
  @Autowired val clanService: ClanServiceImpl = null
  @Autowired val clanRepo: ClanRepoImpl = null
  //  @Autowired val clanInteropService: ClanInteropServiceImpl = null
  @Autowired val priceService: PriceServiceImpl = null

  def before() {
    System.err.println("################ BEFORE ########################")
    priceService.setCreateClanPrice(new Price(Price.CURRENCY_FUSY, 0, Product.CREATE_CLAN, null))

    jdbcTemplate.update("delete from clan.clan")
    clanRepo.getMemberCache.clear()
  }

  def createClan(sessionId: String = clientMain.sessionId, profileId: Long = testerProfileId): ClanClient = new ClanClient(binarySerializer, host, clanPort).createClan(sessionId, profileId)

  def join(clanId: Int, sessionId: String = clientMain.sessionId, profileId: Long = testerProfileId): ClanClient = new ClanClient(binarySerializer, host, clanPort).join(sessionId, profileId, clanId)

  def getClanMember(profileId: Long = testerProfileId): Option[ClanMember] = {
    Option(clanService.getClanMember(1, profileId.toInt))
  }

  def createClan(profileId: Long): (MainClient, ClanClient, ClanMember, UserProfile) = {
    val leaderMainClient = loginMain(profileId)
    val leaderClanConnection = createClan(leaderMainClient.sessionId, leaderMainClient.profileId)
    val leader = getClanMember(leaderMainClient.profileId).get
    val leaderProfile = getProfile(leaderMainClient.profileId)
    leader.clan.joinRating = 0
    (leaderMainClient, leaderClanConnection, leader, leaderProfile)
  }

  def join(profileId: Long, clanId: Int): (MainClient, ClanClient, ClanMember, UserProfile) = {
    val memberMainClient = loginMain(profileId)
    val memberClanConnection = join(clanId, memberMainClient.sessionId, memberMainClient.profileId)
    val member = getClanMember(memberMainClient.profileId).get
    val memberProfile = getProfile(memberMainClient.profileId)
    (memberMainClient, memberClanConnection, member, memberProfile)
  }
}


