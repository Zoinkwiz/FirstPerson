package com.firstperson;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.IntProjection;

public class FirstPersonIntProjection implements IntProjection
{
	Client client;

	@Getter
	@Setter
	int cameraX;

	@Getter
	@Setter
	int cameraY;

	@Getter
	@Setter
	int cameraZ;

	@Getter
	@Setter
	int PitchSin;

	@Getter
	@Setter
	int pitchCos;

	@Getter
	@Setter
	int yawSin;

	@Getter
	@Setter
	int yawCos;

	public FirstPersonIntProjection(Client client)
	{
		this.client = client;
	}

	@Override
	public float[] project(float x, float y, float z)
	{
		// Translate coordinates to camera space
		float dx = x - (float) cameraX;
		float dy = y - (float) cameraY;
		float dz = z - (float) cameraZ;

		double yawRad = client.getCameraFpYaw();
		double pitchRad = client.getCameraFpPitch();

		float cosYaw = (float) Math.cos(-yawRad);
		float sinYaw = (float) Math.sin(-yawRad);
		float x1 = cosYaw * dx - sinYaw * dz;
		float z1 = sinYaw * dx + cosYaw * dz;

		float cosPitch = (float) Math.cos(pitchRad);
		float sinPitch = (float) Math.sin(pitchRad);
		float y1 = cosPitch * dy - sinPitch * z1;
		float z2 = sinPitch * dy + cosPitch * z1;

		// Return the transformed coordinates
		return new float[]{x1, y1, z2};
	}
}
