package wormix.services

import java.util.{Calendar, Date}
import javax.annotation.Resource

import com.pragmatix.app.messages.server.{GetDividendResult, OpenDepositResult}
import com.pragmatix.app.messages.structures.DepositStructure
import com.pragmatix.app.model.{AnyMoneyAddition, DepositBean, UserProfile}
import com.pragmatix.app.services.{DepositService, PaymentService}
import com.pragmatix.app.common.MoneyType
import com.pragmatix.gameapp.common.SimpleResultEnum.{ERROR, SUCCESS}
import org.junit.{After, Before, Ignore, Test}
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.{TransactionCallbackWithoutResult, TransactionTemplate}
import wormix.BaseTest

import scala.util.Random

/**
  * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
  *         Created: 19.04.2016 11:38
  */
class DepositServiceTest extends BaseTest {

  @Resource
  var depositService: DepositService = _

  @Resource
  var paymentService: PaymentService = _

  @Resource
  var transactionTemplate: TransactionTemplate = _

  val defaultDepositId = "daily_ruby10"

  var depositBean: DepositBean = _

  val tempResult: AnyMoneyAddition = new GetDividendResult

  @Before
  def before() {
    clearDeposits()
    depositBean = depositService.getDepositBean(defaultDepositId)
  }

  // TODO: заставить снова работать @Transactional
  def clearDeposits(profile: UserProfile = getProfile(testerProfileId)) {
    transactionTemplate.execute(new TransactionCallbackWithoutResult {
      override def doInTransactionWithoutResult(status: TransactionStatus) = {
        depositService.clearDeposits(profile)
      }
    })
  }

  @Test
  def createDepositTest() { // тест на "внутренности" - корректное сохранение и считывание из базы
    val profile = getProfile(testerProfileId)

    clearDeposits()
    depositService.getDepositsFor(profile) shouldBe empty

    openDepositFor(profile, depositBean)
    // проверяем, что правильно создался
    profile.getDeposits.length shouldBe 1
    val deposit = profile.getDeposits.head
    deposit.getProfileId shouldBe profile.getId
    depositService.dividendsFromStr(deposit.getDividendsByDays) shouldEqual depositBean.getDividendsByDays
    deposit.getMoneyType shouldBe MoneyType.REAL_MONEY.getType

    // проверяем, что правильно читается из базы
    softCache.remove(classOf[UserProfile], testerProfileId)
    depositService.getDepositsFor(getProfile(testerProfileId)).length shouldBe 1
  }

  @Test
  def openDepositTest() { // комплексный тест на открытие депозита с клиента и возврат ему информации об уже открытых
    val profile = getProfile(testerProfileId)
    profile setRealMoney 0
    val startDate = new Date

    loginMain()
    withSessionOf(clientMain) {
      openDepositFor(profile, depositBean, startDate)
      val msg = clientMain.receive[OpenDepositResult]
      msg.deposit.dividendsByDays shouldBe depositBean.getDividendsByDays
      msg.deposit.moneyType shouldBe depositBean.getMoneyType
      msg.deposit.startDate shouldBe (startDate.getTime / 1000L)
      msg.firstPart shouldBe depositBean.getImmediateDividend
    }
    profile.getRealMoney shouldEqual depositBean.getImmediateDividend
    disconnectMain()

    loginMain()
    val depositStructures: Array[DepositStructure] = clientMain.enterAccount.deposits
    depositStructures should have size 1
    depositStructures(0).progress shouldBe 1
    depositStructures(0).startDate shouldBe (startDate.getTime / 1000)
    depositStructures(0).dividendsByDays shouldEqual depositBean.getDividendsByDays
    depositStructures(0).moneyType shouldBe MoneyType.REAL_MONEY
    depositStructures(0).todayDividend shouldBe 0 // сегодня уже нечего забирать
  }

