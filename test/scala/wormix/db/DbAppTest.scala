package wormix.db

import javax.persistence.{EntityManager, EntityManagerFactory}

import org.junit.Test
import org.springframework.context.support.{AbstractApplicationContext, ClassPathXmlApplicationContext}
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.{TransactionCallbackWithoutResult, TransactionTemplate}

/**
  *
  * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
  *         Created: 19.05.2016 15:40
  */
class DbAppTest {

  @Test
  def test(): Unit = {
    val context: AbstractApplicationContext = new ClassPathXmlApplicationContext("/db-beans.xml")

    println(context)

  }

}
