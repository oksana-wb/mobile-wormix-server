package wormix.app

import com.pragmatix.app.services.social.android.PaymentCheatersBanService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import wormix.BaseTest

/**
  *
  * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
  *         Created: 28.01.2015 16:16
  */
class PaymentCheatersBanServiceTest extends BaseTest {

  @Autowired val paymentCheatersBanService: PaymentCheatersBanService = null

  @Test
  def findPaymentCheatersAndBan() {
    paymentCheatersBanService.requestVoidedPurchasesAndBan()
  }

}
