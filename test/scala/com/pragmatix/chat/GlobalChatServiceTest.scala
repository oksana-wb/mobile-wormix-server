package com.pragmatix.chat

import com.pragmatix.chat.messages.ChatMessageEvent.ChatMessageEventType
import com.pragmatix.chat.messages._
import org.junit.{Before, Test}
import org.springframework.beans.factory.annotation.Autowired
import wormix.{BaseTest, MainClient}

/**
  *
  * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
  *         Created: 07.10.2016 10:56
  */
class GlobalChatServiceTest extends BaseTest {

  @Autowired val chatService: GlobalChatService = null

  @Before
  def before(): Unit = {
    chatService.validatePostToChat = false
  }

  @Test
  def joinToChatTest(): Unit = {

    val client1 = new MainClient(binarySerializer, host, portMain)
    client1.login(testerProfileId, Array[Long]())

    val client2 = new MainClient(binarySerializer, host, portMain)
    client2.login(testerProfileId - 1, Array[Long]())

    client1.request[JoinToChatResult](new JoinToChat)
    client1.request[ChatMessageEvent](new PostToChat("client1", "Hello from client1"))

    client2.request[JoinToChatResult](new JoinToChat)
    client2.request[ChatMessageEvent](new PostToChat("client2", "Hello from client2"))
    client1.receive[ChatMessageEvent]

    client2.send(new LeaveFromChat)

    client1.request[ChatMessageEvent](new PostToChat("client1", "Hello#2 from client1"))

  }

  @Test
  def persistChatTest(): Unit = {
    val client1 = new MainClient(binarySerializer, host, portMain)
    chatService.validatePostToChat = true
    chatService.setMinChatDelay(0)
    getProfile(testerProfileId).setLastPaymentTime(0)

    client1.login(testerProfileId, Array[Long]())

    client1.request[JoinToChatResult](new JoinToChat)
    for(i <- 1 to 100) {
      client1.request[ChatMessageEvent](new PostToChat("client1", "Hello from client1")).state shouldBe ChatMessageEventType.SUCCESS
    }

  }

}
