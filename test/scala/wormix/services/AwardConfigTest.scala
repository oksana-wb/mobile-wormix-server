package wormix.services

import javax.annotation.Resource

import com.pragmatix.app.settings.{GenericAward, GenericAwardContainer, GenericAwardContainerImpl, GenericAwardFactory}
import com.pragmatix.craft.services.CraftService
import org.junit.runner.RunWith
import org.junit.{Before, BeforeClass, Test}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

/**
  *
  * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
  *         Created: 15.06.2016 10:24
  */
@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("/test-award-beans.xml"))
class AwardConfigTest {

  @Autowired private val genericAwards: java.util.List[GenericAward] = null
  @Autowired private val genericAwardCotainers: java.util.List[GenericAwardContainer] = null
  @Autowired private val genericAwardFactories: java.util.List[GenericAwardFactory] = null

  @Resource
  var awards: GenericAwardContainerImpl = _

  @Resource(name = "quest02_finishAward_Slot1")
  var quest02_finishAward_Slot1: GenericAwardFactory = _

  @Resource(name = "quest02_finishAward_Slot2_3")
  var quest02_finishAward_Slot2_3: GenericAward = _

  @Before
  def init(): Unit = {
    import scala.collection.JavaConversions._
    for(genericAward <- genericAwards) {
//      setAwardItems(genericAward.getAwardItemsStr, genericAward.getAwardItems)
      genericAward.setReagentsMass(CraftService.parseReagentsMassString(genericAward.getReagentsMassStr))
    }
    for(genericAwardCotainer <- genericAwardCotainers) {
      for(genericAward <- genericAwardCotainer.getGenericAwards) {
//        setAwardItems(genericAward.getAwardItemsStr, genericAward.getAwardItems)
        genericAward.setReagentsMass(CraftService.parseReagentsMassString(genericAward.getReagentsMassStr))
      }
    }
    for(genericAwardFactory <- genericAwardFactories) {
      genericAwardFactory.init()
    }
  }

  @Test
  def test() {
    import scala.collection.JavaConversions._
    println(awards.getGenericAwards.mkString("\n"))
  }

}
