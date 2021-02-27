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
package net.runelite.client.plugins.eltutorial;

import net.runelite.client.config.*;

@ConfigGroup("ElTutorial")

public interface ElTutorialConfig extends Config
{

	@ConfigSection(
			keyName = "instructionsTitle",
			name = "Instructions",
			description = "",
			position = 0
	)
	String instructionsTitle = "instructionsTitle";

	@ConfigItem(
			keyName = "instructions",
			name = "",
			description = "Instructions. Don't enter anything into this field",
			position = 0,
			section = "instructionsTitle"
	)
	default String instructions()
	{
		return "Select from the dropdown which account type you would like to create.";
	}

	@ConfigSection(
			keyName = "generalTitle",
			name = "General Config",
			description = "",
			position = 1
	)
	String generalTitle = "generalTitle";

	@ConfigItem(
			keyName = "type",
			name = "Type",
			description = "Select which type of account you would like.",
			position = 1,
			section = "generalTitle"
	)
	default ElTutorialType type()
	{
		return ElTutorialType.REGULAR;
	}

	@ConfigItem(
			keyName = "female",
			name = "Female Character",
			description = "Click here to make your character female.",
			position = 2,
			section = "generalTitle"
	)
	default boolean female()
	{
		return false;
	}

	@ConfigItem(
			keyName = "bankPin",
			name = "Bank Pin (Iron Only)",
			description = "Enter your bank pin here.",
			position = 3,
			section = "generalTitle"
	)
	default String bankPin()
	{
		return "1337";
	}


	@ConfigSection(
			keyName = "delayTitle",
			name = "Delay Config",
			description = "",
			position = 40
	)
	String delayTitle = "delayTitle";

	@ConfigItem(
			keyName = "customDelays",
			name = "Use Custom Delays",
			description = "Click here to use custom delays",
			position = 40,
			section = "delayTitle"
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
			section = "delayTitle",
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
			section = "delayTitle",
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
			section = "delayTitle",
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
			section = "delayTitle",
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
			section = "delayTitle",
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
			section = "delayTitle",
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
			section = "delayTitle",
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
			section = "delayTitle",
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
			section = "delayTitle",
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
			section = "delayTitle",
			hidden=true,
			unhide="customDelays"
	)
	default boolean tickWeighted()
	{
		return false;
	}

	@ConfigSection(
			keyName = "uiTitle",
			name = "UI Config",
			description = "",
			position = 140
	)
	String uiTitle = "uiTitle";

	@ConfigItem(
			keyName = "enableUI",
			name = "Enable UI",
			description = "Enable to turn on in game UI",
			position = 140,
			section = "uiTitle"
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