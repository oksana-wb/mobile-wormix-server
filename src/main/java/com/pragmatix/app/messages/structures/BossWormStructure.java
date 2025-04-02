package com.pragmatix.app.messages.structures;

import com.pragmatix.serialization.annotations.Structure;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 03.02.2016 11:56
 *         <p>
 *         Структура для записи порождённых или убитых червей в битве с ботами
 */
@Structure
public class BossWormStructure {
    /**
     * червь в команде игрока или босса?
     */
    public boolean isPlayerTeam;

    /**
     * HP с которым он родился или умер
     */
    public int HP;

    public BossWormStructure() {
    }

    public BossWormStructure(boolean isPlayerTeam, int HP) {
        this.isPlayerTeam = isPlayerTeam;
        this.HP = HP;
    }
}
