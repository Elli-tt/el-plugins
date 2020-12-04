package net.runelite.client.plugins.ElBankStander;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.botutils.BotUtils;
import org.pf4j.Extension;

@Extension
@PluginDependency(BotUtils.class)
@PluginDescriptor(
	name = "El Bank Stander",
	description = "Performs various bank standing activities",
	type = PluginType.MISCELLANEOUS
)
@Slf4j
public class ElBankStanderPlugin extends Plugin
{
	@Provides
	ElBankStanderConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ElBankStanderConfig.class);
	}
	@Override
	protected void startUp()
	{

	}

	@Override
	protected void shutDown()
	{
		log.info("Plugin stopped");
	}
}
