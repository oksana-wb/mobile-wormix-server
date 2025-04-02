package wormix.arena.coliseum

import com.pragmatix.app.common.Race
import com.pragmatix.app.model.UserProfile
import com.pragmatix.arena.coliseum.{ColiseumDao, ColiseumService}
import org.junit.Test
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.springframework.beans.factory.annotation.Autowired
import wormix.BaseTest

class ColiseumServiceTest extends BaseTest {

  @Autowired val service: ColiseumService = null

  @Test
  def testPersist(): Unit = {
    val profile = new UserProfile(2L)
    service.coliseumEntity(profile)

    println(profile.getColiseumEntity)
  }

  @Test
  def candidatsGenTest(): Unit = {
    //    val service = new ColiseumService()
    //    service.setHats(util.Arrays.asList(1.toShort))
    //    service.setKits(util.Arrays.asList(1.toShort))
    val dao = mock(classOf[ColiseumDao])
    when(dao.find(anyInt())).thenReturn(null)
    service.setDao(dao)
    val profile = new UserProfile(1L)

    def print(): Unit = {
      val entity = service.coliseumEntity(profile)
      val candidats = Option(entity.candidats).getOrElse(Array()).map(c => Race.valueOf(c.race))
      val team = entity.team.map(m => Option(m)).collect { case Some(member) => Race.valueOf(member.race) }
      println(s"team: [${team.mkString(", ")}] candidats: [${candidats.mkString(", ")}]")
    }
    print()
    (1 to 4).foreach { _ =>
      service.addTeamMember(profile, 0)
      print()
    }
  }

}
