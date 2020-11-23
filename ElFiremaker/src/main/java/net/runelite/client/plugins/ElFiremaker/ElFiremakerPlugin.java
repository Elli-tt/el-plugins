package net.runelite.client.plugins.ElFiremaker;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.queries.GameObjectQuery;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.botutils.BotUtils;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;
import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static net.runelite.client.plugins.ElFiremaker.ElFiremakerState.*;

@Extension
@PluginDependency(BotUtils.class)
@PluginDescriptor(
	name = "El Firemaker",
	description = "Makes fires for you",
	type = PluginType.SKILLING
)
@Slf4j
public class ElFiremakerPlugin extends Plugin
{

	@Inject
	private Client client;

	@Inject
	private BotUtils utils;

	@Inject
	private ConfigManager configManager;

	@Inject
	OverlayManager overlayManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ElFiremakerConfig config;

	@Inject
	private ElFiremakerOverlay overlay;

	MenuEntry targetMenu;
	Instant botTimer;
	Player player;
	boolean firstTime;
	ElFiremakerState state;
	boolean startFireMaker;
	GameObject gameObject;
	WorldPoint startTile;
	int timeout = 0;
	boolean walkAction;
	WorldArea varrockFountainArea = new WorldArea(new WorldPoint(3205,3428,0), new WorldPoint(3214,3432,0));
	int coordX;
	int coordY;
	int firemakingPath;
	GameObject targetObject;
	final Set<GameObject> fireObjects = new HashSet<>();
	final Set<Integer> requiredItems = new HashSet<>();
	boolean[] pathStates;
	WorldPoint currentLoc;
	WorldPoint beforeLoc;

