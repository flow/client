/**
 * This file is part of Client, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013-2014 Spoutcraft <http://spoutcraft.org/>
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
package org.spoutcraft.client.network.codec.play;

import java.io.IOException;

import com.flowpowered.networking.Codec;
import com.flowpowered.networking.MessageHandler;
import io.netty.buffer.ByteBuf;
import org.spoutcraft.client.network.ClientSession;
import org.spoutcraft.client.network.message.play.KeepAliveMessage;

/**
 * The codec for the keep alive message. Also handles the keep alive message .
 */
public class KeepAliveCodec implements Codec<KeepAliveMessage>, MessageHandler<ClientSession, KeepAliveMessage> {

    @Override
    public KeepAliveMessage decode(ByteBuf buf) throws IOException {
        return new KeepAliveMessage(buf.readInt());
    }

    @Override
    public ByteBuf encode(ByteBuf buf, KeepAliveMessage message) throws IOException {
        buf.writeInt(message.getRandom());
        return buf;
    }

    @Override
    public void handle(ClientSession session, KeepAliveMessage message) {
        session.send(new KeepAliveMessage(message.getRandom()));
    }
}
