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
package org.spoutcraft.client.network.codec.play;

import java.io.IOException;

import com.flowpowered.networking.Codec;
import com.flowpowered.networking.MessageHandler;
import com.flowpowered.networking.session.Session;
import io.netty.buffer.ByteBuf;
import org.spoutcraft.client.network.ClientSession;
import org.spoutcraft.client.network.message.ChannelMessage;
import org.spoutcraft.client.network.message.play.PositionLookMessage;

public class PositionLookCodec extends Codec<PositionLookMessage> implements MessageHandler<PositionLookMessage> {
    private static final int OP_CODE = 6;

    public PositionLookCodec() {
        super(PositionLookMessage.class, OP_CODE);
    }

    @Override
    public PositionLookMessage decode(ByteBuf buf) throws IOException {
        final double x = buf.readInt();
        final double y = buf.readInt();
        final double z = buf.readInt();
        final float yaw = buf.readFloat();
        final float pitch = buf.readFloat();
        final boolean onGround = buf.readByte() == 1;
        return new PositionLookMessage(x, y, z, yaw, pitch, onGround);
    }

    @Override
    public ByteBuf encode(ByteBuf buf, PositionLookMessage message) throws IOException {
        buf.writeDouble(message.getX());
        buf.writeDouble(message.getStance());
        buf.writeDouble(message.getY());
        buf.writeDouble(message.getZ());
        buf.writeFloat(message.getYaw());
        buf.writeFloat(message.getPitch());
        buf.writeBoolean(message.isOnGround());
        return buf;
    }

    @Override
    public void handle(Session session, PositionLookMessage message) {
        ((ClientSession) session).getGame().getNetwork().offer(ChannelMessage.Channel.INTERFACE, message);
        ((ClientSession) session).getGame().getNetwork().offer(ChannelMessage.Channel.UNIVERSE, message);
        ((ClientSession) session).getGame().getNetwork().offer(ChannelMessage.Channel.PHYSICS, message);
    }
}
