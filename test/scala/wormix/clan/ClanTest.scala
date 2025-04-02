package wormix.clan

import com.pragmatix.clanserver.domain.Rank
import com.pragmatix.clanserver.messages.event.ChatMessageEvent
import com.pragmatix.clanserver.messages.request._
import com.pragmatix.clanserver.messages.response._
import org.junit.{Before, Test}
import org.springframework.transaction.annotation.Transactional

/**
  *
  * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
  *         Created: 23.03.2016 13:09
  */
class ClanTest extends BaseClanTest {

  @Transactional
  @Before
  override def before(): Unit = {
    super.before()
  }

  @Test
  def setMuteModeTest() {
    val (leaderMainClient, leaderClanConnection, leader, leaderProfile) = createClan(testerProfileId)
    val (memberMainClient, memberClanConnection, member, memberProfile) = join(testerProfileId - 1, leaderClanConnection.clan.id)

    member.muteMode shouldBe false

    val chatMessageText = "some text"
    var postToChatRerp = memberClanConnection.requestOK[PostToChatResponse](new PostToChatRequest(chatMessageText))

    val chatEvent = leaderClanConnection.receive[ChatMessageEvent]
    chatEvent.message.params shouldBe chatMessageText

    var setMuteResp = memberClanConnection.requestError[SetMuteModeResponse](new SetMuteModeRequest(leader.profileId, true))

    setMuteResp = leaderClanConnection.requestError[SetMuteModeResponse](new SetMuteModeRequest(leader.profileId, false))

    setMuteResp = leaderClanConnection.requestError[SetMuteModeResponse](new SetMuteModeRequest(leader.profileId, true))

    setMuteResp = leaderClanConnection.requestOK[SetMuteModeResponse](new SetMuteModeRequest(member.profileId, true))
    member.muteMode shouldBe true

    postToChatRerp = memberClanConnection.requestError[PostToChatResponse](new PostToChatRequest(chatMessageText))
  }

  @Test
  def setExpelModeTest() {
    val (leaderMainClient, leaderClanConnection, leader, leaderProfile) = createClan(testerProfileId)
    val (officerMainClient, officerClanConnection, officer, officerrProfile) = join(testerProfileId - 1, leaderClanConnection.clan.id)
    val (memberMainClient, memberClanConnection, member, memberProfile) = join(testerProfileId - 2, leaderClanConnection.clan.id)

    leaderClanConnection.requestOK[PromoteInRankResponse](new PromoteInRankRequest(officer.socialId, officer.profileId, Rank.OFFICER))

    leaderClanConnection.requestOK[SetExpelPermitResponse](new SetExpelPermitRequest(officer.profileId, false))
    officer.expelPermit shouldBe false

    officerClanConnection.requestError[ExpelFromClanResponse](new ExpelFromClanRequest(member.socialId, member.profileId))

    leaderClanConnection.requestOK[SetExpelPermitResponse](new SetExpelPermitRequest(officer.profileId, true))
    officer.expelPermit shouldBe true

    officerClanConnection.requestOK[ExpelFromClanResponse](new ExpelFromClanRequest(member.socialId, member.profileId))

    leaderClanConnection.requestOK[SetExpelPermitResponse](new SetExpelPermitRequest(officer.profileId, false))
    leaderClanConnection.requestOK[SetMuteModeResponse](new SetMuteModeRequest(officer.profileId, true))
    leaderClanConnection.requestError[PromoteInRankResponse](new PromoteInRankRequest(officer.socialId, officer.profileId, Rank.LEADER))
  }

}
