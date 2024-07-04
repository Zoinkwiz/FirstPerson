package com.firstperson;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;

public class CpuCamera
{
	Client client;
	FirstPersonConfig config;
	InputHandler inputHandler;

	final int[] yAxisAbsoluteChange = PreCalculatedTransformations.yAxisAbsoluteChange;

	final double[] xAndYAxisChangeWithPitch = PreCalculatedTransformations.xAndYAxisChangeWithPitch;

	long lastMillis = 0;

	public CpuCamera(Client client, FirstPersonConfig config, InputHandler inputHandler)
	{
		this.client = client;
		this.config = config;
		this.inputHandler = inputHandler;
	}

	public void updateCameraPosition()
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

		if (inputHandler.middleMousePressed)
		{
			if (inputHandler.yPosOfMouseDown != -1 && inputHandler.xPosOfMouseDown != -1)
			{
				addedPitch = currentMousePos.getY() - inputHandler.yPosOfMouseDown;
				addedYaw = currentMousePos.getX() - inputHandler.xPosOfMouseDown;
			}
			inputHandler.xPosOfMouseDown = currentMousePos.getX();
			inputHandler.yPosOfMouseDown = currentMousePos.getY();
		}
		else if (diff < 10000)
		{
			if (inputHandler.rightKeyPressed)
			{
				addedYaw = diff * cameraSpeed;
			}
			if (inputHandler.leftKeyPressed)
			{
				addedYaw = addedYaw - diff * cameraSpeed;
			}

			if (inputHandler.upKeyPressed)
			{
				addedPitch = -diff * cameraSpeed;
			}
			if (inputHandler.downKeyPressed)
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
			if (inputHandler.lastPitch == currentPitch && currentPitch >= client.getCameraPitchTarget() && addedPitch < 0)
			{
				client.setCameraPitchTarget(client.getCameraPitch());
			}
			else
			{
				client.setCameraPitchTarget(client.getCameraPitchTarget() + (int) addedPitch);
			}
			inputHandler.lastPitch = currentPitch;
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
}
