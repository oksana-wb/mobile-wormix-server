package wormix.services

import com.pragmatix.app.messages.client.{BuyShopItems, SellStuff}
import com.pragmatix.app.messages.server.{SellStuffResult, ShopResult}
import com.pragmatix.app.messages.structures.ShopItemStructure
import org.junit.Test
import wormix.BaseTest

/**
  *
  * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
  *         Created: 28.11.2016 13:29
  */
class PurchaseServiceTest extends BaseTest {

  @Test
  def sellStuffTest(): Unit = {
    /*
     <wormix:stuff stuffId="1000" name="Шапка aса                     " price="5800" realPrice=" -" requiredLevel="24" hp="110"/>
     <wormix:stuff stuffId="1003" name="Панамка                       " price=" 800" realPrice=" 8" requiredLevel=" 5" hp=" 20"/>
     <wormix:stuff stuffId="1004" name="Каска                         " price="1300" realPrice="13" requiredLevel=" 6" hp=" 10"/>
     <wormix:stuff stuffId="1006" name="Цилиндр                       " price="   -" realPrice="12" requiredLevel=" 1" hp=" 30"/>
     <wormix:stuff stuffId="1035" name="Пиратская шапка капитана      " price="   -" realPrice=" -" requiredLevel=" 1" hp=" 40" temporal="true"/>
     <wormix:stuff stuffId="2022" name="Моргенштерн                   " price="   -" realPrice=" -" requiredLevel=" 1" hp="  0" kit="true"/>
    */
    val profile = getProfile(testerProfileId)
    profile.setLevel(30)
    val client = loginMain(testerProfileId)

    profile.setStuff(Array())


    // продаем успешно
    var stuffId: Short = 1000

    profile.setMoney(5800)
    profile.setRealMoney(0)

    var request = new BuyShopItems()
    request.items = Array(new ShopItemStructure(1000, 1, 1))
    client.request[ShopResult](request).result shouldBe 0

    client.request[SellStuffResult](new SellStuff(stuffId)).awards.get(0).count shouldBe 1700
    profile.getMoney shouldBe 1700
    profile.getRealMoney shouldBe 0

    // продаем успешно рубиновую шапку
    stuffId = 1006

    profile.setMoney(0)
    profile.setRealMoney(12)

    request = new BuyShopItems()
    request.items = Array(new ShopItemStructure(stuffId, 1, 0))
    client.request[ShopResult](request).result shouldBe 0

    client.request[SellStuffResult](new SellStuff(stuffId)).awards.get(0).count shouldBe 400
    profile.getMoney shouldBe 400
    profile.getRealMoney shouldBe 0


    // продаем то, чего нет или то, что нельзя продать
    profile.setMoney(0)
    profile.setRealMoney(0)
    profile.setStuff(Array())

    client.request[SellStuffResult](new SellStuff(stuffId)).awards.size() shouldBe 0
    client.request[SellStuffResult](new SellStuff(10)).awards.size() shouldBe 0
    client.request[SellStuffResult](new SellStuff(1035)).awards.size() shouldBe 0
    client.request[SellStuffResult](new SellStuff(2022)).awards.size() shouldBe 0

  }

}