  @Test
  def computeDividendTest() { // тест на "внутрености" - корректное вычисление для одного депозита
    val profile = getProfile(testerProfileId)
    val now = Calendar.getInstance()
    now.set(Calendar.HOUR_OF_DAY, 10)

    profile setRealMoney 0
    openDepositFor(profile, depositBean, now.getTime)

    // первую часть должен получить сразу
    profile.getRealMoney shouldBe depositBean.getImmediateDividend
    profile setRealMoney 0

    // забрать сегодня снова проценты по этому же вкладу нельзя
    now.add(Calendar.HOUR_OF_DAY, +5)
    depositService.payDividendTo(profile, now.getTime, tempResult) shouldBe false
    profile.getRealMoney shouldBe 0

    // наступает новый день
    nextDay_!(now)

    // теперь он получает вторую порцию процентов
    depositService.payDividendTo(profile, now.getTime, tempResult) shouldBe true
    profile.getRealMoney shouldBe depositBean.getDividendsByDays()(1)
  }

  @Test
  def takeDividendMultipleTest() { // комплексный тест на забирание процентов с клиента
    val profile = getProfile(testerProfileId)
    val now = Calendar.getInstance()
    now.add(Calendar.DAY_OF_YEAR, -2)   // now = "вчера"
    profile setRealMoney 0

    loginMain()
    withSessionOf(clientMain) {
      openDepositFor(profile, depositBean, now.getTime)
      clientMain.receive[OpenDepositResult]
    }
    profile.getRealMoney shouldBe depositBean.getImmediateDividend
    profile setRealMoney 0

    // забрать сегодня снова проценты по этому же вкладу нельзя
    now.add(Calendar.HOUR_OF_DAY, +5)
    var res = getDividendFor(profile, now)
    res.result shouldBe ERROR
    res.getMoney shouldBe 0
    res.getRealMoney shouldBe 0
    disconnectMain()
    profile.getRealMoney shouldBe 0

    // наступает новый день
    nextDay_!(now) // now = "вчера"

    loginMain()
    // открываем новый вклад
    val newDepositBean = depositService.getDepositBean("daily_ruby105")
    withSessionOf(clientMain) {
      openDepositFor(profile, newDepositBean, now.getTime)
      clientMain.receive[OpenDepositResult]
    }
    profile.getRealMoney shouldBe newDepositBean.getImmediateDividend
    profile setRealMoney 0

    // забирая сегодня проценты, получаем только проценты по старому вкладу
    res = getDividendFor(profile, now)
    res.result shouldBe SUCCESS
    res.getMoney shouldBe 0
    res.getRealMoney shouldBe depositBean.getDividendsByDays()(1)
    disconnectMain()
    profile.getRealMoney shouldEqual res.getRealMoney
    profile setRealMoney 0

    // наступает новый день
    nextDay_!(now) // now = "сейчас"

    loginMain()
    var depositStructures = clientMain.enterAccount.deposits
    // депозиты должны правильно прийти при логине
    depositStructures should have size 2
    depositStructures(0).dividendsByDays shouldBe depositBean.getDividendsByDays
    depositStructures(0).moneyType shouldBe depositBean.getMoneyType
    depositStructures(0).progress shouldBe 2
    depositStructures(0).todayDividend shouldBe depositBean.getDividendsByDays()(2)
    depositStructures(1).dividendsByDays shouldBe newDepositBean.getDividendsByDays
    depositStructures(1).moneyType shouldBe newDepositBean.getMoneyType
    depositStructures(1).progress shouldBe 1
    depositStructures(1).todayDividend shouldBe newDepositBean.getDividendsByDays()(1)

    // теперь уже получаем проценты по обоим
    res = getDividendFor(profile, now)
    res.result shouldBe SUCCESS
    res.getMoney shouldBe 0
    res.getRealMoney shouldBe (depositBean.getDividendsByDays()(2) + newDepositBean.getDividendsByDays()(1))
    profile.getRealMoney shouldEqual res.getRealMoney
    profile setRealMoney 0

    // второй раз снова не получится снять
    res = getDividendFor(profile, now)
    res.result shouldBe ERROR
    res.getMoney shouldBe 0
    res.getRealMoney shouldBe 0
    profile.getRealMoney shouldBe 0

    // наступает новый день
    nextDay_!(now)
    // по прежнему снимаются проценты с двух вкладов
    res = getDividendFor(profile, now)
    res.result shouldBe SUCCESS
    res.getMoney shouldBe 0
    res.getRealMoney shouldBe (depositBean.getDividendsByDays()(3) + newDepositBean.getDividendsByDays()(2))
    profile.getRealMoney shouldEqual res.getRealMoney
    profile setRealMoney 0

    // наконец, на следующий день...
    nextDay_!(now)
    // ...снимаются последние (увеличенные) проценты по первому вкладу
    res = getDividendFor(profile, now)
    res.result shouldBe SUCCESS
    res.getMoney shouldBe 0
    res.getRealMoney shouldBe (depositBean.getDividendsByDays()(4) + newDepositBean.getDividendsByDays()(3))
    profile.getRealMoney shouldEqual res.getRealMoney
    profile setRealMoney 0
    disconnectMain()

    // NB: далее мы исходим из того, что оба вклада длиной в 5 дней
    depositBean.getDividendsByDays.length shouldBe 5
    newDepositBean.getDividendsByDays.length shouldBe 5

    // в итоге, на пятый день...
    nextDay_!(now)
    loginMain()
    // ...у нас должен остаться только более поздний вклад
    depositStructures = clientMain.enterAccount.deposits
    depositStructures should have size 1
    depositStructures(0).dividendsByDays shouldBe newDepositBean.getDividendsByDays
    depositStructures(0).moneyType shouldBe newDepositBean.getMoneyType
    depositStructures(0).progress shouldBe 4
    // и проценты мы заберём только по нему
    res = getDividendFor(profile, now)
    res.result shouldBe SUCCESS
    res.getMoney shouldBe 0
    res.getRealMoney shouldBe newDepositBean.getDividendsByDays()(4)
    profile.getRealMoney shouldEqual res.getRealMoney
    disconnectMain()
    // после чего и он закроется
    profile.getDeposits shouldBe empty
    // и это отразится и в базе данных
    softCache.remove(classOf[UserProfile], testerProfileId)
    depositService.getDepositsFor(getProfile(testerProfileId)) shouldBe empty
  }

