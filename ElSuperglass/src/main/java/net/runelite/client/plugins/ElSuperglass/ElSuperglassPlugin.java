package net.runelite.client.plugins.ElSuperglass;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.queries.GameObjectQuery;
import net.runelite.api.queries.TileQuery;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;
import net.runelite.client.plugins.botutils.BotUtils;
import java.awt.*;
import java.time.Instant;

import static net.runelite.client.plugins.botutils.Banks.BANK_SET;

@Extension
@PluginDependency(BotUtils.class)
@PluginDescriptor(
	name = "El Superglass",
	description = "Makes superglass.",
	type = PluginType.SKILLING
)
@Slf4j
public class ElSuperglassPlugin extends Plugin
{
	// Injects our config
	@Inject
	private ElSuperglassConfig config;

	@Inject
	private Client client;

	@Inject
	private BotUtils utils;

	@Inject
	OverlayManager overlayManager;

	@Inject
	private ElSuperglassOverlay overlay;

	//plugin data
	MenuEntry targetMenu;
	int clientTickBreak = 0;
	int withdrawClickCount = 0;
	int tickTimer;
	String status = "UNKNOWN";
	boolean clientTickBanking;
	boolean startSuperglassMaker;

	//overlay data
	int seaweedStart = -1;
	int seaweedLeft = -1;
	int bucketsOfSandStart = -1;
	int bucketsOfSandLeft = -1;
	int glassStart = -1;
	int glassLeft = -1;
	Instant botTimer;

