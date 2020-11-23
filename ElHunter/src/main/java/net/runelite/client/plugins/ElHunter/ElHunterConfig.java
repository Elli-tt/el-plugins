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
package net.runelite.client.plugins.ElHunter;

import net.runelite.client.config.*;

@ConfigGroup("ElHunter")

public interface ElHunterConfig extends Config
{

	@ConfigTitleSection(
			keyName = "instructionsTitle",
			name = "Instructions",
			description = "",
			position = 0
	)
	default Title instructionsTitle()
	{
		return new Title();
	}

	@ConfigItem(
			keyName = "instructions",
			name = "",
			description = "Instructions. Don't enter anything into this field",
			position = 0,
			titleSection = "instructionsTitle"
	)
	default String instructions()
	{
		return "Select from the dropdown which Hunter method you would like to use.";
	}

	@ConfigTitleSection(
			keyName = "generalTitle",
			name = "General Config",
			description = "",
			position = 1
	)
	default Title generalTitle()
	{
		return new Title();
	}

	@ConfigItem(
			keyName = "type",
			name = "Type",
			description = "Select what activity you would like to do.",
			position = 1,
			titleSection = "generalTitle"
	)
	default ElHunterType type()
	{
		return ElHunterType.SWAMP_LIZARDS;
	}

	@Range(
			min = 2,
			max = 3
	)
	@ConfigItem(
			keyName = "numTraps",
			name = "Number of Traps",
			description = "Select how many traps you would like to use.",
			position = 2,
			titleSection = "generalTitle",
			hidden = true,
			unhide = "type",
			unhideValue = "SWAMP_LIZARDS"
	)
	default int numTraps()
	{
		return 2;
	}


	@ConfigTitleSection(
			keyName = "delayTitle",
			name = "Delay Config",
			description = "",
			position = 40
	)
	default Title delayTitle()
	{
		return new Title();
	}

	@ConfigItem(
			keyName = "customDelays",
			name = "Use Custom Delays",
			description = "Click here to use custom delays",
			position = 40,
			titleSection = "delayTitle"
	)
	default boolean customDelays()
	{
		return false;
	}

	@ConfigItem(
			keyName = "sleepMin",
			name = "Sleep Min",
			description = "Enter minimum sleep delay here.",
			position = 41,
			titleSection = "delayTitle",
			hidden=true,
			unhide="customDelays"
	)
	default int sleepMin()
	{
		return 60;
	}

	@ConfigItem(
			keyName = "sleepMax",
			name = "Sleep Max",
			description = "Enter maximum sleep delay here.",
			position = 42,
			titleSection = "delayTitle",
			hidden=true,
			unhide="customDelays"
	)
	default int sleepMax()
	{
		return 350;
	}

	@ConfigItem(
			keyName = "sleepDeviation",
			name = "Sleep Deviation",
			description = "Enter sleep delay deviation here.",
			position = 43,
			titleSection = "delayTitle",
			hidden=true,
			unhide="customDelays"
	)
	default int sleepDeviation()
	{
		return 100;
	}

	@ConfigItem(
			keyName = "sleepTarget",
			name = "Sleep Target",
			description = "Enter target sleep delay here.",
			position = 44,
			titleSection = "delayTitle",
			hidden=true,
			unhide="customDelays"
	)
	default int sleepTarget()
	{
		return 100;
	}

	@ConfigItem(
			keyName = "sleepWeighted",
			name = "Sleep Weighted Distribution",
			description = "Click here to use a weighted distribution.",
			position = 45,
			titleSection = "delayTitle",
			hidden=true,
			unhide="customDelays"
	)
	default boolean sleepWeighted()
	{
		return false;
	}

	@ConfigItem(
			keyName = "tickMin",
			name = "Tick Min",
			description = "Enter minimum tick delay here.",
			position = 46,
			titleSection = "delayTitle",
			hidden=true,
			unhide="customDelays"
	)
	default int tickMin()
	{
		return 1;
	}

	@ConfigItem(
			keyName = "tickMax",
			name = "Tick Max",
			description = "Enter maximum tick delay here.",
			position = 47,
			titleSection = "delayTitle",
			hidden=true,
			unhide="customDelays"
	)
	default int tickMax()
	{
		return 3;
	}

	@ConfigItem(
			keyName = "tickDeviation",
			name = "Tick Deviation",
			description = "Enter tick delay deviation here.",
			position = 48,
			titleSection = "delayTitle",
			hidden=true,
			unhide="customDelays"
	)
	default int tickDeviation()
	{
		return 2;
	}

	@ConfigItem(
			keyName = "tickTarget",
			name = "Tick Target",
			description = "Enter target tick delay here.",
			position = 49,
			titleSection = "delayTitle",
			hidden=true,
			unhide="customDelays"
	)
	default int tickTarget()
	{
		return 2;
	}

	@ConfigItem(
			keyName = "tickWeighted",
			name = "Tick Weighted Distribution",
			description = "Click here to use a weighted distribution.",
			position = 50,
			titleSection = "delayTitle",
			hidden=true,
			unhide="customDelays"
	)
	default boolean tickWeighted()
	{
		return false;
	}

	@ConfigTitleSection(
			keyName = "uiTitle",
			name = "UI Config",
			description = "",
			position = 140
	)
	default Title uiTitle()
	{
		return new Title();
	}

	@ConfigItem(
			keyName = "enableUI",
			name = "Enable UI",
			description = "Enable to turn on in game UI",
			position = 140,
			titleSection = "uiTitle"
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