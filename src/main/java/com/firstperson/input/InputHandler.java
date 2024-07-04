/*
 * Copyright (c) 2024, Zoinkwiz <https://github.com/Zoinkwiz>
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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.MouseListener;

public class InputHandler implements KeyListener, MouseListener
{
	Client client;
	FirstPersonConfig config;

	boolean rightKeyPressed;
	boolean leftKeyPressed;
	boolean upKeyPressed;
	boolean downKeyPressed;
	boolean middleMousePressed;
	int xPosOfMouseDown;
	int yPosOfMouseDown;

	int lastPitch = -1;

	long lastMillis;

	public InputHandler(Client client, FirstPersonConfig config, long lastMillis)
	{
		this.client = client;
		this.config = config;
		this.lastMillis = lastMillis;
	}

	public void updateCameraPosition()
	{
		if (client.getLocalPlayer() == null) return;

		final long before = lastMillis;
		final long now = System.currentTimeMillis();
		lastMillis = now;
		final long diff = now - before;
		// Assume free camera speed of 1
		double addedYaw = 0;
		double addedPitch = 0;
		double cameraSpeed = config.keyCameraSpeed();

		Point currentMousePos = client.getMouseCanvasPosition();

		if (middleMousePressed)
		{
			if (yPosOfMouseDown != -1 && xPosOfMouseDown != -1)
			{
				addedPitch = currentMousePos.getY() - yPosOfMouseDown;
				addedYaw = currentMousePos.getX() - xPosOfMouseDown;
			}
			xPosOfMouseDown = currentMousePos.getX();
			yPosOfMouseDown = currentMousePos.getY();
		}
		else if (diff < 10000)
		{
			if (rightKeyPressed)
			{
				addedYaw = diff * cameraSpeed;
			}
			if (leftKeyPressed)
			{
				addedYaw += -diff * cameraSpeed;
			}

			if (upKeyPressed)
			{
				addedPitch = -diff * cameraSpeed;
			}
			if (downKeyPressed)
			{
				addedPitch += diff * cameraSpeed;
			}
		}

		if (config.inverseKeys())
		{
			addedPitch *= -1;
		}
		else
		{
			addedYaw *= -1;
		}

		if (addedYaw != 0)
		{
			client.setCameraYawTarget((client.getCameraYawTarget() + (int) addedYaw) % 2048);
		}

		if (addedPitch != 0 && client.getCameraPitchTarget() + addedPitch < 512 && client.getCameraPitchTarget() + addedPitch >= 0)
		{
			int currentPitch = client.getCameraPitch();

			// If we've gone below the current pitch limit, thus the adjusting pitch got stuck, shift back to it
			if (lastPitch == currentPitch && currentPitch >= client.getCameraPitchTarget() && addedPitch < 0)
			{
				client.setCameraPitchTarget(client.getCameraPitch());
			}
			else
			{
				client.setCameraPitchTarget(client.getCameraPitchTarget() + (int) addedPitch);
			}
			lastPitch = currentPitch;
		}
	}

	/*
	 * MouseListener and KeyListener are used due to the jumping of the camera usually between the key-pressed movement before a new Focus Point can be set
	 */
	@Override
	public void keyTyped(KeyEvent e)
	{

	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_RIGHT:
				e.consume();
				rightKeyPressed = true;
				break;
			case KeyEvent.VK_LEFT:
				e.consume();
				leftKeyPressed = true;
				break;
			case KeyEvent.VK_UP:
				e.consume();
				upKeyPressed = true;
				break;
			case KeyEvent.VK_DOWN:
				e.consume();
				downKeyPressed = true;
				break;
			case KeyEvent.VK_E:
			case KeyEvent.VK_R:
			case KeyEvent.VK_F1:
			case KeyEvent.VK_F:
			case KeyEvent.VK_SPACE:
			case KeyEvent.VK_PAGE_UP:
			case KeyEvent.VK_PAGE_DOWN:
			case KeyEvent.VK_ESCAPE:
			case KeyEvent.VK_SHIFT:
			case KeyEvent.VK_D:
			case KeyEvent.VK_A:
			case KeyEvent.VK_W:
			case KeyEvent.VK_S:
				if (!config.useGpu())
				{
					e.consume();
				}
				break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_RIGHT:
				e.consume();
				rightKeyPressed = false;
				break;
			case KeyEvent.VK_LEFT:
				e.consume();
				leftKeyPressed = false;
				break;
			case KeyEvent.VK_UP:
				e.consume();
				upKeyPressed = false;
				break;
			case KeyEvent.VK_DOWN:
				e.consume();
				downKeyPressed = false;
				break;
		}
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent mouseEvent)
	{
		if (mouseEvent.getButton() == MouseEvent.BUTTON2)
		{
			middleMousePressed = true;
			xPosOfMouseDown = mouseEvent.getX();
			yPosOfMouseDown = mouseEvent.getY();
			mouseEvent.consume();
		}
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent mouseEvent)
	{
		if (mouseEvent.getButton() == MouseEvent.BUTTON2)
		{
			middleMousePressed = false;
			xPosOfMouseDown = mouseEvent.getX();
			yPosOfMouseDown = mouseEvent.getY();
			mouseEvent.consume();
		}
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}
}