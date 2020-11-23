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
package net.runelite.client.plugins.ElSandstone;

import com.google.inject.Provides;
import com.owain.chinbreakhandler.ChinBreakHandler;
import java.time.Instant;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.botutils.BotUtils;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;
import static net.runelite.client.plugins.ElSandstone.ElSandstoneState.*;


@Extension
@PluginDependency(BotUtils.class)
@PluginDescriptor(
	name = "El Sandstone",
	enabledByDefault = false,
	description = "Mines sandstone for you.",
	tags = {"mining, bot, power, skill"},
	type = PluginType.SKILLING
)
@Slf4j
public class ElSandstonePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ElSandstoneConfiguration config;

	@Inject
	private BotUtils utils;

	@Inject
	private ConfigManager configManager;

	@Inject
	PluginManager pluginManager;

	@Inject
	OverlayManager overlayManager;

	@Inject
	private ElSandstoneOverlay overlay;

	@Inject
	private ChinBreakHandler chinBreakHandler;

	ElSandstoneState state;
	GameObject targetObject;
	MenuEntry targetMenu;
	WorldPoint skillLocation;
	Instant botTimer;
	LocalPoint beforeLoc;
	Player player;
	private final WorldPoint WEST_ROCK = new WorldPoint(3164, 2914, 0);
	private final WorldPoint SW_ROCK = new WorldPoint(3166, 2913, 0);
	private final WorldPoint SE_ROCK = new WorldPoint(3167, 2913, 0);
	private final WorldArea DESERT_QUARRY = new WorldArea(new WorldPoint(3148,2896,0),new WorldPoint(3186,2926,0));
	int waterskinsLeft;

	int timeout = 0;
	long sleepLength;
	boolean startSandstoneMiner;


	@Provides
	ElSandstoneConfiguration provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ElSandstoneConfiguration.class);
	}

	@Override
	protected void startUp()
	{
		chinBreakHandler.registerPlugin(this);
	}

	@Override
	protected void shutDown()
	{
		resetVals();
		chinBreakHandler.unregisterPlugin(this);
	}

	private void resetVals()
	{
		overlayManager.remove(overlay);
		chinBreakHandler.stopPlugin(this);
		state = null;
		timeout = 0;
		botTimer = null;
		skillLocation = null;
		startSandstoneMiner = false;
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (!configButtonClicked.getGroup().equalsIgnoreCase("ElSandstone"))
		{
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton"))
		{
			if (!startSandstoneMiner)
			{
				startSandstoneMiner = true;
				chinBreakHandler.startPlugin(this);
				state = null;
				targetMenu = null;
				botTimer = Instant.now();
				setLocation();
				overlayManager.add(overlay);
			}
			else
			{
				resetVals();
			}
		}
	}

	public void setLocation()
	{
		if (client != null && client.getLocalPlayer() != null && client.getGameState().equals(GameState.LOGGED_IN))
		{
			skillLocation = client.getLocalPlayer().getWorldLocation();
			beforeLoc = client.getLocalPlayer().getLocalLocation();
		}
		else
		{
			log.debug("Tried to start bot before being logged in");
			skillLocation = null;
			resetVals();
		}
	}

	private long sleepDelay()
	{
		sleepLength = utils.randomDelay(config.sleepWeightedDistribution(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
		return sleepLength;
	}

	private int tickDelay()
	{
		int tickLength = (int) utils.randomDelay(config.tickDelayWeightedDistribution(), config.tickDelayMin(), config.tickDelayMax(), config.tickDelayDeviation(), config.tickDelayTarget());
		log.debug("tick delay for {} ticks", tickLength);
		return tickLength;
	}


	private GameObject getDenseEssence()
	{
		assert client.isClientThread();

		if (client.getVarbitValue(4927) == 0)
		{
			return utils.findNearestGameObject(NullObjectID.NULL_8981);
		}
		if (client.getVarbitValue(4928) == 0)
		{
			return utils.findNearestGameObject(NullObjectID.NULL_10796);
		}
		return null;
	}

	public ElSandstoneState getState()
	{
		if (timeout > 0)
		{
			return TIMEOUT;
		}
		if (utils.iterating)
		{
			return ITERATING;
		}
		if (utils.isMoving(beforeLoc))
		{
			timeout = 2 + tickDelay();
			return MOVING;
		}

		if(DESERT_QUARRY.intersectsWith(player.getWorldArea())){
			updateWaterskinsLeft();
			if(waterskinsLeft==0){
				return CASTING_HUMIDIFY;
			}
		}
		if(utils.inventoryFull()){
			return ADDING_SANDSTONE_TO_GRINDER;
		} else if (player.getWorldLocation().equals(new WorldPoint(3152,2910,0))) {
			return WALKING_BACK_TO_SANDSTONE;
		}
		if (chinBreakHandler.shouldBreak(this))
		{
			return HANDLE_BREAK;
		}
		if (client.getLocalPlayer().getAnimation() == -1)
		{
			return FIND_GAME_OBJECT;

		}
		return ANIMATING;
	}

	@Subscribe
	private void onGameTick(GameTick tick)
	{
		if (!startSandstoneMiner || chinBreakHandler.isBreakActive(this))
		{
			return;
		}
		player = client.getLocalPlayer();
		if (client != null && player != null && skillLocation != null)
		{
			if (!client.isResized())
			{
				utils.sendGameMessage("client must be set to resizable");
				startSandstoneMiner = false;
				return;
			}
			state = getState();
			beforeLoc = player.getLocalLocation();
			switch (state)
			{
				case TIMEOUT:
					utils.handleRun(30, 20);
					timeout--;
					break;
				case CASTING_HUMIDIFY:
					castHumidify();
					timeout=tickDelay();
					break;
				case ADDING_SANDSTONE_TO_GRINDER:
					interactGrinder();
					timeout=tickDelay();
					break;
				case WALKING_BACK_TO_SANDSTONE:
					utils.walk(new WorldPoint(3166,2914,0),1,sleepDelay());
					timeout=tickDelay();
					break;
				case FIND_GAME_OBJECT:
					interactSandstone();
					timeout = tickDelay();
					break;
				case HANDLE_BREAK:
					chinBreakHandler.startBreak(this);
					timeout = 10;
					break;
				case ANIMATING:
				case MOVING:
					utils.handleRun(30, 20);
					timeout = tickDelay();
					break;
			}
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		if (targetObject == null || event.getGameObject() != targetObject || !startSandstoneMiner)
		{
			return;
		}
		else
		{
			if (client.getLocalDestinationLocation() != null)
			{
				interactSandstone(); //This is a failsafe, Player can get stuck with a destination on object despawn and be "forever moving".
			}
		}
	}

	private void interactSandstone()
	{
		int sandstoneId = 11386;
		for(GameObject gameObject : utils.getGameObjects(sandstoneId)){
			if(gameObject.getWorldLocation().equals(WEST_ROCK)){
				targetObject=gameObject; //west rock
				break;
			} else if(gameObject.getWorldLocation().equals(SW_ROCK)){
				targetObject=gameObject; //south west rock
				break;
			} else if(gameObject.getWorldLocation().equals(SE_ROCK)){
				targetObject=gameObject; //south east rock
				break;
			}
		}
		if (targetObject != null)
		{
			targetMenu = new MenuEntry("", "", targetObject.getId(), MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId(),
					targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
			utils.setMenuEntry(targetMenu);
			utils.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
		}
		else
		{
			log.info("Couldn't find any sandstone.");
		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN && startSandstoneMiner)
		{
			state = TIMEOUT;
			timeout = 2;
		}
	}

	private void interactGrinder()
	{
		//looking for the grinder
		int grinderId = 26199;
		targetObject = utils.getGameObjects(grinderId).get(0);
		if (targetObject != null)
		{
			targetMenu = new MenuEntry("", "", targetObject.getId(), MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId(),
					targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
			utils.setMenuEntry(targetMenu);
			utils.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
		}
		else
		{
			log.info("Couldn't find the grinder.");
		}
	}

	private void updateWaterskinsLeft(){
		waterskinsLeft=0;
		waterskinsLeft+=utils.getInventoryItemCount(1823,false)*4; //4 dose waterskin
		waterskinsLeft+=utils.getInventoryItemCount(1825,false)*3; //3 dose waterskin
		waterskinsLeft+=utils.getInventoryItemCount(1827,false)*2; //2 dose waterskin
		waterskinsLeft+=utils.getInventoryItemCount(1829,false); //1 dose waterskin

		if(waterskinsLeft==0){
			if(!utils.inventoryContains(1831)){
				waterskinsLeft=-1; //no waterskins detected
			}
		}
	}

	private void castHumidify(){
		if(!utils.inventoryContains(9075) && !utils.runePouchContains(9075)){
			utils.sendGameMessage("out of astrals runes");
			startSandstoneMiner = false;
		}
		targetMenu = new MenuEntry("Cast","<col=00ff00>Humidify</col>",1,57,-1,14286954,false);
		Widget spellWidget = utils.getSpellWidget("Humidify");
		if(spellWidget==null){
			utils.sendGameMessage("unable to find humidify widget");
			startSandstoneMiner = false;
		}
		utils.oneClickCastSpell(utils.getSpellWidgetInfo("Humidify"),targetMenu,sleepDelay());
	}
}
