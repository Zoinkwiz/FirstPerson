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
