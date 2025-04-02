package wormix

import com.pragmatix.app.controllers.UserProfileController
import com.pragmatix.app.init.{LevelCreator, UserProfileCreator, WeaponsCreator}
import com.pragmatix.app.messages.client.{Login, LoginByProfileStringId}
import com.pragmatix.app.messages.server.EnterAccount
import com.pragmatix.app.model.UserProfile
import com.pragmatix.app.services._
import com.pragmatix.app.services.rating.{RatingService, RatingServiceImpl}
import com.pragmatix.gameapp.IGameApp
import com.pragmatix.gameapp.cache.SoftCache
import com.pragmatix.gameapp.sessions.{Connection, SessionImpl}
import com.pragmatix.gameapp.social.SocialServiceEnum
import com.pragmatix.serialization.AppBinarySerializer
import com.pragmatix.gameapp.threads.{Execution, ExecutionContext, ExecutionData}
import com.pragmatix.serialization.BinarySerializer
import com.pragmatix.testcase.SocketClientConnection
import com.pragmatix.testcase.handlers.TestcaseSimpleMessageHandler
import org.junit.Before
import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatest.junit.JUnitSuite
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.support.AnnotationConfigContextLoader

import scala.reflect.ClassTag

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(classes = Array(classOf[AppConfiguration]), loader = classOf[AnnotationConfigContextLoader])
class BaseTest extends JUnitSuite with Matchers {

  @Value("${connection.main.port}") val portMain = 0

  @Autowired val gameApp: IGameApp = null
  @Autowired val binarySerializer: AppBinarySerializer = null
  @Autowired val softCache: SoftCache = null
  @Autowired val userProfileCreator: UserProfileCreator = null
  @Autowired val weaponService: WeaponService = null
  @Autowired val weaponCreator: WeaponsCreator = null
  @Autowired val profileExperienceService: ProfileExperienceService = null
  @Autowired val levelCreator: LevelCreator = null
  @Autowired val stuffService: StuffService = null
  @Autowired val profileService: ProfileService = null
  @Autowired val dailyRegistry: DailyRegistry = null
  @Autowired val jdbcTemplate: JdbcTemplate = null
  @Autowired val profileBonusService: ProfileBonusService = null
  @Autowired val daoService: DaoService = null
  @Autowired val ratingService: RatingService = null

  protected var host = "127.0.0.1"
  protected var startServer = true
  protected var clientMain: MainClient = null
  protected var testerProfileId = 58027749L

  @Before
  def init() {
    if(gameApp.getSessionService == null && startServer) {
      gameApp.start()
      System.out.println("Server started...")
    }
  }

  def loginMain(profileId: Long) = new MainClient(binarySerializer, host, portMain).login(profileId, Array[Long]())

  def loginMain() {
    clientMain = loginMain(testerProfileId)
  }

  def disconnectMain() {
    clientMain.disconnect()
    Thread.sleep(1000)
  }

  def getProfile(profileId: Long): UserProfile = {
    profileService.getProfileOrCreate(profileId, Array())._1
  }

  def getProfileOpt(profileId: Long): Option[UserProfile] = {
    Option(profileService.getUserProfile(profileId))
  }

  def wipeProfile(profileId: Long) = {
    userProfileCreator.wipeUserProfile(getProfile(profileId))
  }

  //  @Test
  def test() {
    val person: Person = Person("Вася", "Пупкин")
    person.firstName shouldBe "Вася"
  }

  def doInTransactionWithoutResult(f: => Unit): Unit ={
    daoService.doInTransactionWithoutResult(new Runnable {
      override def run(): Unit = f
    })
  }
  /**
    * Выполняет код operation, как будто он внутри той же сессии, что открыта в MainClient
    *
    * Позволяет вызывать в тестах методы, использующие внутри себя Sessions.getKey()
    */
  protected def withSessionOf(client: MainClient)(operation: => Unit) {
    Execution.EXECUTION.get() match {
      case executionContext: ExecutionData =>
        val oldSession = executionContext.currentSession
        executionContext.currentSession = new SessionImpl( getProfile(client.profileId), client.connection.getSessionId )
        operation
        executionContext.currentSession = oldSession
      case anythingElse => fail(s"No execution context or wrong type: $anythingElse. Failed to fake session")
    }
  }

  protected def setExecutionContext(msgHandler: TestcaseSimpleMessageHandler) {
    val context = new ExecutionContext(gameApp) {
      override def sendMessage(message: Any, connection: Connection) {
        msgHandler.messageReceived(message)
      }
    }
    Execution.EXECUTION.set(context)
  }

}

class MainClient(val binarySerializer: AppBinarySerializer, val host: String, val port: Int) {

  var connection: SocketClientConnection = _
  var enterAccount: EnterAccount = _

  var authKey = SimpleTest.MASTER_AUTH_KEY

  def login(profileId: Long, ids: Array[Long]): MainClient = {
    connection = new SocketClientConnection(binarySerializer).connect(host, port)

    import collection.JavaConverters._
    val message = new Login
    message.socialNet = SocialServiceEnum.vkontakte
    message.id = profileId
    message.ids = ids.map(id => new java.lang.Long(id)).toList.asJava
    message.authKey = authKey

    connection.send(message)
    enterAccount = connection.receive(classOf[EnterAccount], 1500)
    //    enterAccount = connection.receive(classOf[EnterAccount], Int.MaxValue)
    this
  }

  def withAuthKey(authKey: String): MainClient = {
    this.authKey = authKey
    this
  }

  def login(socialId: String, socialNet: SocialServiceEnum = SocialServiceEnum.vkontakte, ids: Array[String] = Array()): MainClient = {
    connection = new SocketClientConnection(binarySerializer).connect(host, port)

    import collection.JavaConverters._
    val message = new LoginByProfileStringId
    message.socialNet = socialNet
    message.id = socialId
    message.ids = ids.toList.asJava
    message.referrerId = ""
    message.authKey = authKey

    connection.send(message)
    enterAccount = connection.receive(classOf[EnterAccount], 1500)
    //    enterAccount = connection.receive(classOf[EnterAccount], Int.MaxValue)
    this
  }

  def send(request: Object): Unit = connection.send(request)

  def request[Response: ClassTag](request: Object): Response = {
    val tClass = implicitly[ClassTag[Response]].runtimeClass
    connection.send(request)
    connection.receive(tClass, 300).asInstanceOf[Response]
  }

  def receive[Response: ClassTag]: Response = {
    val tClass = implicitly[ClassTag[Response]].runtimeClass
    connection.receive(tClass, 300).asInstanceOf[Response]
  }

  def disconnect() {
    connection.disconnect()
  }

  def sessionId = enterAccount.sessionKey

  def profileId = enterAccount.userProfileStructure.id

}

