/*
 * Copyright (c) 2018, Andrew EP | ElPinche256 <https://github.com/ElPinche256>
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
package net.runelite.client.plugins.ElGlassBlower;

import net.runelite.client.config.Button;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("ElGlassBlower")

public interface ElGlassBlowerConfig extends Config
{
	@ConfigItem(
			keyName = "instructions",
			name = "",
			description = "Instructions.",
			position = 0,
			hidden = false,
			hide = "makeBestItem"
	)
	default String instructions()
	{
		return "Please select what item you would like to blow from the dropdown menu below.";
	}

	@ConfigItem(
			keyName = "type",
			name = "",
			description = "Select what you would like to blow.",
			position = 1,
			hidden = false,
			hide = "makeBestItem"
	)
	default ElGlassBlowerType type()
	{
		return ElGlassBlowerType.LIGHT_ORB;
	}

	@ConfigItem(
			keyName = "makeBestItem",
			name = "Make Best Item",
			description = "Toggle this to make the best item you currently can.",
			position = 2
	)
	default boolean makeBestItem()
	{
		return false;
	}

	@ConfigItem(
			keyName = "setCustomTickDelays",
			name = "Set Custom Tick Delays",
			description = "Don't change this unless you know what you are doing.",
			position = 3
	)
	default boolean setCustomTickDelays()
	{
		return false;
	}
	@ConfigItem(
			keyName = "tickDelays",
			name = "Tick Delays",
			description = "Tick Delays",
			position = 5,
			hidden = true,
			unhide = "setCustomTickDelays"
	)
	default String tickDelays()
	{
		return "1,3,1,2";
	}
	@ConfigItem(
			keyName = "setCustomSleepDelays",
			name = "Set Custom Sleep Delays",
			description = "Don't change this unless you know what you are doing.",
			position = 6
	)
	default boolean setCustomSleepDelays()
	{
		return false;
	}
	@ConfigItem(
			keyName = "sleepDelays",
			name = "Sleep Delays",
			description = "Sleep Delays",
			position = 8,
			hidden = true,
			unhide = "setCustomSleepDelays"
	)
	default String sleepDelays()
	{
		return "60,350,100,100";
	}

	@ConfigItem(
			keyName = "fastBank",
			name = "[EXPERIMENTAL] Bank Fast",
			description = "Banks faster than normal.",
			position = 9
	)
	default boolean fastBank()
	{
		return false;
	}

	@ConfigItem(
			keyName = "grandExchange",
			name = "Grand Exchange Bank",
			description = "Banks in the grand exchange.",
			position = 10
	)
	default boolean grandExchange()
	{
		return false;
	}

	@ConfigItem(
			keyName = "enableUI",
			name = "Enable UI",
			description = "Enable to turn on in game UI",
			position = 140
	)
	default boolean enableUI()
	{
		return true;
	}

	@ConfigItem(
			keyName = "startButton",
			name = "Start/Stop",
			description = "Test button that changes variable value",
			position = 150
	)
	default Button startButton()
	{
		return new Button();
	}
}