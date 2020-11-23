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
package net.runelite.client.plugins.ElZMI;

import net.runelite.client.config.*;

@ConfigGroup("ElZMI")

public interface ElZMIConfig extends Config
{
	@ConfigItem(
			keyName = "instructions",
			name = "",
			description = "Instructions. Don't enter anything into this field",
			position = 0,
			titleSection = "instructionsTitle"
	)
	default String instructions()
	{
		return "Make sure you have Stamina (1)s, food, Essence, Air Runes and Cosmic Runes in your bank. " +
				"Equip a staff that gives earth runes and make sure you have ourania teleport and banking runes in your pouch.";
	}

	@ConfigItem(
			keyName = "giantPouch",
			name = "Use Giant Pouch",
			description = "Use giant pouch",
			position = 1
	)
	default boolean giantPouch() { return false; }

	@ConfigItem(
			keyName = "daeyalt",
			name = "Use Daeyalt Essence",
			description = "Use daeyalt essence",
			position = 2
	)
	default boolean daeyalt() { return false; }

	@ConfigItem(
			keyName = "trouver",
			name = "Use Trouver Rune Pouch",
			description = "Use Trouver Rune Pouch",
			position = 2
	)
	default boolean trouver() { return false; }

	@ConfigItem(
			keyName = "dropRunes",
			name = "Drop Runes",
			description = "Drop runes at altar",
			position = 3
	)
	default boolean dropRunes() { return false; }

	@ConfigItem(
			keyName = "dropRunesString",
			name = "Runes To Drop",
			description = "Runes you would like to drop.",
			position = 4,
			hidden = true,
			unhide = "dropRunes"
	)
	default String dropRunesString() { return "554,555,556,557,558,559"; }

	@ConfigItem(
			keyName = "noStams",
			name = "No Staminas",
			description = "Tick this if you don't have any stamina potions.",
			position = 5
	)
	default boolean noStams() { return false; }

	@ConfigItem(
			keyName = "minEnergy",
			name = "Minimum Energy",
			description = "Minimum energy before stam pot drank",
			position = 13,
			hidden = false,
			hide = "noStams"
	)
	default int minEnergy() { return 35; }

	@ConfigItem(
			keyName = "instructions2",
			name = "",
			description = "Instructions. Don't enter anything into this field",
			position = 14
	)
	default String instructions2()
	{
		return "Common food IDs: " +
				"Karambwan: 3144, Shark: 385, Monkfish: 7946.";
	}

	@ConfigItem(
			keyName = "foodType",
			name = "Food ID",
			description = "ID of food to eat",
			position = 15
	)
	default int foodId() { return 7946; }


	@ConfigItem(
			keyName = "minHealth",
			name = "Minimum Health",
			description = "Minimum health before food eaten",
			position = 16
	)
	default int minHealth() { return 65; }

	@ConfigItem(
			keyName = "pauseHealth",
			name = "Pause Health",
			description = "Health below which the plugin will pause.",
			position = 17
	)
	default int pauseHealth() { return 40; }

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