	// Provides our config
	@Provides
	ElFiremakerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ElFiremakerConfig.class);
	}

	@Override
	protected void startUp()
	{
		// runs on plugin startup
		log.info("Plugin started");
		botTimer = Instant.now();
		walkAction=false;
		coordX=0;
		coordY=0;
		firstTime=true;
		firemakingPath = 1;
		startFireMaker=false;
		requiredItems.clear();
		requiredItems.add(590);
		if(!config.walk()){
			requiredItems.add(563);
			if(!config.justLaws()){
				requiredItems.add(554);
			}
		}
		pathStates = null;

		// example how to use config items
	}

	@Override
	protected void shutDown()
	{
		// runs on plugin shutdown
		log.info("Plugin stopped");
		overlayManager.remove(overlay);
		startFireMaker=false;
		fireObjects.clear();
		pathStates = null;
		requiredItems.clear();
	}

	private long sleepDelay()
	{
		return utils.randomDelay(false, 60,350,100,10);
	}

	private int tickDelay()
	{
		if(config.customTickDelays()){
			return (int) utils.randomDelay(false,config.tickDelayMin(),config.tickDelayMax(),config.tickDelayDev(),config.tickDelayTarg());
		} else {
			return (int) utils.randomDelay(false,2,3,1,3);
		}

	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (!configButtonClicked.getGroup().equalsIgnoreCase("ElFiremaker"))
		{
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton"))
		{
			if (!startFireMaker)
			{
				startUp();
				startFireMaker = true;
				targetMenu = null;
				botTimer = Instant.now();
				overlayManager.add(overlay);
				setLocation();
			} else {
				shutDown();
			}
		}
	}

	public void setLocation()
	{
		if (client != null && client.getLocalPlayer() != null && client.getGameState().equals(GameState.LOGGED_IN))
		{
			beforeLoc = client.getLocalPlayer().getWorldLocation();
			currentLoc = client.getLocalPlayer().getWorldLocation();
		}
		else
		{
			log.debug("Tried to start bot before being logged in");
			startFireMaker=false;
		}
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("ElFiremaker"))
		{
			return;
		}
		startFireMaker = false;
	}

	@Subscribe
	private void onGameTick(GameTick gameTick)
	{
		if (!startFireMaker)
		{
			return;
		}
		if (!client.isResized())
		{
			utils.sendGameMessage("client must be set to resizable");
			startFireMaker = false;
			return;
		}
		player = client.getLocalPlayer();

		if(player==null){
			state = NULL_PLAYER;
			return;
		}
		beforeLoc=currentLoc;
		currentLoc=player.getWorldLocation();
		if(player.getAnimation()!=-1){
			state = ANIMATING;
			timeout=tickDelay();
			return;
		}
		if(currentLoc.getX()!=beforeLoc.getX() ||
			currentLoc.getY()!=beforeLoc.getY()){
			state = MOVING;
			return;
		}
		if(timeout>0){
			utils.handleRun(30, 20);
			timeout--;
			return;
		}

		if(!utils.isBankOpen()) {
			if (utils.getInventorySpace() == 28 - requiredItems.size()) {
				openNearestBank();
				state = OPEN_BANK;
				timeout = tickDelay();
				return;
			}
		}
		//26185 fire id
		if(!utils.isBankOpen() && utils.inventoryFull() && player.getWorldLocation().equals(new WorldPoint(3185, 3436, 0))){
			getToVarrockSquare();
			state = WALK_SQUARE;
			timeout=tickDelay();
			return;
		}
		if (!utils.isBankOpen() && utils.inventoryFull() && !player.getWorldArea().intersectsWith(varrockFountainArea)) {
			checkFreePath();
			if(firemakingPath==0) {
				startTile = new WorldPoint(3206 + utils.getRandomIntBetweenRange(0, 3), 3430, 0);
			} else if(firemakingPath==1){
				startTile = new WorldPoint(3206 + utils.getRandomIntBetweenRange(0,7), 3429, 0);
			} else {
				startTile = new WorldPoint(3206 + utils.getRandomIntBetweenRange(0,7), 3428, 0);
			}
			if (LocalPoint.fromWorld(client,startTile) != null) {
				utils.walk(LocalPoint.fromWorld(client,startTile), 0, sleepDelay());
			}
			timeout = tickDelay();
			state = WALK_START;
			return;
		}
		if(!utils.isBankOpen()){
			if(firstTime){
				targetMenu=new MenuEntry("Use","<col=ff9040>Tinderbox",590,38,utils.getInventoryWidgetItem(590).getIndex(),9764864,false);
				utils.setMenuEntry(targetMenu);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				firstTime=false;
				state=LIGHT_FIRST;
				return;
			}
			targetMenu = new MenuEntry("Use","<col=ff9040>Tinderbox<col=ffffff> -> <col=ff9040>"+itemManager.getItemDefinition(config.logId()).getName(),config.logId(),31,utils.getInventoryWidgetItem(config.logId()).getIndex(),9764864,false);
			utils.setMenuEntry(targetMenu);
			utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
			timeout = tickDelay();
			state=LIGHT_LOG;
			return;
		}
		if(utils.inventoryFull()){
			closeBank();
			state = CLOSE_BANK;
			timeout=tickDelay();
			return;
		}
		if(utils.isBankOpen() && !utils.inventoryFull()){
			utils.withdrawAllItem(config.logId());
			state = WITHDRAW_LOGS;
			timeout=tickDelay();
			return;
		}
		state = NOT_SURE;
	}

	private void openNearestBank()
	{
		targetObject = new GameObjectQuery()
				.idEquals(34810)
				.result(client)
				.nearestTo(client.getLocalPlayer());
		if(targetObject!=null){
			targetMenu = new MenuEntry("","",targetObject.getId(),4,targetObject.getLocalLocation().getSceneX(),targetObject.getLocalLocation().getSceneY(),false);
			utils.sendGameMessage(targetMenu.toString());
			utils.setMenuEntry(targetMenu);
			utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
		} else {
			utils.sendGameMessage("bank object is null.");
		}
	}

	private Point getRandomNullPoint()
	{
		if(client.getWidget(161,34)!=null){
			Rectangle nullArea = client.getWidget(161,34).getBounds();
			return new Point ((int)nullArea.getX()+utils.getRandomIntBetweenRange(0,nullArea.width), (int)nullArea.getY()+utils.getRandomIntBetweenRange(0,nullArea.height));
		}

		return new Point(client.getCanvasWidth()-utils.getRandomIntBetweenRange(0,2),client.getCanvasHeight()-utils.getRandomIntBetweenRange(0,2));
	}

	private void closeBank()
	{
		targetMenu = new MenuEntry("Close", "", 1, 57, 11, 786434, false);
		utils.setMenuEntry(targetMenu);
		utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
	}

	private void getToVarrockSquare(){
		if(!config.walk()){
			targetMenu=new MenuEntry("Cast","<col=00ff00>Varrock Teleport</col>",1,57,-1,14286868,false);
			utils.setMenuEntry(targetMenu);
			utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
		} else {
			startTile = new WorldPoint(3196,3430,0);
			if (LocalPoint.fromWorld(client,startTile) != null) {
				utils.walk(LocalPoint.fromWorld(client,startTile), 0, sleepDelay());
			}
		}
	}

	private void checkFreePath(){
		pathStates = new boolean[]{false, false, false};
		fireObjects.clear();
		fireObjects.addAll(getLocalGameObjects(15,26185));
		for(GameObject fire : fireObjects){
			if(fire.getWorldLocation()!=null){
				if(fire.getWorldLocation().getY()==3430){
					pathStates[0]= true;
				} else if(fire.getWorldLocation().getY()==3429){
					pathStates[1]= true;
				} else if(fire.getWorldLocation().getY()==3428){
					pathStates[2]= true;
				}
			}
		}
		log.debug(Arrays.toString(pathStates));
		if(!pathStates[0]){
			firemakingPath=0;
		} else if (!pathStates[1]){
			firemakingPath=1;
		} else if (!pathStates[2]){
			firemakingPath=2;
		}
		log.debug(String.valueOf(firemakingPath));
		pathStates=null;
	}

	private java.util.List<GameObject> getLocalGameObjects(int distanceAway, int... ids)
	{
		if (client.getLocalPlayer() == null)
		{
			return new ArrayList<>();
		}
		List<GameObject> localGameObjects = new ArrayList<>();
		for(GameObject gameObject : utils.getGameObjects(ids)){
			if(gameObject.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation())<distanceAway){
				localGameObjects.add(gameObject);
			}
		}
		return localGameObjects;
	}
}