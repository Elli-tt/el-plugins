package net.runelite.client.plugins.ElAstrals;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import org.pf4j.Extension;
import net.runelite.client.plugins.botutils.BotUtils;

@Extension
@PluginDependency(BotUtils.class)
@PluginDescriptor(
		name = "El Astrals",
		description = "Crafts astrals.",
		type = PluginType.SKILLING
)
@Slf4j
public class ElAstralsPlugin extends Plugin
{
	@Provides
	ElAstralsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ElAstralsConfig.class);
	}
	@Override
	protected void startUp()
	{

	}

	@Override
	protected void shutDown()
	{

	}
}
