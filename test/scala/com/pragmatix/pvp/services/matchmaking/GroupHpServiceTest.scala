package com.pragmatix.pvp.services.matchmaking

import com.pragmatix.app.messages.structures.WormStructure
import org.junit.Test
import org.scalatest.FunSuite

/**
  *
  * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
  *         Created: 31.01.2017 13:44
  */
class GroupHpServiceTest extends FunSuite {

  @Test
  def trimUnitsRandomTest(): Unit = {

    def WormStructure(id: Long) = {val ws = new WormStructure(); ws.ownerId = id; ws;}

    val team = Array[WormStructure](WormStructure(1), WormStructure(2), WormStructure(3), WormStructure(4))
    GroupHpService.trimUnitsDropSoclan(4, team).foreach(ws => println(ws.ownerId))

  }

}
