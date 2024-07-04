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
package com.firstperson;

import com.google.inject.Provides;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.hooks.DrawCallbacks;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.gpu.GpuPlugin;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "First Person",
	description = "Allows for a first-person experience without the ability to interact with anything",
	conflicts = "GPU"
)
public class FirstPersonPlugin extends Plugin implements KeyListener, MouseListener, DrawCallbacks
{
	@Inject
	private Client client;

	@Inject
	private FirstPersonConfig config;

	@Inject
	private Hooks hooks;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private DrawManager drawManager;

	@Inject
	KeyManager keyManager;

	@Inject
	MouseManager mouseManager;

	GpuPlugin gpuPlugin;

	final int[] yAxisAbsoluteChange = PreCalculatedTransformations.yAxisAbsoluteChange;

	final double[] xAndYAxisChangeWithPitch = PreCalculatedTransformations.xAndYAxisChangeWithPitch;

	long lastMillis = 0;
	boolean rightKeyPressed;
	boolean leftKeyPressed;
	boolean upKeyPressed;
	boolean downKeyPressed;
	boolean middleMousePressed;
	int xPosOfMouseDown;
	int yPosOfMouseDown;

	int lastPitch = -1;

	private final Runnable updateFocus = () -> {
		if (client.getCameraMode() != 1) activateFirstPersonCameraMode();
		updateCameraPosition();
	};

	@Inject
	private PluginManager pluginManager;

	@Override
	protected void startUp() throws Exception
	{
		keyManager.registerKeyListener(this);
		mouseManager.registerMouseListener(this);
		lastMillis = System.currentTimeMillis();
		activateFirstPersonCameraMode();

		this.gpuPlugin = new GpuPlugin();
		pluginManager.startPlugin(gpuPlugin);

		drawManager.registerEveryFrameListener(updateFocus);
	}

	@Override
	protected void shutDown() throws Exception
	{
		pluginManager.stopPlugin(gpuPlugin);

		keyManager.unregisterKeyListener(this);
		mouseManager.unregisterMouseListener(this);
		client.setCameraPitchRelaxerEnabled(false);
		client.setCameraMode(0);
		drawManager.unregisterEveryFrameListener(updateFocus);
	}

	private void activateFirstPersonCameraMode()
	{
		client.setCameraMode(1);
		// For some reason this won't turn back on once I've turned it off, resulting in a shifted view when beyond
		// the relaxed limit
		client.setCameraPitchRelaxerEnabled(true);
	}

	private void updateCameraPosition()
	{
		if (client.getCameraMode() != 1 || client.getLocalPlayer() == null) return;

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
				addedYaw = addedYaw - diff * cameraSpeed;
			}

			if (upKeyPressed)
			{
				addedPitch = -diff * cameraSpeed;
			}
			if (downKeyPressed)
			{
				addedPitch = addedYaw - -diff * cameraSpeed;
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

		LocalPoint lp = client.getLocalPlayer().getLocalLocation();
		double playerX = lp.getX();
		double playerY = Perspective.getTileHeight(client, lp, client.getPlane()) - 200.0;
		double playerZ = lp.getY();

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

		int yaw = client.getCameraYawTarget();
		int pitch = client.getCameraPitchTarget();

		double yawRad = Math.toRadians((yaw * 360.0 / 2048.0) - 180.0);

		double distanceAt0Pitch = 750.0;
		int zRate = yAxisAbsoluteChange[pitch];

		double cosPitch = xAndYAxisChangeWithPitch[pitch];

		double xShift = distanceAt0Pitch * cosPitch * Math.sin(yawRad);
		double yShift = distanceAt0Pitch * cosPitch * Math.cos(yawRad);
		double focalPointX = playerX + xShift;
		double focalPointY = playerY - zRate;
		double focalPointZ = playerZ - yShift;

		client.setCameraFocalPointX(focalPointX);
		client.setCameraFocalPointY(focalPointY);
		client.setCameraFocalPointZ(focalPointZ);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("firstperson") && event.getKey().equals("useGpu"))
		{

		}
	}

	@Provides
	FirstPersonConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FirstPersonConfig.class);
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
			case KeyEvent.VK_D:
				e.consume();
				rightKeyPressed = true;
				break;
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_A:
				e.consume();
				leftKeyPressed = true;
				break;
			case KeyEvent.VK_UP:
			case KeyEvent.VK_W:
				e.consume();
				upKeyPressed = true;
				break;
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_S:
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
				e.consume();
				break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_D:
				e.consume();
				rightKeyPressed = false;
				break;
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_A:
				e.consume();
				leftKeyPressed = false;
				break;
			case KeyEvent.VK_UP:
			case KeyEvent.VK_W:
				e.consume();
				upKeyPressed = false;
				break;
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_S:
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

	@Override
	public void draw(Projection projection, Scene scene, Renderable renderable, int orientation, int x, int y, int z, long hash)
	{
		if (projection instanceof IntProjection)
		{
			IntProjection p = (IntProjection) projection;

			int[] firstPersonCamera = firstPersonCameraPosition();

			FirstPersonIntProjection intProjection = new FirstPersonIntProjection(client);
			intProjection.setCameraX(firstPersonCamera[0]);
			intProjection.setCameraY(firstPersonCamera[1]);
			intProjection.setCameraZ(firstPersonCamera[2]);
			intProjection.setYawCos(p.getYawCos());
			intProjection.setYawSin(p.getYawSin());
			intProjection.setPitchCos(p.getPitchCos());
			intProjection.setPitchSin(p.getPitchSin());

			gpuPlugin.draw(intProjection, scene, renderable, orientation, x, y, z, hash);
		}
	}

	@Override
	public void drawScenePaint(Scene scene, SceneTilePaint paint, int plane, int tileX, int tileZ)
	{
		gpuPlugin.drawScenePaint(scene, paint, plane, tileX, tileZ);
	}

	@Override
	public void drawSceneTileModel(Scene scene, SceneTileModel model, int tileX, int tileZ)
	{
		gpuPlugin.drawSceneTileModel(scene, model, tileX, tileZ);
	}

	@Override
	public void draw(int overlayColor)
	{
		gpuPlugin.draw(overlayColor);
	}

	@Override
	public void drawScene(double cameraX, double cameraY, double cameraZ, double cameraPitch, double cameraYaw, int plane)
	{
		int[] firstPersonCamera = firstPersonCameraPosition();
		drawScene(firstPersonCamera[0], firstPersonCamera[1], firstPersonCamera[2], cameraPitch, cameraYaw, plane);
	}

	@Override
	public void postDrawScene()
	{
		gpuPlugin.postDrawScene();
	}

	@Override
	public void animate(Texture texture, int diff)
	{
		gpuPlugin.animate(texture, diff);
	}

	@Override
	public void loadScene(Scene scene)
	{
		gpuPlugin.loadScene(scene);
	}

	@Override
	public void swapScene(Scene scene)
	{
		gpuPlugin.swapScene(scene);
	}

	public int[] firstPersonCameraPosition()
	{
		LocalPoint lp = client.getLocalPlayer().getLocalLocation();
		int cameraX = lp.getX();
		int cameraY = Perspective.getTileHeight(client, lp, client.getTopLevelWorldView().getPlane()) - 200;
		int cameraZ = lp.getY();

		return new int[] { cameraX, cameraY, cameraZ };
	}
}
