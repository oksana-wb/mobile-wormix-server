package wormix.arena.coliseum

import javax.annotation.Resource

import com.pragmatix.app.settings.GenericAward
import com.pragmatix.arena.coliseum.{ColiseumRewardItem, ColiseumService}
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations = Array("/coliseum-beans.xml"))
class RewardTest {

  @Resource(name = "ColiseumService.rewardMap")
  var rewardMap: java.util.Map[String, java.util.ArrayList[ColiseumRewardItem]] = null

  val service = new ColiseumService()

  @Test
  def test(): Unit = {
    (0 to 10).foreach(i => println("" + i + ": " + mapToGenericAward(rewardMap.get(s"$i"))))
    println(mapToGenericAward(rewardMap.get("10_0")))
  }

  def mapToGenericAward(items: java.util.List[ColiseumRewardItem]): GenericAward = {
    import collection.JavaConverters._
    items.asScala.foldLeft(GenericAward.builder())(service.mapItemToGenericAward).build()
  }

}
