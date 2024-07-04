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

import com.google.inject.Injector;
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
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.gpu.GpuPlugin;
import net.runelite.client.plugins.gpu.GpuPluginConfig;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDependency(GpuPlugin.class)
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
	private GpuPluginConfig gpuConfig;

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

	@Inject
	GpuPlugin gpuPlugin;

	FirstPersonDrawCallbacks firstPersonDrawCallbacks;

	InputHandler inputHandler;

	CpuCamera cpuCamera;

	@Inject
	Injector injector;

	@Inject
	EventBus eventBus;

	@Inject
	ConfigManager configManager;

	private final Runnable updateFocus = () -> {
		if (client.getCameraMode() != 1) activateFirstPersonCameraMode();
		cpuCamera.updateCameraPosition();
	};

	@Inject
	private PluginManager pluginManager;

	@Override
	protected void startUp() throws Exception
	{
		firstPersonDrawCallbacks = new FirstPersonDrawCallbacks(client, gpuPlugin, pluginManager);
		inputHandler = new InputHandler();
		cpuCamera = new CpuCamera(client, config, inputHandler);
		cpuCamera.lastMillis = System.currentTimeMillis();
		activateFirstPersonCameraMode();

		if (config.useGpu())
		{
			if (pluginManager.isPluginEnabled(gpuPlugin))
			{
				clientThread.invoke(this::activateGpuMode);
			}
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		disableFirstPersonPlugin();
	}

	@Subscribe
	public void onPluginChanged(PluginChanged pluginChanged)
	{
		if (pluginChanged.getPlugin() == gpuPlugin)
		{
			System.out.println("MOO");
			// If GPU plugin now active, we need to set this to be the drawCallback if we're trying for GPU mode
			if (pluginChanged.isLoaded() && config.useGpu())
			{
				activateGpuMode();
			}
			else if (!pluginChanged.isLoaded())
			{
				firstPersonDrawCallbacks.disabledGpu = true;
				disableGpuMode();
			}
		}
	}

	private void activateGpuMode()
	{
		keyManager.unregisterKeyListener(inputHandler);
		mouseManager.unregisterMouseListener(inputHandler);
		client.setCameraPitchRelaxerEnabled(false);
		client.setCameraMode(0);
		drawManager.unregisterEveryFrameListener(updateFocus);

		clientThread.invokeLater(() -> client.setDrawCallbacks(firstPersonDrawCallbacks));
	}

	private void disableGpuMode()
	{
		clientThread.invokeLater(() -> {
			enableCpuMode();
			client.setDrawCallbacks(gpuPlugin);
		});
	}

	private void disableFirstPersonPlugin()
	{
		keyManager.unregisterKeyListener(inputHandler);
		mouseManager.unregisterMouseListener(inputHandler);
		client.setCameraPitchRelaxerEnabled(false);
		client.setCameraMode(0);
		drawManager.unregisterEveryFrameListener(updateFocus);
		clientThread.invokeLater(() -> {
			if (pluginManager.isPluginEnabled(gpuPlugin))
			{
				client.setDrawCallbacks(gpuPlugin);
			}
			else
			{
				client.setDrawCallbacks(null);
			}
		});
	}


	private void activateFirstPersonCameraMode()
	{
		client.setCameraMode(1);
		// For some reason this won't turn back on once I've turned it off, resulting in a shifted view when beyond
		// the relaxed limit
		client.setCameraPitchRelaxerEnabled(true);
	}

	private void enableCpuMode()
	{
		keyManager.registerKeyListener(inputHandler);
		mouseManager.registerMouseListener(inputHandler);
		drawManager.registerEveryFrameListener(updateFocus);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("firstperson") && event.getKey().equals("useGpu"))
		{
			if ("true".equals(event.getNewValue()))
			{
				if (pluginManager.isPluginEnabled(gpuPlugin))
				{
					activateGpuMode();
				}
			}
			else
			{
					disableGpuMode();
			}
		}
	}

	@Provides
	FirstPersonConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FirstPersonConfig.class);
	}
}
