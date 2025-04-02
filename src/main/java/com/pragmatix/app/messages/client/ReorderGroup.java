package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;

/**
 * команда изменения очередности ходов членов команды
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 16.08.12 10:48
 *
 * @see com.pragmatix.app.controllers.GroupController#onReorderGroup(ReorderGroup, com.pragmatix.app.model.UserProfile)
 */
@Command(22)
public class ReorderGroup {

    /**
     * id членов команды (включая свой) в новом порядке
     */
    public int[] reorderedWormGroup;

    public ReorderGroup() {
    }

    public ReorderGroup(int[] reorderedWormGroup) {
        this.reorderedWormGroup = reorderedWormGroup;
    }

    @Override
    public String toString() {
        return "ReorderGroup{" +
                "reorderedWormGroup=" + Arrays.toString(reorderedWormGroup) +
                '}';
    }
}
