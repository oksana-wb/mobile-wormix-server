package wormix

import java.util.concurrent.ConcurrentHashMap

import com.pragmatix.pvp.model.BattleBuffer
import org.junit.Test
import org.scalatest.Matchers
import org.scalatest.junit.JUnitSuite

class SimpleTest extends JUnitSuite with Matchers {

  @Test
  def test() {
    val battles = new ConcurrentHashMap[Long, BattleBuffer]()
    val mainServerAddress = ""
    import scala.collection.JavaConverters._
    val inBattle = battles.values().asScala
      .filterNot(_.isFinished)
      .map(_.getParticipants.asScala.count(p => !p.isEnvParticipant && !p.isDroppedFromBattle && p.getMainServer.getDestAddress == mainServerAddress))
      .sum

    "123" shouldBe "123"

  }

}

object SimpleTest {
  val MASTER_AUTH_KEY = "QScsHjh0s4kDYnZH5N7jFazub724zpPi"
}