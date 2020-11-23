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
package net.runelite.client.plugins.ElKarambwans;

import com.google.inject.Provides;
import com.owain.chinbreakhandler.ChinBreakHandler;
import java.awt.Rectangle;
import java.time.Instant;
import java.util.*;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.botutils.BotUtils;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;
import static net.runelite.client.plugins.ElKarambwans.ElKarambwansState.*;


@Extension
@PluginDependency(BotUtils.class)
@PluginDescriptor(
	name = "El Karambwans",
	enabledByDefault = false,
	description = "Fishes karambwans",
	tags = {"fish, karambwans, fishing, el"},
	type = PluginType.SKILLING
)
@Slf4j
public class ElKarambwansPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ElKarambwansConfiguration config;

	@Inject
	private BotUtils utils;

	@Inject
	private ConfigManager configManager;

	@Inject
	PluginManager pluginManager;

	@Inject
	OverlayManager overlayManager;

	@Inject
	private ElKarambwansOverlay overlay;

	@Inject
	private ChinBreakHandler chinBreakHandler;


	ElKarambwansState state;
	GameObject targetObject;
	NPC targetNPC;
	MenuEntry targetMenu;
	WorldPoint skillLocation;
	Instant botTimer;
	LocalPoint beforeLoc;
	Player player;
	Rectangle altRect = new Rectangle(-100,-100, 10, 10);

	WorldPoint FISHING_SPOT = new WorldPoint(2899,3118,0);
	WorldArea FISHING_AREA = new WorldArea(new WorldPoint(2898,3110,0),new WorldPoint(2901,3119,0));

	WorldPoint ZANARIS_RING = new WorldPoint(2412,4434,0);
	WorldArea ZANARIS_MID_AREA = new WorldArea(new WorldPoint(2398,4443,0),new WorldPoint(2402,4451,0));
	WorldArea ZANARIS_BANK_AREA = new WorldArea(new WorldPoint(2380,4454,0),new WorldPoint(2387,4461,0));

	int timeout = 0;
	long sleepLength;
	boolean startKarambwanFisher;
	boolean firstTimeUsingChisel;
	private final Set<Integer> itemIds = new HashSet<>();
	private final Set<Integer> requiredIds = new HashSet<>();


	@Provides
	ElKarambwansConfiguration provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ElKarambwansConfiguration.class);
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
		startKarambwanFisher = false;
		requiredIds.clear();
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (!configButtonClicked.getGroup().equalsIgnoreCase("ElKarambwans"))
		{
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton"))
		{
			if (!startKarambwanFisher)
			{
				startKarambwanFisher = true;
				chinBreakHandler.startPlugin(this);
				state = null;
				targetMenu = null;
				botTimer = Instant.now();
				setLocation();
				overlayManager.add(overlay);
				requiredIds.add(3159);
				requiredIds.add(3150);
			}
			else
			{
				resetVals();
			}
		}
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("ElKarambwans"))
		{
			return;
		}
		startKarambwanFisher = false;
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

	private void interactFishingSpot()
	{
		targetNPC = utils.findNearestNpcWithin(player.getWorldLocation(), 10, Collections.singleton(4712));
		if (targetNPC != null)
		{
			targetMenu = new MenuEntry("", "", targetNPC.getIndex(), 9, 0, 0, false);
			utils.setMenuEntry(targetMenu);
			utils.delayMouseClick(targetNPC.getConvexHull().getBounds(), sleepDelay());
		}
		else
		{
			log.info("Fishing Spot is null");
		}
	}

	private void openBank()
	{
		targetNPC = utils.findNearestNpcWithin(player.getWorldLocation(), 25, Collections.singleton(3092));
		if (targetNPC != null)
		{
			targetMenu = new MenuEntry("Bank", "<col=ffff00>Banker", targetNPC.getIndex(), 11, 0, 0, false);
			utils.setMenuEntry(targetMenu);
			utils.delayMouseClick(targetNPC.getConvexHull().getBounds(), sleepDelay());
		}
		else
		{
			log.info("Banker is null");
		}
	}

	private ElKarambwansState getBankState()
	{
		if(utils.inventoryFull()){
			return DEPOSIT_ITEMS;
		}
		if(!utils.inventoryFull()){
			return WALK_TO_MIDDLE;
		}
		return UNHANDLED_STATE;
	}

	public ElKarambwansState getState()
	{
		if (timeout > 0)
		{
			return TIMEOUT;
		}
		if (!utils.inventoryContains(requiredIds))
		{
			return MISSING_ITEMS;
		}
		if (utils.isMoving(beforeLoc))
		{
			timeout = 2 + tickDelay();
			return MOVING;
		}
		if (chinBreakHandler.shouldBreak(this) && player.getWorldArea().intersectsWith(FISHING_AREA))
		{
			return HANDLE_BREAK;
		}
		if(utils.isBankOpen()){
			return getBankState();
		}
		if(client.getLocalPlayer().getAnimation()!=-1){
			return ANIMATING;
		}
		if (utils.inventoryFull())
		{
			return getKarambwanFisherState();
		}
		if (player.getWorldArea().intersectsWith(FISHING_AREA))
		{
			return FIND_NPC;
		}
		if (!utils.inventoryFull() && player.getWorldArea().intersectsWith(ZANARIS_MID_AREA)){
			return USE_FAIRY_RING_2;
		}
		return UNHANDLED_STATE;
	}

	@Subscribe
	private void onGameTick(GameTick tick)
	{
		if (!startKarambwanFisher || chinBreakHandler.isBreakActive(this))
		{
			return;
		}
		player = client.getLocalPlayer();
		if (client != null && player != null && skillLocation != null)
		{
			if (!client.isResized())
			{
				utils.sendGameMessage("client must be set to resizable");
				startKarambwanFisher = false;
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
				case FIND_NPC:
					interactFishingSpot();
					timeout = tickDelay();
					break;
				case MISSING_ITEMS:
					startKarambwanFisher = false;
					utils.sendGameMessage("Missing required items IDs: " + String.valueOf(requiredIds) + " from inventory. Stopping.");
					resetVals();
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
				case USE_FAIRY_RING_1:
					interactFairyRing(29495,3);
					timeout = tickDelay();
					break;
				case WALK_TO_MIDDLE:
					utils.walk(new WorldPoint(2398+utils.getRandomIntBetweenRange(0,3),4443+utils.getRandomIntBetweenRange(0,7),0),0,sleepDelay());
					timeout = tickDelay();
					break;
				case WALK_TO_BANK:
					utils.walk(new WorldPoint(2381+utils.getRandomIntBetweenRange(0,5),4455+utils.getRandomIntBetweenRange(0,5),0),0,sleepDelay());
					timeout = tickDelay();
					break;
				case FIND_BANK:
					openBank();
					timeout = tickDelay();
					break;
				case DEPOSIT_ITEMS:
					utils.depositAllExcept(requiredIds);
					timeout = tickDelay();
					break;
				case USE_FAIRY_RING_2:
					interactFairyRing(29560,5);
					timeout = tickDelay();
					break;
			}
		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN && startKarambwanFisher)
		{
			state = TIMEOUT;
			timeout = 2;
		}
	}

	private ElKarambwansState getKarambwanFisherState()
	{
		log.info("getting karambwan fishing state");
		if(utils.inventoryFull()){
			if(player.getWorldArea().intersectsWith(FISHING_AREA)){
				return USE_FAIRY_RING_1;
			}
			if(player.getWorldLocation().equals(ZANARIS_RING)){
				return WALK_TO_MIDDLE;
			}
			if(player.getWorldArea().intersectsWith(ZANARIS_MID_AREA)){
				return utils.findNearestNpcWithin(player.getWorldLocation(), 25, Collections.singleton(3092))==null ? WALK_TO_BANK : FIND_BANK;
			}
			if(player.getWorldArea().intersectsWith(ZANARIS_BANK_AREA)){
				return FIND_BANK;
			}
		}
		return TIMEOUT;
	}

	private void interactFairyRing(int ringID, int opcode){
		targetObject = utils.findNearestGameObjectWithin(player.getWorldLocation(), 25, ringID);
		if (targetObject != null)
		{
			targetMenu = new MenuEntry("", "", targetObject.getId(), opcode,
					targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
			utils.setMenuEntry(targetMenu);
			utils.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
		}
		else
		{
			log.info("Fairy ring is null.");
		}
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event){
		log.debug(event.toString());
	}
}
