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
package com.firstperson.detachedcamera;

import com.firstperson.FirstPersonConfig;
import com.firstperson.input.InputHandler;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;

public class DetachedCameraMovementHandler
{
	Client client;
	FirstPersonConfig config;
	InputHandler inputHandler;

	final int[] yAxisAbsoluteChange = PreCalculatedTransformations.yAxisAbsoluteChange;

	final double[] xAndYAxisChangeWithPitch = PreCalculatedTransformations.xAndYAxisChangeWithPitch;

	public DetachedCameraMovementHandler(Client client, FirstPersonConfig config, InputHandler inputHandler)
	{
		this.client = client;
		this.config = config;
		this.inputHandler = inputHandler;
	}

	public void updateDetachedCameraPosition()
	{
		if (client.getLocalPlayer() == null) return;
		int yaw = client.getCameraYawTarget();
		int pitch = client.getCameraPitchTarget();

		double yawRad = Math.toRadians((yaw * 360.0 / 2048.0) - 180.0);

		double distanceAt0Pitch = 750.0;
		int zRate = yAxisAbsoluteChange[pitch];

		double cosPitch = xAndYAxisChangeWithPitch[pitch];

		double[] playerPos = getPlayerPerspectivePosition();

		double xShift = distanceAt0Pitch * cosPitch * Math.sin(yawRad);
		double yShift = distanceAt0Pitch * cosPitch * Math.cos(yawRad);
		double focalPointX = playerPos[0] + xShift;
		double focalPointY = playerPos[1] - zRate;
		double focalPointZ = playerPos[2] - yShift;

		client.setCameraFocalPointX(focalPointX);
		client.setCameraFocalPointY(focalPointY);
		client.setCameraFocalPointZ(focalPointZ);
	}


	private double[] getPlayerPerspectivePosition()
	{
		LocalPoint lp = client.getLocalPlayer().getLocalLocation();
		double playerX = lp.getX();
		double playerY = Perspective.getTileHeight(client, lp, client.getTopLevelWorldView().getPlane()) - 200.0;
		double playerZ = lp.getY();

		return new double[] { playerX, playerY, playerZ };
	}
}
