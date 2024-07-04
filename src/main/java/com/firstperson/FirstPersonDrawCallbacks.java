package com.firstperson;

import net.runelite.api.Client;
import net.runelite.api.IntProjection;
import net.runelite.api.Perspective;
import net.runelite.api.Projection;
import net.runelite.api.Renderable;
import net.runelite.api.Scene;
import net.runelite.api.SceneTileModel;
import net.runelite.api.SceneTilePaint;
import net.runelite.api.Texture;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.hooks.DrawCallbacks;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.gpu.GpuPlugin;

public class FirstPersonDrawCallbacks implements DrawCallbacks
{
	Client client;

	GpuPlugin gpuPlugin;

	PluginManager pluginManager;

	public FirstPersonDrawCallbacks(Client client, GpuPlugin gpuPlugin, PluginManager pluginManager)
	{
		this.client = client;
		this.gpuPlugin = gpuPlugin;
		this.pluginManager = pluginManager;
	}


	@Override
	public void draw(Projection projection, Scene scene, Renderable renderable, int orientation, int x, int y, int z, long hash)
	{
		if (!pluginManager.isPluginEnabled(gpuPlugin)) return;

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
		if (!pluginManager.isPluginEnabled(gpuPlugin)) return;
		gpuPlugin.drawScenePaint(scene, paint, plane, tileX, tileZ);
	}

	@Override
	public void drawSceneTileModel(Scene scene, SceneTileModel model, int tileX, int tileZ)
	{
		if (!pluginManager.isPluginEnabled(gpuPlugin)) return;
		gpuPlugin.drawSceneTileModel(scene, model, tileX, tileZ);
	}

	public boolean disabledGpu = false;

	@Override
	public void draw(int overlayColor)
	{
		if (!pluginManager.isPluginEnabled(gpuPlugin)) return;
		System.out.println("DISABLED GPU");
		System.out.println(disabledGpu);
		System.out.println(pluginManager.isPluginEnabled(gpuPlugin));
		gpuPlugin.draw(overlayColor);
	}

	@Override
	public void drawScene(double cameraX, double cameraY, double cameraZ, double cameraPitch, double cameraYaw, int plane)
	{
		if (!pluginManager.isPluginEnabled(gpuPlugin)) return;
		System.out.println(pluginManager.isPluginEnabled(gpuPlugin));
		int[] firstPersonCamera = firstPersonCameraPosition();
		gpuPlugin.drawScene(firstPersonCamera[0], firstPersonCamera[1], firstPersonCamera[2], cameraPitch, cameraYaw, plane);
	}

	@Override
	public void postDrawScene()
	{
		if (!pluginManager.isPluginEnabled(gpuPlugin)) return;
		gpuPlugin.postDrawScene();
	}

	@Override
	public void animate(Texture texture, int diff)
	{
		if (!pluginManager.isPluginEnabled(gpuPlugin)) return;
		gpuPlugin.animate(texture, diff);
	}

	@Override
	public void loadScene(Scene scene)
	{
		if (!pluginManager.isPluginEnabled(gpuPlugin)) return;
		gpuPlugin.loadScene(scene);
	}

	@Override
	public void swapScene(Scene scene)
	{
		if (!pluginManager.isPluginEnabled(gpuPlugin)) return;
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
