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
package com.firstperson.gpu;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Projection;
import net.runelite.api.Renderable;
import net.runelite.api.Scene;
import net.runelite.api.SceneTileModel;
import net.runelite.api.SceneTilePaint;
import net.runelite.api.Texture;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.hooks.DrawCallbacks;

public class FirstPersonDrawCallbacks implements DrawCallbacks
{
	Client client;

	@Getter
	DrawCallbacks pluginImplementingDrawCallback;


	public FirstPersonDrawCallbacks(Client client)
	{
		this.client = client;
	}

	@Override
	public void draw(Projection projection, Scene scene, Renderable renderable, int orientation, int x, int y, int z, long hash)
	{
		pluginImplementingDrawCallback.draw(projection, scene, renderable, orientation, x, y, z, hash);
	}

	@Override
	public void drawScenePaint(Scene scene, SceneTilePaint paint, int plane, int tileX, int tileZ)
	{
		pluginImplementingDrawCallback.drawScenePaint(scene, paint, plane, tileX, tileZ);
	}

	@Override
	public void drawSceneTileModel(Scene scene, SceneTileModel model, int tileX, int tileZ)
	{
		pluginImplementingDrawCallback.drawSceneTileModel(scene, model, tileX, tileZ);
	}
	@Override
	public void draw(int overlayColor)
	{
		pluginImplementingDrawCallback.draw(overlayColor);
	}

	@Override
	public void drawScene(double cameraX, double cameraY, double cameraZ, double cameraPitch, double cameraYaw, int plane)
	{
		int[] firstPersonCamera = firstPersonCameraPosition();
		pluginImplementingDrawCallback.drawScene(firstPersonCamera[0], firstPersonCamera[1], firstPersonCamera[2], cameraPitch, cameraYaw, plane);
	}

	@Override
	public void postDrawScene()
	{
		pluginImplementingDrawCallback.postDrawScene();
	}

	@Override
	public void animate(Texture texture, int diff)
	{
		pluginImplementingDrawCallback.animate(texture, diff);
	}

	@Override
	public void loadScene(Scene scene)
	{
		pluginImplementingDrawCallback.loadScene(scene);
	}

	@Override
	public void swapScene(Scene scene)
	{
		pluginImplementingDrawCallback.swapScene(scene);
	}

	public void setCallback(DrawCallbacks pluginWithCallbacks)
	{
		pluginImplementingDrawCallback =  pluginWithCallbacks;
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
