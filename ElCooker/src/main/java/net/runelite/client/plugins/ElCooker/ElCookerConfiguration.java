/*
 * Copyright (c) 2018, SomeoneWithAnInternetConnection
 * Copyright (c) 2018, oplosthee <https://github.com/oplosthee>
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
package net.runelite.client.plugins.ElCooker;

import net.runelite.client.config.Button;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.ConfigTitleSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Title;

@ConfigGroup("ElCooker")
public interface ElCookerConfiguration extends Config
{

	@ConfigSection(
		keyName = "delayConfig",
		name = "Sleep Delay Configuration",
		description = "Configure how the bot handles sleep delays",
		position = 2
	)
	default boolean delayConfig()
	{
		return false;
	}

	@Range(
		min = 0,
		max = 550
	)
	@ConfigItem(
		keyName = "sleepMin",
		name = "Sleep Min",
		description = "",
		position = 3,
		section = "delayConfig"
	)
	default int sleepMin()
	{
		return 60;
	}

	@Range(
		min = 0,
		max = 550
	)
	@ConfigItem(
		keyName = "sleepMax",
		name = "Sleep Max",
		description = "",
		position = 4,
		section = "delayConfig"
	)
	default int sleepMax()
	{
		return 350;
	}

	@Range(
		min = 0,
		max = 550
	)
	@ConfigItem(
		keyName = "sleepTarget",
		name = "Sleep Target",
		description = "",
		position = 5,
		section = "delayConfig"
	)
	default int sleepTarget()
	{
		return 100;
	}

	@Range(
		min = 0,
		max = 550
	)
	@ConfigItem(
		keyName = "sleepDeviation",
		name = "Sleep Deviation",
		description = "",
		position = 6,
		section = "delayConfig"
	)
	default int sleepDeviation()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "sleepWeightedDistribution",
		name = "Sleep Weighted Distribution",
		description = "Shifts the random distribution towards the lower end at the target, otherwise it will be an even distribution",
		position = 7,
		section = "delayConfig"
	)
	default boolean sleepWeightedDistribution()
	{
		return false;
	}

	@ConfigSection(
		keyName = "delayTickConfig",
		name = "Game Tick Configuration",
		description = "Configure how the bot handles game tick delays, 1 game tick equates to roughly 600ms",
		position = 8
	)
	default boolean delayTickConfig()
	{
		return false;
	}

	@Range(
		min = 0,
		max = 10
	)
	@ConfigItem(
		keyName = "tickDelayMin",
		name = "Game Tick Min",
		description = "",
		position = 9,
		section = "delayTickConfig"
	)
	default int tickDelayMin()
	{
		return 1;
	}

	@Range(
		min = 0,
		max = 10
	)
	@ConfigItem(
		keyName = "tickDelayMax",
		name = "Game Tick Max",
		description = "",
		position = 10,
		section = "delayTickConfig"
	)
	default int tickDelayMax()
	{
		return 3;
	}

	@Range(
		min = 0,
		max = 10
	)
	@ConfigItem(
		keyName = "tickDelayTarget",
		name = "Game Tick Target",
		description = "",
		position = 11,
		section = "delayTickConfig"
	)
	default int tickDelayTarget()
	{
		return 2;
	}

	@Range(
		min = 0,
		max = 10
	)
	@ConfigItem(
		keyName = "tickDelayDeviation",
		name = "Game Tick Deviation",
		description = "",
		position = 12,
		section = "delayTickConfig"
	)
	default int tickDelayDeviation()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "tickDelayWeightedDistribution",
		name = "Game Tick Weighted Distribution",
		description = "Shifts the random distribution towards the lower end at the target, otherwise it will be an even distribution",
		position = 13,
		section = "delayTickConfig"
	)
	default boolean tickDelayWeightedDistribution()
	{
		return false;
	}

	@ConfigTitleSection(
			keyName = "cookerfisherTitle",
			name = "Cooker Configuration",
			description = "",
			position = 20
	)
	default Title cookerTitle()
	{
		return new Title();
	}

	@ConfigItem(
		keyName = "instructions",
		name = "",
		description = "Instructions. Don't enter anything into this field",
		position = 40
	)
	default String instructions()
	{
		return "Cooks raw food. Doesn't currently work with NPCs to bank.";
	}

	@ConfigItem(
			keyName = "seaweedMode",
			name = "Giant Seaweed",
			description = "Tick this to use giant seaweed.",
			position = 69,
			section = "cookerTitle"
	)
	default boolean seaweedMode()
	{
		return false;
	}

	@ConfigItem(
			keyName = "rawFoodId",
			name = "Raw Food ID",
			description = "Enter your raw food ID here.",
			position = 70,
			section = "cookerTitle",
			hidden=false,
			hide="seaweedMode"
	)
	default int rawFoodId()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "roguesDen",
			name = "Rogues' Den",
			description = "Enable this to cook in Rogues' Den.",
			position = 75,
			section = "cookerTitle"
	)
	default boolean roguesDen()
	{
		return false;
	}

	@ConfigItem(
			keyName = "instructions2",
			name = "",
			description = "Instructions2. Don't enter anything into this field",
			position = 82,
			hide = "roguesDen"
	)
	default String instructions3()
	{
		return "Click Value Finder and click on a bank. Check chat for the IDs you should enter here. Hosidius range settings: bankId: 21301, opCode: 3, rangeId: 21302.";
	}

	@ConfigItem(
			keyName = "valueFinder",
			name = "Value Finder",
			description = "Enable this to get a game output of values for the settings.",
			position = 83,
			hide = "roguesDen"
	)
	default boolean valueFinder()
	{
		return false;
	}

	@ConfigItem(
			keyName = "bankObjectId",
			name = "Bank Object Id",
			description = "Enter the ID of a bank booth, chest or similar. Will not work with NPCs.",
			position = 84,
			hide = "roguesDen"
	)
	default int bankObjectId()
	{
		return 21301;
	}


	@ConfigItem(
			keyName = "bankOpCode",
			name = "Bank Op Code",
			description = "Enter the Op of a bank booth, chest or similar. Will not work with NPCs.",
			position = 85,
			hide = "roguesDen"
	)
	default int bankOpCode()
	{
		return 3;
	}

	@ConfigItem(
			keyName = "rangeObjectId",
			name = "Range/Fire Object Id",
			description = "Enter the ID of a range or fire.",
			position = 86,
			hide = "roguesDen"
	)
	default int rangeObjectId()
	{
		return 21302;
	}

	@ConfigItem(
		keyName = "enableUI",
		name = "Enable UI",
		description = "Enable to turn on in game UI",
		position = 140,
		titleSection = "cookerTitle"
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
