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
package net.runelite.client.plugins.ElGemMine;

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
import net.runelite.api.queries.TileQuery;
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
import static net.runelite.client.plugins.ElGemMine.ElGemMineState.*;


@Extension
@PluginDependency(BotUtils.class)
@PluginDescriptor(
	name = "El Gem Mine",
	enabledByDefault = false,
	description = "Mines gems.",
	tags = {"mining, mine, gems, el"},
	type = PluginType.SKILLING
)
@Slf4j
public class ElGemMinePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ElGemMineConfiguration config;

	@Inject
	private BotUtils utils;

	@Inject
	private ConfigManager configManager;

	@Inject
	PluginManager pluginManager;

	@Inject
	OverlayManager overlayManager;

	@Inject
	private ElGemMineOverlay overlay;

	@Inject
	private ChinBreakHandler chinBreakHandler;


	ElGemMineState state;
	GameObject targetObject;
	NPC targetNPC;
	MenuEntry targetMenu;
	WorldPoint skillLocation;
	Instant botTimer;
	LocalPoint beforeLoc;
	Player player;
	Rectangle altRect = new Rectangle(-100,-100, 10, 10);

	List<Integer> REQUIRED_ITEMS = new ArrayList<>();

	int timeout = 0;
	int teaksCut;
	long sleepLength;
	boolean startGemMine;
	int oldInventCount = -1;
	int gemMineProgress;


	@Provides
	ElGemMineConfiguration provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ElGemMineConfiguration.class);
	}

	@Override
	protected void startUp()
	{
		resetVals();
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
		startGemMine = false;
		oldInventCount=-1;
		gemMineProgress=0;
		REQUIRED_ITEMS = List.of(5070,5071,5072,5073,5074,5075,7413,13653,19712,19714,19716,19718,22798,22800,23127);
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (!configButtonClicked.getGroup().equalsIgnoreCase("ElGemMine"))
		{
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton"))
		{
			if (!startGemMine)
			{
				startGemMine = true;
				chinBreakHandler.startPlugin(this);
				state = null;
				targetMenu = null;
				botTimer = Instant.now();
				setLocation();
				overlayManager.add(overlay);
				gemMineProgress=0;
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
		if (!event.getGroup().equals("ElGemMine"))
		{
			return;
		}
		startGemMine = false;
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

	private void openBank()
	{
		targetObject = utils.findNearestGameObject(10530);
		if (targetObject != null)
		{
			targetMenu=new MenuEntry("","",targetObject.getId(),3,targetObject.getSceneMinLocation().getX(),targetObject.getSceneMinLocation().getY(),false);
			utils.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
		}
		else
		{
			log.info("Banker is null");
		}
	}

	private ElGemMineState getBankState()
	{
		if(utils.inventoryFull()){
			return DEPOSIT_ITEMS;
		}
		if(!utils.inventoryFull()){
			return getGemMineState();
		}
		return UNHANDLED_STATE;
	}

	public ElGemMineState getState()
	{
		if (timeout > 0)
		{
			return TIMEOUT;
		}
		else if (utils.isMoving(beforeLoc))
		{
			timeout = 2 + tickDelay();
			return MOVING;
		}
		else if (chinBreakHandler.shouldBreak(this))
		{
			return HANDLE_BREAK;
		}
		else if(client.getWidget(192,4)!=null && !client.getWidget(192,4).isHidden()){
			return getBankState();
		}
		else if(client.getLocalPlayer().getAnimation()!=-1){
			return ANIMATING;
		}
		else {
			return getGemMineState();
		}
	}

	@Subscribe
	private void onGameTick(GameTick tick)
	{
		if (!startGemMine || chinBreakHandler.isBreakActive(this))
		{
			return;
		}
		player = client.getLocalPlayer();
		if (client != null && player != null && skillLocation != null)
		{
			if (!client.isResized())
			{
				utils.sendGameMessage("client must be set to resizable");
				startGemMine = false;
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
				case HANDLE_BREAK:
					chinBreakHandler.startBreak(this);
					timeout = 10;
					break;
				case ANIMATING:
				case MOVING:
					utils.handleRun(30, 20);
					timeout = tickDelay();
					break;
				case FIND_BANK:
					openBank();
					timeout = tickDelay();
					break;
				case DEPOSIT_ITEMS:
					targetMenu = new MenuEntry("","",1,57,-1,12582916,false);
					utils.delayMouseClick(client.getWidget(192,4).getBounds(),sleepDelay());
					timeout = tickDelay();
					break;
			}
		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN && startGemMine)
		{
			state = TIMEOUT;
			timeout = 2;
		}
	}

	private ElGemMineState getGemMineState()
	{
		timeout=tickDelay();
		switch(utils.getInventorySpace()){
			case 28:
				mineRock(11380,2839,9381);
				break;
			case 27:
				mineRock(11380,2837,9380);
				break;
			case 26:
				mineRock(11380,2838,9381);
				break;
			case 25:
				mineRock(11380,2832,9381);
				break;
			case 24:
				mineRock(11381,2833,9380);
				break;
			case 23:
				mineRock(11380,2831,9382);
				break;
			case 22:
				mineRock(11380,2829,9384);
				break;
			case 21:
				mineRock(11380,2831,9387);
				break;
			case 20:
				mineRock(11380,2831,9386);
				break;
			case 19:
				mineRock(11381,2827,9386);
				break;
			case 18:
				mineRock(11380,2826,9389);
				break;
			case 17:
				mineRock(11380,2827,9388);
				break;
			case 16:
				mineRock(11380,2827,9391);
				break;
			case 15:
				mineRock(11380,2826,9390);
				break;
			case 14:
				mineRock(11380,2828,9392);
				break;
			case 13:
				mineRock(11380,2833,9391);
				break;
			case 12:
				mineRock(11380,2831,9396);
				break;
			case 11:
				mineRock(11381,2832,9399);
				break;
			case 10:
				mineRock(11380,2833,9398);
				break;
			case 9:
				mineRock(11380,2835,9398);
				break;
			case 8:
				mineRock(11380,2837,9397);
				break;
			case 7:
				mineRock(11380,2836,9398);
				break;
			case 6:
				mineRock(11380,2835,9392);
				break;
			case 5:
				mineRock(11380,2836,9392);
				break;
			case 4:
				mineRock(11380,2844,9391);
				break;
			case 3:
				mineRock(11380,2843,9392);
				break;
			case 2:
				mineRock(11380,2846,9390);
				break;
			case 1:
				mineRock(11380,2845,9391);
				break;
			case 0:
				openBank();
				return FIND_BANK;
		}
		return MINING_ROCK;
	}

	private void mineRock(int id, int x, int y){
		LocalPoint local = LocalPoint.fromWorld(client,x,y);
		targetMenu = new MenuEntry ("Mine", "<col=ff9040>Rocks",id,3,local.getSceneX(),local.getSceneY(),false);
		utils.delayMouseClick(new Point(0,0),sleepDelay());
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event){
		log.debug(event.toString());
		if(targetMenu!=null){
			event.consume();
			client.invokeMenuAction(targetMenu.getOption(),targetMenu.getOption(),targetMenu.getIdentifier(),targetMenu.getOpcode(),targetMenu.getParam0(),targetMenu.getParam1());
			targetMenu=null;
		}
	}

	@Subscribe
	private void onItemContainerChanged(ItemContainerChanged event){
		if (event.getContainerId() != 93 || !startGemMine)
		{
			return;
		} else {
			if(oldInventCount==-1){
				event.getItemContainer().count(6333);
			}
			if(event.getItemContainer().count(6333)>oldInventCount){
				teaksCut++;
			}
			oldInventCount=event.getItemContainer().count(6333);
		}
	}

	private boolean checkForGroundItems()
	{
		for(Tile tile : new TileQuery().isWithinDistance(client.getLocalPlayer().getWorldLocation(),10).result(client)) {
			if(tile.getGroundItems()!=null){
				for(TileItem tileItem : tile.getGroundItems()){
					if(REQUIRED_ITEMS.contains(tileItem.getId())){
						targetMenu = new MenuEntry ("Take", "<col=ff9040>",tileItem.getId(),20,tileItem.getTile().getX(),tileItem.getTile().getY(),false);
						utils.delayMouseClick(new Point(0,0),sleepDelay());
						return true;
					}
				}
			}
		}
		return false;
	}
}
