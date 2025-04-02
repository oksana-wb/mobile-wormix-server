package wormix.quest

import java.util
import java.util.Date
import javax.annotation.Resource

import com.pragmatix.app.common.{AwardTypeEnum, PvpBattleResult, ShopResultEnum}
import com.pragmatix.app.services.BanService
import com.pragmatix.pvp.PvpParticipant
import com.pragmatix.pvp.dsl.{BossBattle, FriendQuestBossBattle, QuestBossBattle}
import com.pragmatix.pvp.services.matchmaking.BlackListService
import com.pragmatix.pvp.services.matchmaking.lobby.LobbyConf
import com.pragmatix.quest.quest01.Quest01
import com.pragmatix.quest.QuestService
import com.pragmatix.quest.dao.{QuestDao, QuestEntity}
import com.pragmatix.quest.quest02.Quest02
import io.netty.channel.Channel
import org.junit.Test
import wormix.BaseTest

/**
  *
  * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
  *         Created: 15.12.2015 18:09
  */
class QuestServiceTest extends BaseTest {

  @Resource var lobbyConf: LobbyConf = null
  @Resource var blackListService: BlackListService = null
  @Resource var banService: BanService = null

  @Resource var questService: QuestService = null
  @Resource var questDao: QuestDao = null
  @Resource var quest1: Quest01 = null
  @Resource var quest2: Quest02 = null

  var sender: PvpParticipant = null
  var receiver: PvpParticipant = null
  val mainChannelsMap = new util.HashMap[PvpParticipant, Channel]
  var user1Id = 58027749L
  var user2Id = 58027748L

  @Test
  def q1_rewardTest(): Unit = {
    doInTransactionWithoutResult(jdbcTemplate.update("truncate wormswar.quest_progress"))
    val profile = profileService.getUserProfile(testerProfileId)
    profile.setBattlesCount(0)
    profile.setExperience(0)
    profile.setMoney(0)
    val questEntity = questService.getQuestEntity(profile)
    val data = questEntity.q1
    quest1.start(profile, questEntity)

    var result = questService.reward(profile, 1, 1)
    println(result)
    result._1 shouldBe ShopResultEnum.ERROR
    result._2.size() shouldBe 0

    questService.consumeBattleResult(profile, 1, PvpBattleResult.WINNER)
    data.awardSlots(0) should be > 0.toByte
    data.awardSlots.tail.sum shouldBe 0

    (0 to 5).foreach { _ => questService.consumeBattleResult(profile, 1, PvpBattleResult.WINNER) }
    (0 to 3).foreach { i => data.awardSlots(i) should be > 0.toByte }
    println(data.awardSlots.mkString(","))

    val awardBoxId: Int = data.awardSlots(0)
    result = questService.reward(profile, 1, awardBoxId)
    result._1 shouldBe ShopResultEnum.NOT_ENOUGH_MONEY
    result._2.size() shouldBe 0

    profile.setBattlesCount(quest1.getAwardFactory.getAwardsMap.get(awardBoxId).price)

    result = questService.reward(profile, 1, awardBoxId)
    result._1 shouldBe ShopResultEnum.SUCCESS
    result._2.size() should be > 0
    profile.getBattlesCount shouldBe 0
    profile.getMoney should be > 0
    profile.getExperience should be > 0

    println(data.awardSlots.mkString(","))
    questService.consumeBattleResult(profile, 1, PvpBattleResult.WINNER)
    println(data.awardSlots.mkString(","))
  }

  @Test
  def q2_rewardReagentsTest(): Unit = {
    val profile = profileService.getUserProfile(testerProfileId)
    profile.setLevel(25)
    for(i <- 1 to 10) {
      val result = profileBonusService.awardProfile(quest2.getFinishAward, profile, AwardTypeEnum.QUEST_FINISH)
      println(result)
    }
  }

