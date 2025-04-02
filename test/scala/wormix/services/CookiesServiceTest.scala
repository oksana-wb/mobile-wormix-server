package wormix.services

import javax.annotation.Resource

import com.pragmatix.app.dao.CookiesDao
import com.pragmatix.app.domain.CookiesEntity
import com.pragmatix.app.services.{CookiesService, PaymentService}
import org.junit.Test
import org.scalatest.Matchers
import wormix.BaseTest

/**
  *
  * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
  *         Created: 21.11.2016 14:38
  */
class CookiesServiceTest extends BaseTest {

  @Resource
  var cookiesService: CookiesService = _

  @Resource
  var cookiesDao: CookiesDao = _

  @Test
  def persistTest(): Unit ={
    val profile = getProfile(testerProfileId)
    doInTransactionWithoutResult(cookiesDao.deleteById(testerProfileId.toInt))

    profile.setCookiesEntity(null)
    var entity = cookiesService.getCookiesFor(profile)
    println(entity.getValues.toSeq)
    entity.getValues should  equal (Array())

    entity.setValue("1", "1")
    entity.setValue("2", "2")

    doInTransactionWithoutResult(cookiesDao.persist(entity))
    profile.setCookiesEntity(null)
    entity = cookiesService.getCookiesFor(profile)
    println(entity.getValues.toSeq)
    entity.getValues should  equal (Array(1, 1, 2, 2))

    entity.setValue("1", "11")
    entity.setValue("3", "3")

    doInTransactionWithoutResult(cookiesDao.persist(entity))
    profile.setCookiesEntity(null)
    entity = cookiesService.getCookiesFor(profile)
    println(entity.getValues.toSeq)
    entity.getValues should  equal (Array(1, 11, 2, 2, 3, 3))

    entity.setValue("2", "")
    entity.setValue("3", "")

    doInTransactionWithoutResult(cookiesDao.persist(entity))
    profile.setCookiesEntity(null)
    entity = cookiesService.getCookiesFor(profile)
    println(entity.getValues.toSeq)
    entity.getValues should  equal (Array(1, 11))

  }

}

class CookiesEntityTest extends Matchers {

  @Test
  def test(): Unit = {
    val entity = new CookiesEntity(1)
    entity.setValue("1", "1")
    entity.getValues should  equal (Array(1, 1))

    entity.setValue("2", "200")
    entity.getValues should  equal (Array(1, 1, 2, 200))

    entity.setValue("3", "tree")
    entity.getValues should  equal (Array(1, 1, 2, 200, 3 , "tree"))

    entity.setValue("a", "4")
    entity.getValues should  equal (Array(1, 1, 2, 200, 3 , "tree", "a", 4))

    entity.setValue("b", "5")
    entity.getValues should  equal (Array(1, 1, 2, 200, 3 , "tree", "a", 4, "b", 5))

    entity.setValue("11", "11")
    entity.getValues should  equal (Array(1, 1, 2, 200, 3 , "tree", 11, 11, "a", 4, "b", 5))


  }

}