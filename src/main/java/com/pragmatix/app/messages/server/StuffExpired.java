package com.pragmatix.app.messages.server;

import com.pragmatix.serialization.annotations.Command;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 14.11.13 11:50
 *
 * @see com.pragmatix.app.services.StuffService#cronTask()
 */
@Command(10093)
public class StuffExpired {

    public List<Short> stuff;

    /**
    * id шляпы которая выбрана
    */
   public short hat;

   /**
    * id артефакта который выбран
    */
   public short kit;

    public StuffExpired() {
    }

    public StuffExpired(List<Short> stuff, short hat, short kit) {
        this.stuff = stuff;
        this.hat = hat;
        this.kit = kit;
    }

    @Override
    public String toString() {
        return "StuffExpired{" +
                "stuff=" + stuff +
                ", hat=" + hat +
                ", kit=" + kit +
                '}';
    }

}