  @Test
  def q2_rewardTest(): Unit = {
    quest2.setEnabled(true)
    val questId = 2

    val profile = profileService.getUserProfile(testerProfileId)
    val questEntity = questService.getQuestEntity(profile)
    val data = questEntity.q2

    profile.setBattlesCount(quest2.getRewardPriceInBattles)
    data.finishedDate = new Date()
    data.rewarded = false
    val result = questService.reward(profile, questId, 0)

    println(result._1 + " " + result)

    result._1 shouldBe ShopResultEnum.SUCCESS
    data.rewarded shouldBe true
    profile.getBattlesCount shouldBe 0

    data.finishedDate = null
    data.rewarded = false
    questService.reward(profile, questId, 0)._2.size() shouldBe 0
    println(result._1)

    data.finishedDate = new Date()
    data.rewarded = true
    questService.reward(profile, questId, 0)._2.size() shouldBe 0
    println(result._1)

    data.rewarded = false
    profile.setBattlesCount(0)
    questService.reward(profile, questId, 0)._2.size() shouldBe 0
    println(result._1)
  }

  @Test
  def quest2Progress(): Unit = {
    val questId = 2
    quest2.setEnabled(true)
    val profile1 = profileService.getUserProfile(user1Id)
    doInTransactionWithoutResult(questService.wipeQuestsState(profile1))
    println(questService.questsProgress(profile1))
  }

  @Test
  def startQuest2Test(): Unit = {
    val questId = 2
    quest2.setEnabled(true)
    val profile1 = profileService.getUserProfile(user1Id)
    val profile2 = profileService.getUserProfile(user2Id)
    turnOffLobbyRestrict()


    val battle = new QuestBossBattle(questId, binarySerializer, mainChannelsMap)
      .startBattle(user1Id, user2Id)
      .winBattle()

    battle.participant1.disconnectFromMain()
    battle.participant2.disconnectFromMain()
  }

  @Test
  def completeQuest2Test(): Unit = {
    val questId = 2
    quest2.setEnabled(true)
    val profile1 = profileService.getUserProfile(user1Id)
    val profile2 = profileService.getUserProfile(user2Id)
    //    turnOffLobbyRestrict()
    lobbyConf.setCheckLastOpponent(true)


    var battle: BossBattle = null
    (1 to 2).foreach(_ => {
      println(questService.questsProgress(profile1))
      println(questService.questsProgress(profile2))

      battle = new QuestBossBattle(questId, binarySerializer, mainChannelsMap)
        .startBattle(user1Id, user2Id)
        .winBattle()

      println(questService.questsProgress(profile1))
      println(questService.questsProgress(profile2))
    })

    battle.participant1.disconnectFromMain()
    battle.participant2.disconnectFromMain()
  }

  @Test
  def doFriendQuest2Test(): Unit = {
    val questId = 2
    quest2.setEnabled(true)
    val profile1 = profileService.getUserProfile(user1Id)
    val profile2 = profileService.getUserProfile(user2Id)
    turnOffLobbyRestrict()

    var battle: BossBattle = null
    (1 to 2).foreach(_ => {
      println(questService.questsProgress(profile1))
      println(questService.questsProgress(profile2))

      battle = new FriendQuestBossBattle(questId, binarySerializer, mainChannelsMap)
        .startBattle(user1Id, user2Id)
        .winBattle()

      println(questService.questsProgress(profile1))
      println(questService.questsProgress(profile2))
    })

    battle.participant1.disconnectFromMain()
    battle.participant2.disconnectFromMain()
  }

  def turnOffLobbyRestrict() {
    lobbyConf.setUseLevelDiffFactor(false)
    lobbyConf.setEnemyLevelRange(30)
    lobbyConf.setHpDiffFactor(100)
    lobbyConf.setMaxHpDiffFactor(100)
    lobbyConf.setBestMatchQuality(0)
    lobbyConf.setSandboxBattlesDelimiter(0)
    lobbyConf.setCheckLastOpponent(false)
    getProfile(user1Id).setLevel(30)
    getProfile(user2Id).setLevel(30)
    //    getProfile(user3Id).setLevel(30)
    //    getProfile(user4Id).setLevel(30)
    banService.getBanList.clear()
    blackListService.getBlackListsForUsers.clear()
  }
}