	// Provides our config
	@Provides
	ElSuperglassConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ElSuperglassConfig.class);
	}

	@Override
	protected void startUp()
	{
		clientTickBreak=0;
		withdrawClickCount=-1;
		clientTickBanking = false;
		botTimer = Instant.now();
		log.info("Plugin started");
		startSuperglassMaker=false;

	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		log.info("Plugin stopped");
		startSuperglassMaker=false;
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (!configButtonClicked.getGroup().equalsIgnoreCase("ElSuperglass"))
		{
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton"))
		{
			if (!startSuperglassMaker)
			{
				startSuperglassMaker = true;
				targetMenu = null;
				botTimer = Instant.now();
				overlayManager.add(overlay);
			} else {
				shutDown();
			}
		}
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("ElSuperglass"))
		{
			return;
		}
		startSuperglassMaker = false;
	}

	@Subscribe
	private void onGameTick(GameTick gameTick)
	{
		if (!startSuperglassMaker)
		{
			return;
		}
		if (!client.isResized())
		{
			utils.sendGameMessage("client must be set to resizable");
			startSuperglassMaker = false;
			return;
		}

		status = checkPlayerStatus();
		updateLeftValues();
		log.info(status);

		switch (status) {
			case "ANIMATING":
			case "TICK_TIMER":
				break;
			case "PICKING_UP_GROUND_ITEMS":
				getGlassUnderneathPlayer();
				break;
			case "DEPOSITING_FULL_INVENTORY":
				if(utils.isBankOpen()){
					targetMenu = new MenuEntry("Deposit inventory","",1,57,-1,786473,false);
					utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
					utils.pressKey(27);
					tickTimer+=tickDelay();
				} else {
					openNearestBank();
					tickTimer+=tickDelay();
				}
				break;
			case  "CASTING_SUPERGLASS_MAKE":
				targetMenu = new MenuEntry("Cast", "<col=00ff00>Superglass Make</col>",1,57,-1,14286965,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				tickTimer+=3+tickDelay();
				break;
			case "BANKING_FOR_SUPPLIES":
				if(!utils.isBankOpen()){
					openNearestBank();
					tickTimer+=tickDelay();
				}
				if(clientTickBanking){
					return;
				}
				withdrawClickCount=0;
				clientTickBanking=true;
				tickTimer+=tickDelay();
				break;
		}
	}

	@Subscribe
	private void onClientTick(ClientTick gameTick)
	{
		if(!clientTickBanking){
			return;
		}
		if(clientTickBreak>0){
			clientTickBreak--;
			return;
		}
		clientTickBreak+=(utils.getRandomIntBetweenRange(15,20)+config.clientTickDelay());



		if(utils.isBankOpen()){
			if(withdrawClickCount==0){
				targetMenu = new MenuEntry ("Deposit inventory","",1,57,-1,786473,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				withdrawClickCount++;
				return;
			} else if(withdrawClickCount>0 && withdrawClickCount<4) {
				if (utils.bankContains("Giant seaweed")) {
					targetMenu = new MenuEntry("Withdraw-1", "Withdraw-1", 1, 57, utils.getBankItemWidget(21504).getIndex(), 786444, false);
					utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
					withdrawClickCount++;
					return;
				}
			} else if(withdrawClickCount==4) {
				if (utils.bankContains("Bucket of sand")) {
					targetMenu = new MenuEntry("Withdraw-18", "<col=ff9040>Bucket of sand</col>", 5, 57, utils.getBankItemWidget(1783).getIndex(), 786444, false);
					utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
					withdrawClickCount++;
					return;
				}
			} else if(withdrawClickCount==5){
				utils.pressKey(27);
				withdrawClickCount=-1;
				clientTickBanking=false;
				tickTimer+=tickDelay();
				return;
			}
		}

	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event)
	{
		log.debug(event.toString());
		if(targetMenu!=null){
			event.consume();
			client.invokeMenuAction(targetMenu.getOption(), targetMenu.getTarget(), targetMenu.getIdentifier(), targetMenu.getOpcode(),
					targetMenu.getParam0(), targetMenu.getParam1());
			targetMenu = null;
		}

	}

	private long sleepDelay()
	{
		return utils.randomDelay(false, 60, 350, 100, 100);
	}

	private int tickDelay()
	{
		return (int) utils.randomDelay(false,1, 3, 1, 2);
	}

	private String checkPlayerStatus()
	{
		if(tickTimer>0)
		{
			tickTimer--;
			return "TICK_TIMER";
		}
		else if(groundItemsUnderneathPlayer() && config.pickupExtraGlass())
		{
			if(utils.inventoryFull()){
				return "DEPOSITING_FULL_INVENTORY";
			} else {
				return "PICKING_UP_GROUND_ITEMS";
			}
		}
		else if(utils.inventoryContains(21504) && utils.getInventoryItemCount(21504,false)==3 &&
				utils.inventoryContains(1783) && utils.getInventoryItemCount(1783,false)==18){
				return "CASTING_SUPERGLASS_MAKE";
		}
		else {
			return "BANKING_FOR_SUPPLIES";
		}
	}

	private boolean groundItemsUnderneathPlayer()
	{
		log.info("groundItemsUnderneathPlayer called.");
		for(Tile tile : new TileQuery().isWithinDistance(client.getLocalPlayer().getWorldLocation(),0).result(client)) {
			if(tile.getGroundItems()!=null){
				for(TileItem tileItem : tile.getGroundItems()){
					if(tileItem.getId()==1775){
						return true;
					}
				}
			}
		}
		return false;
	}

	private void getGlassUnderneathPlayer()
	{
		log.info("getGlassUnderneathPlayer called.");
		for(Tile tile : new TileQuery().isWithinDistance(client.getLocalPlayer().getWorldLocation(),0).result(client)) {
			if(tile.getGroundItems()!=null){
				for(TileItem tileItem : tile.getGroundItems()){
					if(tileItem.getId()==1775){
						targetMenu = new MenuEntry ("Take", "<col=ff9040>Molten glass",1775,20,client.getLocalPlayer().getLocalLocation().getSceneX(),client.getLocalPlayer().getLocalLocation().getSceneY(),false);
						utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
						return;
					}
				}
			}
		}
	}

	private void openNearestBank()
	{
		if(config.grandExchange()){
			NPC targetNPC = utils.findNearestNpc("Banker");
			targetMenu=new MenuEntry("Bank","<col=ffff00>Banker",targetNPC.getIndex(),11,0,0,false);
			utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
			return;
		}
		GameObject targetObject = new GameObjectQuery()
				.idEquals(BANK_SET)
				.result(client)
				.nearestTo(client.getLocalPlayer());
		if(targetObject!=null){
			targetMenu = new MenuEntry("","",targetObject.getId(),4,targetObject.getLocalLocation().getSceneX(),targetObject.getLocalLocation().getSceneY(),false);
			utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
		}
	}

	private Point getRandomNullPoint()
	{
		if(client.getWidget(161,34)!=null){
			Rectangle nullArea = client.getWidget(161,34).getBounds();
			return new Point ((int)nullArea.getX()+utils.getRandomIntBetweenRange(0,nullArea.width), (int)nullArea.getY()+utils.getRandomIntBetweenRange(0,nullArea.height));
		} else {
			return new Point(0,0);
		}
	}

	private void updateLeftValues()
	{
		if (utils.isBankOpen()) {
			ItemContainer bankItemContainer = client.getItemContainer(InventoryID.BANK);

			for (Item item : bankItemContainer.getItems()) {
				if (item.getId() == 21504) {
					if(seaweedLeft==-1){
						seaweedStart=item.getQuantity();
					}
					seaweedLeft = item.getQuantity();
				} else if (item.getId() == 1783) {
					if(bucketsOfSandLeft==-1){
						bucketsOfSandStart=item.getQuantity();
					}
					bucketsOfSandLeft = item.getQuantity();
				} else if (item.getId() == 1775) {
					if(glassLeft==-1){
						glassStart=item.getQuantity();
					}
					glassLeft = item.getQuantity();
				}
			}
		}
	}
}
