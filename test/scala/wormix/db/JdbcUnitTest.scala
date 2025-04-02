package wormix.db

import java.sql.{Date, ResultSet}
import java.time.{LocalDate, LocalDateTime}
import javax.annotation.Resource
import javax.persistence.{EntityManager, PersistenceContext}

import com.pragmatix.testcase.AbstractTest
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.jdbc.core.{JdbcTemplate, RowMapper, SingleColumnRowMapper}
import org.springframework.jdbc.support.JdbcUtils
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.{TransactionCallbackWithoutResult, TransactionTemplate}

/**
  *
  * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
  *         Created: 19.05.2016 15:40
  */
@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("/jdbc-beans.xml"))
class JdbcUnitTest extends AbstractTest {

  @Resource
  var jdbcTemplate: JdbcTemplate = _

  @Resource
  var transactionTemplate: TransactionTemplate = _

  @Test
  def simpleTest(): Unit = {
    val prevSeasonStartDateTime = jdbcTemplate.queryForObject("SELECT max(season) FROM wormswar.season_total", new RowMapper[Date] {
      override def mapRow(rs: ResultSet, rowNum: Int): Date = {
        rs.getDate(1)
      }
    })

    println(prevSeasonStartDateTime)
  }

  def update(query: String): Unit = {
    transactionTemplate.execute(new TransactionCallbackWithoutResult() {
        protected def doInTransactionWithoutResult(transactionStatus: TransactionStatus) {
          jdbcTemplate.update(query)
        }
      })
  }

}
