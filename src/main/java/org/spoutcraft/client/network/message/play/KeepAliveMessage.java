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
 * Two-way message used to keep alive the session: </p> A. The server sends a random int value B. The client returns the very same message </p> If the server doesn't receive a response within a
 * timeout period, it will terminate the session.
 */
public class KeepAliveMessage extends ChannelMessage {
    private static final Channel REQUIRED_CHANNEL = Channel.UNIVERSE;
    private final int random;

    /**
     * Constructs a new keep alive
     *
     * @param random Random value
     */
    public KeepAliveMessage(int random) {
        super(REQUIRED_CHANNEL);
        this.random = random;
    }

    /**
     * Returns the random value sent by the server.
     *
     * @return The random value
     */
    public int getRandom() {
        return random;
    }
}
