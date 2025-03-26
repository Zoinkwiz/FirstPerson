/*
 * Copyright (c) 2024, Zoinkwiz <https://github.com/Zoinkwiz>
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
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

import com.firstperson.detachedcamera.DetachedCameraMovementHandler;
import com.firstperson.gpu.FirstPersonDrawCallbacks;
import com.firstperson.input.InputHandler;
import com.firstperson.input.ToggleInputHandler;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.gpu.GpuPlugin;
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
	ConfigManager configManager;

	FirstPersonDrawCallbacks firstPersonDrawCallbacks;

	InputHandler inputHandler;

	ToggleInputHandler toggleInputHandler;

	DetachedCameraMovementHandler detachedCameraMovementHandler;

	@Getter
	boolean active;

	@Override
	protected void startUp() throws Exception
	{
		firstPersonDrawCallbacks = new FirstPersonDrawCallbacks(client);
		inputHandler = new InputHandler(client, this, config, System.currentTimeMillis());
		toggleInputHandler = new ToggleInputHandler(this, config);
		detachedCameraMovementHandler = new DetachedCameraMovementHandler(client, config, inputHandler);

		activate();
		keyManager.registerKeyListener(toggleInputHandler);
	}

	@Override
	protected void shutDown() throws Exception
	{
		deactivate();
		keyManager.unregisterKeyListener(toggleInputHandler);
	}

	public void activate()
	{
		active = true;
		if (client.getDrawCallbacks() != null && firstPersonDrawCallbacks != client.getDrawCallbacks())
		{
			firstPersonDrawCallbacks.setCallback(client.getDrawCallbacks());
		}

		keyManager.registerKeyListener(inputHandler);
		mouseManager.registerMouseListener(inputHandler);

		drawManager.registerEveryFrameListener(cameraMovementHandler);

		client.setCameraPitchRelaxerEnabled(true);
	}

	public void deactivate()
	{
		active = false;
		keyManager.unregisterKeyListener(inputHandler);
		mouseManager.unregisterMouseListener(inputHandler);
		client.setCameraMode(0);
		drawManager.unregisterEveryFrameListener(cameraMovementHandler);
		clientThread.invokeLater(() -> {
			if (config.useGpu())
			{
				if (firstPersonDrawCallbacks.getPluginImplementingDrawCallback() != null)
				{
					client.setDrawCallbacks(firstPersonDrawCallbacks.getPluginImplementingDrawCallback());
				}
				else
				{
					client.setDrawCallbacks(null);
				}
			}
		});

		client.setCameraPitchRelaxerEnabled(false);
	}

	public boolean isGpuActive()
	{
		return config.useGpu() && firstPersonDrawCallbacks.getPluginImplementingDrawCallback() != null;
	}

	private final Runnable cameraMovementHandler = () -> {
		inputHandler.updateCameraPosition();
		if (!isGpuActive())
		{
			if (client.getCameraMode() != 1) client.setCameraMode(1);
			detachedCameraMovementHandler.updateDetachedCameraPosition();
		}
	};

	private void activateGpuMode()
	{
		if (!active) return;
		clientThread.invokeLater(() -> {
			if (client.getDrawCallbacks() != null && client.getDrawCallbacks() != firstPersonDrawCallbacks)
			{
				firstPersonDrawCallbacks.setCallback(client.getDrawCallbacks());
				client.setDrawCallbacks(firstPersonDrawCallbacks);
				client.setCameraMode(0);
			}
			else
			{
				disableGpuMode();
			}
		});
	}

	private void disableGpuMode()
	{
		if (!active) return;
		clientThread.invokeLater(() -> {
			if (firstPersonDrawCallbacks.getPluginImplementingDrawCallback() != null)
			{
				client.setDrawCallbacks(firstPersonDrawCallbacks.getPluginImplementingDrawCallback());
			}
			else
			{
				client.setDrawCallbacks(null);
			}
			client.setCameraMode(1);
		});
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		// If changed to something
		if (client.getDrawCallbacks() == firstPersonDrawCallbacks) return;

		// Something cleared the drawcallback. We should revert from GPU mode
		if (client.getDrawCallbacks() == null && firstPersonDrawCallbacks.getPluginImplementingDrawCallback() != null)
		{
			firstPersonDrawCallbacks.setCallback(null);
			if (config.useGpu()) disableGpuMode();
		}
		else if (client.getDrawCallbacks() != null && client.getDrawCallbacks() != firstPersonDrawCallbacks)
		{
			// New drawcallback set. We should wrap it and use it
			firstPersonDrawCallbacks.setCallback(client.getDrawCallbacks());
			if (config.useGpu()) activateGpuMode();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("firstperson") && event.getKey().equals("useGpu"))
		{
			if ("true".equals(event.getNewValue()))
			{
				activateGpuMode();
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
