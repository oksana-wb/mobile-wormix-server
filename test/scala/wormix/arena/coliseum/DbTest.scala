package wormix.arena.coliseum

import com.pragmatix.arena.coliseum.messages.ColiseumStateResponse
import com.pragmatix.arena.coliseum.{ColiseumDao, ColiseumEntity, ColiseumService, GladiatorTeamMemberStructure}
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.{ApplicationContext, ApplicationContextAware}
import wormix.BaseTest

class DbTest extends BaseTest with ApplicationContextAware {

  @Autowired val coliseumDao: ColiseumDao = null
  @Autowired val coliseumService: ColiseumService = null

  var applicationContext: ApplicationContext = null

  @Test
  def testPersist(): Unit = {
    val team: Array[GladiatorTeamMemberStructure] = (1 to 4).map(_ => new GladiatorTeamMemberStructure()).toArray
    val currentChoice = (1 to 3).map(_ => new GladiatorTeamMemberStructure()).toArray

    val entity = new ColiseumEntity(
      1,
      true,
      2,
      3,
      4,
      5,
      currentChoice,
      team,
      false,
      false,
      0
    )

    currentChoice(0).race = 1
    currentChoice(1).race = 2
    currentChoice(2).race = 3
    //    entity.newly = true
    entity.dirty = true

    coliseumDao.persist(entity)

    val result = coliseumDao.find(1)
    println(result)
  }

  @Test
  def testFind(): Unit = {
    val state = coliseumDao.find(36353064)
    val result = new ColiseumStateResponse(state.num, state.open, state.win, state.defeat, state.draw, state.candidats, coliseumService.removeNulls(state.team), 0, 0)
    println(result)
  }

  override def setApplicationContext(applicationContext: ApplicationContext): Unit = this.applicationContext = applicationContext

}
