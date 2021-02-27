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
package net.runelite.client.plugins.eldiscord;

import net.runelite.client.config.*;

@ConfigGroup("ElDiscord")

public interface ElDiscordConfig extends Config
{
	String GROUP = "discordlootlogger";

	@ConfigSection(
			keyName = "discordTitle",
			name = "Discord Setup",
			description = "",
			position = 0
	)
	String discordTitle = "discordTitle";

	@ConfigItem(
			keyName = "webhook",
			name = "Webhook URL",
			description = "The Discord Webhook URL to send messages to",
			position = 0,
			section = "discordTitle"
	)
	String webhook();

	@ConfigItem(
			keyName = "userID",
			name = "Discord User ID",
			description = "Your Discord user id.",
			position = 1,
			section = "discordTitle"
	)
	String userID();

	@ConfigItem(
			keyName = "name",
			name = "Name",
			description = "Your name.",
			position = 2,
			section = "discordTitle"
	)
	String name();

	@ConfigSection(
			keyName = "generalTitle",
			name = "General",
			description = "",
			position = 1
	)
	String generalTitle = "generalTitle";

	@ConfigItem(
		keyName = "sendScreenshot",
		name = "Send Screenshots",
		description = "Includes a screenshot when receiving the loot",
			position = 0,
			section = "generalTitle"
	)
	default boolean sendScreenshot()
	{
		return false;
	}

	@ConfigItem(
			keyName = "sendPlayers",
			name = "Send Players",
			description = "Sends player spotted messages.",
			position = 1,
			section = "generalTitle"
	)
	default boolean sendPlayers()
	{
		return false;
	}

	@ConfigItem(
			keyName = "sendDamage",
			name = "Send Damage",
			description = "Sends damage taken messages.",
			position = 2,
			section = "generalTitle"
	)
	default boolean sendDamage()
	{
		return false;
	}

	@ConfigItem(
			keyName = "sendDeath",
			name = "Send Death Message",
			description = "Sends death messages.",
			position = 3,
			section = "generalTitle"
	)
	default boolean sendDeath()
	{
		return false;
	}

	@ConfigItem(
			keyName = "sendLevelUp",
			name = "Send Level Ups",
			description = "Sends level up messages.",
			position = 4,
			section = "generalTitle"
	)
	default boolean sendLevelUp()
	{
		return false;
	}

	@ConfigItem(
			keyName = "sendWorld",
			name = "Send World",
			description = "Sends world messages.",
			position = 5,
			section = "generalTitle"
	)
	default boolean sendWorld()
	{
		return false;
	}

	@ConfigItem(
			keyName = "sendKills",
			name = "Send Kills",
			description = "Sends Kill messages.",
			position = 6,
			section = "generalTitle"
	)
	default boolean sendKills()
	{
		return false;
	}

	@ConfigItem(
			keyName = "sendLoot",
			name = "Send Loot",
			description = "Sends Loot messages.",
			position = 7,
			section = "generalTitle"
	)
	default boolean sendLoot()
	{
		return false;
	}

	@ConfigItem(
			keyName = "idleMessage",
			name = "Idle Message (Ticks)",
			description = "If idle for this amount of ticks, mention user.",
			position = 8,
			section = "generalTitle"
	)
	default int idleMessage()
	{
		return 100;
	}

	@ConfigItem(
			keyName = "sendUpdateScreenshot",
			name = "Send Update Screenshot",
			description = "Sends an update screenshot every 30 seconds.",
			position = 10,
			section = "generalTitle"
	)
	default boolean sendUpdateScreenshot()
	{
		return true;
	}

	@ConfigItem(
			keyName = "updateInterval",
			name = "Interval (Ticks)",
			description = "Set the interval to send update screenshots.",
			position = 11,
			section = "generalTitle",
			hidden=true,
			unhide = "sendUpdateScreenshot"
	)
	default int updateInterval()
	{
		return 1000;
	}

	@ConfigSection(
			keyName = "hotkeyTitle",
			name = "Hotkey",
			description = "",
			position = 2
	)
	String hotkeyTitle = "hotkeyTitle";

	@ConfigItem(
			keyName = "hotkeyToggle",
			name = "Hotkey toggle",
			description = "Press this to send an instant message.",
			position = 0,
			section = "hotkeyTitle"
	)
	default Keybind hotkeyToggle()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
			keyName = "mentionUser",
			name = "Mention User",
			description = "Enable this to mention user.",
			position = 1,
			section = "hotkeyTitle"
	)
	default boolean mentionUser()
	{
		return true;
	}

	@ConfigSection(
			keyName = "lootTitle",
			name = "Loot",
			description = "",
			position = 3
	)
	String lootTitle = "lootTitle";

	@ConfigItem(
		keyName = "lootnpcs",
		name = "Loot NPCs",
		description = "Only logs loot from these NPCs, comma separated",
			position = 0,
			section = "lootTitle"
	)
	String lootNpcs();

	@ConfigItem(
			keyName = "stackvalue",
			name = "Include Stack Value",
			description = "Include the value of each stack.",
			position = 1,
			section = "lootTitle"
	)
	default boolean stackValue()
	{
		return true;
	}

	@ConfigItem(
		keyName = "includeLowValueItems",
		name = "Include Low Value Items",
		description = "Only log loot items worth more than the value set in loot value option.",
			position = 2,
			section = "lootTitle"
	)
	default boolean includeLowValueItems()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lootvalue",
		name = "Loot Value",
		description = "Only logs loot worth more then the given value. 0 to disable.",
			position = 3,
			section = "lootTitle",
			hidden = true,
			unhide = "includeLowValueItems"
	)
	default int lootValue()
	{
		return 100000;
	}
}