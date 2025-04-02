package wormix.login

import com.pragmatix.app.messages.client.{LoginByProfileStringId, Ping}
import com.pragmatix.app.messages.server.Pong
import com.pragmatix.gameapp.social.SocialServiceEnum
import com.pragmatix.testcase.HttpClientConnection
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Test
import org.springframework.beans.factory.annotation.Value
import wormix.{BaseTest, MainClient}

import scala.util.Random

class LoginTest extends BaseTest {

  @Value("${myrmart_vk.connection.main.port}") val myrmartVkMainPort = 0

  @Test
  def loginLocal() {
    clientMain = new MainClient(binarySerializer, "127.0.0.1", 6001).login(testerProfileId, Array(623575L))
    println(clientMain.enterAccount)

    clientMain.request[Pong](new Ping())

    clientMain.disconnect()
    Thread.sleep(300)
  }

  @Test
  def loginLocalOK() {
    clientMain = new MainClient(binarySerializer, "127.0.0.1", 6001).login(RandomStringUtils.randomAlphanumeric(16), SocialServiceEnum.odnoklassniki, Array("405526675054"))
    println(clientMain.enterAccount)
    clientMain.disconnect()

    Thread.sleep(1000)
  }

  @Test
  def loginOk() {
    clientMain = new MainClient(binarySerializer, "aurora.rmart.ru", 6030).login("123", SocialServiceEnum.mailru)
    println(clientMain.enterAccount)
    clientMain.enterAccount.loginSequence shouldBe 1
    clientMain.disconnect()
  }

  @Test
  def loginTestVk() {
    clientMain = new MainClient(binarySerializer, "my.rmart.ru", myrmartVkMainPort).login(Random.nextInt(100000), Array(58027749L))
    println(clientMain.enterAccount)
    clientMain.disconnect()
  }

  @Test
  def loginVk() {
    val times = 50
    var success = 0
    var failure = 0
    for(i <- 1 to times) {
      try {
        println(s"[$i] login ...")
        clientMain = new MainClient(binarySerializer, "tesla.rmart.ru", 60101)
          .withAuthKey("YPdcX7EZBsIF3ZyWnz7Bee0J")
          .login(58027749L, Array(58027749L))
        clientMain.request[Pong](new Ping())
        success += 1
        clientMain.connection.getChannel.disconnect()
      } catch {
        case e: Throwable =>
          System.err.println(e.toString)
          failure += 1

      }
      if(times % 10 == 0){
        Thread.sleep(5000)
      } else{
        Thread.sleep(100)
      }
    }
    println(s"$success/$failure of $times")
  }

  @Test
  def loginToStub(): Unit = {
    val connection = new HttpClientConnection("http://127.0.0.1:8096", binarySerializer)

    val message = new LoginByProfileStringId
    message.referrerId = ""
    message.socialNet = SocialServiceEnum.android
    while (true) {
      try {
        connection.send(message)
      } catch {
        case e: Exception => e.printStackTrace()
      }
      Thread.sleep(3000L)
    }
  }

}
