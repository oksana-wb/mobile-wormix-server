package com.pragmatix.app.messages.structures;

import com.pragmatix.app.common.ConnectionState;
import com.pragmatix.serialization.annotations.Structure;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 23.10.2017 18:05
 */
@Structure
public class Time_ConnectionState {
    public int time;
    public ConnectionState state;

    public Time_ConnectionState() {
    }

    public Time_ConnectionState(int time, ConnectionState state) {
        this.time = time;
        this.state = state;
    }
}
