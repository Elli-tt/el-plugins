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
package net.runelite.client.plugins.ElSuperglass;

import net.runelite.client.config.Button;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("ElSuperglass")

public interface ElSuperglassConfig extends Config
{
	@ConfigItem(
			keyName = "instructions1",
			name = "",
			description = "Instructions. Don't enter anything into this field",
			position = 0,
			titleSection = "instructionsTitle"
	)
	default String instructions1()
	{
		return "Stand in a bank with astral runes in your inventory. " +
				"Wear Staff of Air and Tome of Fire/Smoke Battlestaff.";
	}

	@ConfigItem(
		keyName = "pickupExtraGlass",
		name = "Pick up extra glass",
		description = "Picks up extra glass.",
		position = 1
	)
	default boolean pickupExtraGlass()
	{
		return true;
	}

	@ConfigItem(
			keyName = "useCustomDelays",
			name = "Use custom delays",
			description = "Enable this to set custom delays.",
			position = 3
	)
	default boolean useCustomDelays()
	{
		return false;
	}

	@ConfigItem(
			keyName = "instructions2",
			name = "",
			description = "Instructions. Don't enter anything into this field",
			position = 4,
			hidden = true,
			unhide = "useCustomDelays"
	)
	default String instructions2()
	{
		return "Please enter your custom delays below, they should be in the form: " +
				"\"minimum,maximum,target,variance\"";
	}

	@ConfigItem(
			keyName = "customDelays",
			name = "Set Custom Delays",
			description = "Set custom delays using this.",
			position = 5,
			hidden = true,
			unhide = "useCustomDelays"
	)
	default String customDelays()
	{
		return "1,5,3,1";
	}

	@ConfigItem(
			keyName = "clientTickDelay",
			name = "Add Client Tick Delay",
			description = "Adds this number to the client tick delay.",
			position = 6,
			hidden = true,
			unhide = "useCustomDelays"
	)
	default int clientTickDelay()
	{
		return 0;
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