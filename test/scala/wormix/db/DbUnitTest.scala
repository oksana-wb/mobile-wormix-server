package wormix.db

import javax.annotation.Resource
import javax.persistence.{EntityManager, PersistenceContext}

import com.pragmatix.app.domain.BackpackConfEntity
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional

/**
  *
  * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
  *         Created: 19.05.2016 15:40
  */
@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("/db-beans.xml"))
class DbUnitTest {

  @PersistenceContext
  var em: EntityManager = _

  @Resource
  var jdbcTemplate: JdbcTemplate = _

  @Test
  @Transactional
  @Rollback(false)
  def simpleTest(): Unit = {
    println(em.find(classOf[BackpackConfEntity], 58027749L).getSeasonsBestRank.mkString(","))

    val map = jdbcTemplate.queryForMap("select profile_id, seasons_best_rank from wormswar.backpack_conf where profile_id = 58027749")
    val arr = map.get("seasons_best_rank").asInstanceOf[Array[Byte]]
    println(java.util.Arrays.toString(arr))
  }

}
