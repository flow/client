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
import com.flowpowered.networking.util.ByteBufUtils;

import io.netty.buffer.ByteBuf;

import org.spoutcraft.client.game.Difficulty;
import org.spoutcraft.client.game.Dimension;
import org.spoutcraft.client.game.GameMode;
import org.spoutcraft.client.game.LevelType;
import org.spoutcraft.client.network.message.play.RespawnMessage;

/**
 * The codec for the respawn message.
 */
public class RespawnCodec implements Codec<RespawnMessage> {
    @Override
    public RespawnMessage decode(ByteBuf buf) throws IOException {
        final Dimension dimension = Dimension.get(buf.readInt());
        final Difficulty difficulty = Difficulty.get(buf.readUnsignedByte());
        final GameMode gameMode = GameMode.get(buf.readUnsignedByte());
        final LevelType levelType = LevelType.get(ByteBufUtils.readUTF8(buf));
        return new RespawnMessage(dimension, difficulty, gameMode, levelType);
    }

    @Override
    public ByteBuf encode(ByteBuf buf, RespawnMessage message) throws IOException {
        throw new IOException("The client should not send a respawn to the Minecraft server!");
    }
}