  @Test
  def differentCurrencyTest() { // комплексный тест на одновременное открытие вкладов в разных валютах с клиента
    val profile = getProfile(testerProfileId)
    val now = Calendar.getInstance()
    profile setRealMoney 0
    profile setMoney 0

    val fuzyDepositBean = depositService.getDepositBean("daily_fuzy10500")

    loginMain()
    withSessionOf(clientMain) {
      openDepositFor(profile, depositBean, now.getTime)
      val msg = clientMain.receive[OpenDepositResult]
      msg.deposit.moneyType shouldBe MoneyType.REAL_MONEY
      msg.firstPart shouldBe depositBean.getImmediateDividend
    }
    now.add(Calendar.HOUR_OF_DAY, +1)
    withSessionOf(clientMain) {
      openDepositFor(profile, fuzyDepositBean, now.getTime)
      val msg = clientMain.receive[OpenDepositResult]
      msg.deposit.moneyType shouldBe MoneyType.MONEY
      msg.firstPart shouldBe fuzyDepositBean.getImmediateDividend
    }
    profile.getRealMoney shouldBe depositBean.getImmediateDividend
    profile.getMoney shouldBe fuzyDepositBean.getImmediateDividend

    profile setRealMoney 0
    profile setMoney 0

    // наступает новый день
    nextDay_!(now)

    val res = getDividendFor(profile, now)
    res.result shouldBe SUCCESS
    res.getRealMoney shouldBe depositBean.getDividendsByDays()(1)
    res.getMoney shouldBe fuzyDepositBean.getDividendsByDays()(1)

    profile.getRealMoney shouldEqual res.getRealMoney
    profile.getMoney shouldEqual res.getMoney

  }

  @After
  def disconnectIfConnected() {
    if (clientMain != null) {
      disconnectMain()
    }
  }

  private def openDepositFor(profile: UserProfile, depositBean: DepositBean, date: Date = new Date()) = {
    paymentService.openDeposit(profile, depositBean.getTotalValue, 1, Random.alphanumeric.take(24).mkString, date, depositBean.getId)
  }

  private def getDividendFor(profile: UserProfile, now: Calendar): GetDividendResult = {
    val res = new GetDividendResult
    val success = depositService.payDividendTo(profile, now.getTime, res)
    res.result = if (success) SUCCESS else ERROR
    res
  }

  private def nextDay_!(now: Calendar = Calendar.getInstance) = {
    dailyRegistry.getDailyTask.runServiceTask()
    now.add(Calendar.DAY_OF_YEAR, +1)
    now
  }


}
