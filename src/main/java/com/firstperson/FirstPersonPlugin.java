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
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "First Person",
	description = "Allows for a first-person experience without the ability to interact with anything"
)
public class FirstPersonPlugin extends Plugin
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

	int[] yAxisAbsoluteChange = PreCalculatedTransformations.yAxisAbsoluteChange;

	double[] xAndYAxisChangeWithPitch = PreCalculatedTransformations.xAndYAxisChangeWithPitch;

	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;

	@Override
	protected void startUp() throws Exception
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			client.setCameraMode(1);
			client.setCameraPitchRelaxerEnabled(true);
		}
		hooks.registerRenderableDrawListener(drawListener);
	}

	@Override
	protected void shutDown() throws Exception
	{
		client.setCameraPitchRelaxerEnabled(false);
		client.setCameraMode(0);
		hooks.unregisterRenderableDrawListener(drawListener);
	}

	boolean shouldDraw(Renderable renderable, boolean drawingUI)
	{
		if (renderable instanceof Player)
		{
			Player player = (Player) renderable;
			Player local = client.getLocalPlayer();

			if (player.getName() == null)
			{
				return true;
			}
			return player != local;
		}

		return true;
	}

	@Subscribe
	public void onClientTick(ClientTick clientTick)
	{
		if (client.getCameraMode() != 1) client.setCameraMode(1);
		updateCameraPosition();
	}

	private void updateCameraPosition()
	{
		if (client.getCameraMode() != 1 || client.getLocalPlayer() == null) return;

		LocalPoint lp = client.getLocalPlayer().getLocalLocation();
		double playerX = lp.getX();
		double playerY = Perspective.getTileHeight(client, lp, client.getPlane()) - 200.0;
		double playerZ = lp.getY();

		// Camera orientation
		int yaw = client.getCameraYaw();
		int pitch = client.getCameraPitch();

		double yawRad = Math.toRadians((yaw * 360.0 / 2048.0) - 180.0);

		double distanceAt0Pitch = 750.0;
		int zRate = yAxisAbsoluteChange[client.getCameraPitch()];

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

	@Provides
	FirstPersonConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FirstPersonConfig.class);
	}
}
