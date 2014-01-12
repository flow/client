/**
 * This file is part of Client, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013 Spoutcraft <http://spoutcraft.org/>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spoutcraft.client.network.message.play;

import org.spoutcraft.client.network.message.ChannelMessage;

/**
 * A server-bound message to inform the server of various client states.
 */
public class ClientStatusMessage extends ChannelMessage {
    private final ClientState state;

    /**
     * Constructs a new client status
     *
     * @param state See {@link org.spoutcraft.client.network.message.play.ClientStatusMessage.ClientState}
     */
    public ClientStatusMessage(ClientState state) {
        this.state = state;
    }

    /**
     * Returns the client's state.
     *
     * @return The client's state
     */
    public ClientState getState() {
        return state;
    }

    /**
     * An enum of the client states.
     */
    public enum ClientState {
        /**
         * This informs the server that the client is ready to login/respawn from death
         */
        RESPAWN(0),
        /**
         * This informs the server that the client is ready to receive stats (snooping)
         */
        REQUEST_STATS(1),
        /**
         * TODO: check this and rephrase
         * This informs the server that the client is opening an inventory achievement
         */
        OPEN_INVENTORY_ACHIEVEMENT(2);
        private final int value;

        private ClientState(int value) {
            this.value = value;
        }

        /**
         * Returns the numerical value associated to the client state.
         *
         * @return The numerical value
         */
        public int value() {
            return value;
        }
    }
}
