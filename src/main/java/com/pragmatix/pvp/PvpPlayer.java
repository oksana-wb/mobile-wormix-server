package com.pragmatix.pvp;

import com.pragmatix.pvp.services.PvpService;
import jskills.Guard;
import jskills.IPlayer;
import jskills.ISupportPartialPlay;
import jskills.ISupportPartialUpdate;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 08.04.13 15:55
 */
public class PvpPlayer implements IPlayer, ISupportPartialPlay, ISupportPartialUpdate {

    /**
     * = 100% play time *
     */
    private static final double DefaultPartialPlayPercentage = 1.0;

    /**
     * = receive 100% update *
     */
    private static final double DefaultPartialUpdatePercentage = 1.0;

    /**
     * The identifier for the player, such as a name. *
     */
    private final Long id;

    /**
     * Indicates the percent of the time the player should be weighted where 0.0
     * indicates the player didn't play and 1.0 indicates the player played 100%
     * of the time.
     */
    private final double partialPlayPercentage;

    /**
     * Indicated how much of a skill update a player should receive where 0.0
     * represents no update and 1.0 represents 100% of the update.
     */
    private final double partialUpdatePercentage;

    /**
     * Constructs a player.
     *
     * @param id The identifier for the player, such as a name.
     */
    public PvpPlayer(Long id) {
        this(id, DefaultPartialPlayPercentage, DefaultPartialUpdatePercentage);
    }

    /**
     * Constructs a player.
     *
     * @param id                    The identifier for the player, such as a name.
     * @param partialPlayPercentage The weight percentage to give this player when calculating a new rank.
     */
    public PvpPlayer(Long id, double partialPlayPercentage) {
        this(id, partialPlayPercentage, DefaultPartialUpdatePercentage);
    }

    /**
     * Constructs a player.
     *
     * @param id                      The identifier for the player, such as a name.
     * @param partialPlayPercentage   The weight percentage to give this player when calculating a new rank.
     * @param partialUpdatePercentage Indicates how much of a skill update a player should receive
     *                                where 0 represents no update and 1.0 represents 100% of the
     *                                update.
     */
    public PvpPlayer(Long id, double partialPlayPercentage, double partialUpdatePercentage) {
        // If they don't want to give a player an id, that's ok...
        Guard.argumentInRangeInclusive(partialPlayPercentage, 0, 1.0, "partialPlayPercentage");
        Guard.argumentInRangeInclusive(partialUpdatePercentage, 0, 1.0, "partialUpdatePercentage");
        this.id = id;
        this.partialPlayPercentage = partialPlayPercentage;
        this.partialUpdatePercentage = partialUpdatePercentage;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        PvpPlayer pvpPlayer = (PvpPlayer) o;

        if(!id.equals(pvpPlayer.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return PvpService.formatPvpUserId(id);
    }

    public Long getId() {
        return id;
    }

    public double getPartialPlayPercentage() {
        return partialPlayPercentage;
    }

    public double getPartialUpdatePercentage() {
        return partialUpdatePercentage;
    }
}
