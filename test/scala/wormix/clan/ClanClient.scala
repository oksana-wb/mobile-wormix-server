package wormix.clan

import com.pragmatix.clanserver.messages.ServiceResult
import com.pragmatix.clanserver.messages.request.{LoginBase, LoginCreateRequest, LoginJoinRequest, LoginRequest}
import com.pragmatix.clanserver.messages.response.{CommonResponse, EnterAccount}
import com.pragmatix.gameapp.social.SocialServiceEnum
import com.pragmatix.serialization.AppBinarySerializer
import com.pragmatix.testcase.SocketClientConnection
import org.junit.Assert

import scala.reflect.ClassTag

/**
  *
  * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
  *         Created: 14.01.2015 19:03
  */
class ClanClient(val binarySerializer: AppBinarySerializer, val host: String, val port: Int) {

  var connection: SocketClientConnection = _
  var enterAccount: EnterAccount = _

  def createClan(sessionId: String, profileId: Long): ClanClient = {
    val createRequest = new LoginCreateRequest
    createRequest.sessionKey = sessionId
    createRequest.socialId = SocialServiceEnum.vkontakte.getType.toShort
    createRequest.profileId = profileId.toInt
    createRequest.socialProfileId = "" + profileId
    createRequest.name = "" + profileId
    createRequest.clanName = "Клан " + profileId
    createRequest.clanEmblem = new Array[Byte](5)

    connectAndSendLogin(createRequest)
  }

  def join(sessionId: String, profileId: Long, clanId: Int): ClanClient = {
    val joinRequest = new LoginJoinRequest
    joinRequest.sessionKey = sessionId
    joinRequest.socialId = SocialServiceEnum.vkontakte.getType.toShort
    joinRequest.profileId = profileId.toInt
    joinRequest.socialProfileId = "" + profileId
    joinRequest.name = "" + profileId
    joinRequest.clanId = clanId

    connectAndSendLogin(joinRequest)
  }

  def login(sessionId: String, profileId: Long): ClanClient = {
    val joinRequest = new LoginRequest
    joinRequest.sessionKey = sessionId
    joinRequest.socialId = SocialServiceEnum.vkontakte.getType.toShort
    joinRequest.profileId = profileId.toInt
    joinRequest.socialProfileId = "" + profileId
    joinRequest.name = "" + profileId

    connectAndSendLogin(joinRequest)
  }

  def connectAndSendLogin(createRequest: LoginBase): ClanClient = {
    connection = new SocketClientConnection(binarySerializer).connect(host, port)
    connection.send(createRequest)
    enterAccount = connection.receive(classOf[EnterAccount], 1000)
    //    enterAccount = connection.receive(classOf[EnterAccount], Int.MaxValue)
    Assert.assertTrue(enterAccount.isOk)
    this
  }

  def request[Response: ClassTag](request: Object): Response = {
    val tClass = implicitly[ClassTag[Response]].runtimeClass
    connection.send(request)
    connection.receive(tClass, 300).asInstanceOf[Response]
  }

  def requestOK[Response: ClassTag](request: Object): Response = {
    val tClass = implicitly[ClassTag[Response]].runtimeClass
    connection.send(request)
    val resp = connection.receive(tClass, 300).asInstanceOf[Response]
    Assert.assertTrue(resp.asInstanceOf[CommonResponse[_]].serviceResult == ServiceResult.OK)
    resp
  }

  def requestError[Response: ClassTag](request: Object): Response = {
    val tClass = implicitly[ClassTag[Response]].runtimeClass
    connection.send(request)
    val resp = connection.receive(tClass, 300).asInstanceOf[Response]
    Assert.assertTrue(resp.asInstanceOf[CommonResponse[_]].serviceResult != ServiceResult.OK)
    resp
  }

  def receive[Response: ClassTag]: Response = {
    val tClass = implicitly[ClassTag[Response]].runtimeClass
    connection.receive(tClass, 300).asInstanceOf[Response]
  }

  def clan = enterAccount.clan

  def disconnect() {
    connection.disconnect()
  }
}
