/*
 * Copyright (c) 2025, Zoinkwiz <https://github.com/Zoinkwiz>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.firstperson.input;

import com.firstperson.FirstPersonConfig;
import com.firstperson.FirstPersonPlugin;
import java.awt.event.KeyEvent;
import net.runelite.client.config.Keybind;
import net.runelite.client.input.KeyListener;

public class ToggleInputHandler implements KeyListener
{
	public final FirstPersonPlugin firstPersonPlugin;
	public final FirstPersonConfig firstPersonConfig;


	public ToggleInputHandler(FirstPersonPlugin firstPersonPlugin, FirstPersonConfig firstPersonConfig)
	{
		this.firstPersonPlugin = firstPersonPlugin;
		this.firstPersonConfig = firstPersonConfig;
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		Keybind keybind = firstPersonConfig.toggleKeybind();
		if (keybind.matches(e))
		{
			if (firstPersonPlugin.isActive())
			{
				firstPersonPlugin.deactivate();
			}
			else
			{
				firstPersonPlugin.activate();
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}
}
