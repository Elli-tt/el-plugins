package net.runelite.client.plugins.ElTest;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.game.SpriteOverride;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.ImageUtil;
import org.pf4j.Extension;
import net.runelite.client.plugins.botutils.BotUtils;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

import static net.runelite.client.plugins.ElTest.ElTestState.*;

@Extension
@PluginDependency(BotUtils.class)
@PluginDescriptor(
		name = "El Test",
		description = "Test",
		type = PluginType.SKILLING
)
@Slf4j
public class ElTestPlugin extends Plugin implements MouseListener, KeyListener {
	@Inject
	private Client client;

	@Inject
	private BotUtils utils;

	@Inject
	private ConfigManager configManager;

	@Inject
	OverlayManager overlayManager;

	@Inject
	ItemManager itemManager;

	@Inject
	private ElTestConfig config;

	@Inject
	private ElTestOverlay overlay;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private SpriteManager spriteManager;

	int clientTickBreak = 0;
	int tickTimer;
	boolean startTest;
	ElTestState status;

	Instant botTimer;

	private Widget picker = null;
	private Widget protMelee = null;
	private Widget bankEniola = null;
	private Widget ouraniaTele = null;

	MenuEntry targetMenu;
	GameObject targetObject;

	int clientTickCounter;
	boolean clientClick;

	NPC targetNpc;

	int currentWorld;


	// Provides our config
	@Provides
	ElTestConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ElTestConfig.class);
	}

	@Override
	protected void startUp()
	{
		mouseManager.registerMouseListener(this);
		keyManager.registerKeyListener(this);
		botTimer = Instant.now();
		setValues();
		startTest=false;
		log.info("Plugin started");
		//clientThread.invoke(this::addPickerWidget);
		//clientThread.invoke(this::addMeleeWidget);
		clientThread.invoke(this::addOuraniaTeleWidget);
		clientThread.invoke(this::addBankEniolaWidget);
		currentWorld = client.getWorld();
	}

	@Override
	protected void shutDown()
	{
		mouseManager.unregisterMouseListener(this);
		keyManager.unregisterKeyListener(this);
		overlayManager.remove(overlay);
		setValues();
		startTest=false;
		log.info("Plugin stopped");
		//clientThread.invoke(this::removePickerWidget);
		//clientThread.invoke(this::removeMeleeWidget);
		clientThread.invoke(this::removeOuraniaTeleWidget);
		clientThread.invoke(this::removeBankEniolaWidget);
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (!configButtonClicked.getGroup().equalsIgnoreCase("ElTest"))
		{
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton"))
		{
			if (!startTest)
			{
				startTest = true;
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
		if (!event.getGroup().equals("ElTest"))
		{
			return;
		}
		startTest = false;
	}

	private void setValues()
	{
		clientTickCounter=-1;
		clientTickBreak=0;
		clientClick=false;
	}

	@Subscribe
	private void onClientTick(ClientTick clientTick)
	{
		clientTickCounter++;
		if(clientTickBreak>0){
			clientTickBreak--;
			return;
		}
		clientTickBreak=utils.getRandomIntBetweenRange(4,6);
	}

	@Subscribe
	private void onGameTick(GameTick gameTick)
	{

		if(client.getWorld()!=currentWorld){
			addOuraniaTeleWidget();
			addBankEniolaWidget();
			currentWorld=client.getWorld();
		}
		clientTickCounter=0;
		if (!startTest)
		{
			return;
		}
		if (!client.isResized())
		{
			utils.sendGameMessage("client must be set to resizable");
			startTest = false;
			return;
		}

		clientTickCounter=0;
		status = checkPlayerStatus();
		switch (status) {
			case ANIMATING:
			case NULL_PLAYER:
			case TICK_TIMER:
				break;
			case PUSH:
				hosidiusFavourFunction();
				break;
		}
	}

	private void addPickerWidget()
	{
		removePickerWidget();

		int x = 0, y = 0;
		Widget parent = client.getWidget(161,16);
		if (parent == null)
		{
			Widget[] roots = client.getWidgetRoots();
			parent = Stream.of(roots)
					.filter(w -> w.getType() == WidgetType.LAYER && w.getContentType() == 0 && !w.isSelfHidden()).max(Comparator.comparing((Widget w) -> w.getRelativeX() + w.getRelativeY())
							.reversed()
							.thenComparing(Widget::getId)).get();
			x = 4;
			y = 4;
		}
		picker = parent.createChild(-1, WidgetType.GRAPHIC);

		log.info("Picker is {}.{} [{}]", WidgetInfo.TO_GROUP(picker.getId()), WidgetInfo.TO_CHILD(picker.getId()), picker.getIndex());

		client.getSpriteOverrides().put(-300, ImageUtil.getImageSprite(ImageUtil.getResourceStreamFromClass(ElTestPlugin.class, "ouraniachin.png"), client));
		picker.setSpriteId(-300);
		picker.setOriginalWidth(30);
		picker.setOriginalHeight(30);
		picker.setOriginalX(parent.getWidth()-30);
		picker.setOriginalY(parent.getHeight()-30);
		picker.revalidate();
		picker.setTargetVerb("action3");
		picker.setName("button3");
		picker.setClickMask(WidgetConfig.USE_WIDGET);
		picker.setNoClickThrough(true);
	}

	private void removePickerWidget()
	{
		if (picker == null)
		{
			return;
		}

		Widget parent = picker.getParent();
		if (parent == null)
		{
			return;
		}

		Widget[] children = parent.getChildren();
		if (children == null || children.length <= picker.getIndex() || children[picker.getIndex()] != picker)
		{
			return;
		}

		children[picker.getIndex()] = null;
	}

	private void addOuraniaTeleWidget()
	{
		removeOuraniaTeleWidget();
		Widget parent = client.getWidget(161,16);
		int x = 0, y = 0;
		if (parent == null)
		{
			Widget[] roots = client.getWidgetRoots();

			parent = Stream.of(roots)
					.filter(w -> w.getType() == WidgetType.GRAPHIC && w.getContentType() == 0 && !w.isSelfHidden()).max(Comparator.comparing((Widget w) -> w.getRelativeX() + w.getRelativeY())
							.reversed()
							.thenComparing(Widget::getId)).get();
			x = 4;
			y = 4;
		}

		ouraniaTele = parent.createChild(-1, WidgetType.GRAPHIC);

		//client.getSpriteOverrides().put(-300, ImageUtil.getImageSprite(ImageUtil.getResourceStreamFromClass(ElTestPlugin.class, "zmibutton.png"), client));
		ouraniaTele.setSpriteId(SpriteID.SPELL_OURANIA_TELEPORT);
		ouraniaTele.setOriginalWidth(30);
		ouraniaTele.setOriginalHeight(30);
		ouraniaTele.setOriginalX(parent.getWidth()-30);
		ouraniaTele.setOriginalY(parent.getHeight()-65);
		ouraniaTele.revalidate();
		ouraniaTele.setTargetVerb("action2");
		ouraniaTele.setName("button2");
		ouraniaTele.setClickMask(WidgetConfig.USE_WIDGET);
		ouraniaTele.setNoClickThrough(true);
	}

	private void removeOuraniaTeleWidget()
	{
		if (ouraniaTele == null)
		{
			return;
		}

		Widget parent = ouraniaTele.getParent();
		if (parent == null)
		{
			return;
		}

		Widget[] children = parent.getChildren();
		if (children == null || children.length <= ouraniaTele.getIndex() || children[ouraniaTele.getIndex()] != ouraniaTele)
		{
			return;
		}

		children[ouraniaTele.getIndex()] = null;
	}

	private void addBankEniolaWidget()
	{
		removeBankEniolaWidget();
		Widget parent = client.getWidget(161,16);
		int x = 0, y = 0;
		if (parent == null)
		{
			Widget[] roots = client.getWidgetRoots();

			parent = Stream.of(roots)
					.filter(w -> w.getType() == WidgetType.GRAPHIC && w.getContentType() == 0 && !w.isSelfHidden()).max(Comparator.comparing((Widget w) -> w.getRelativeX() + w.getRelativeY())
							.reversed()
							.thenComparing(Widget::getId)).get();
			x = 4;
			y = 4;
		}

		bankEniola = parent.createChild(-1, WidgetType.GRAPHIC);

		client.getSpriteOverrides().put(-301, ImageUtil.getImageSprite(ImageUtil.getResourceStreamFromClass(ElTestPlugin.class, "eniola.png"), client));
		bankEniola.setSpriteId(-301);
		bankEniola.setOriginalWidth(30);
		bankEniola.setOriginalHeight(30);
		bankEniola.setOriginalX(parent.getWidth()-30);
		bankEniola.setOriginalY(parent.getHeight()-100);
		bankEniola.revalidate();
		bankEniola.setTargetVerb("action1");
		bankEniola.setName("button1");
		bankEniola.setClickMask(WidgetConfig.USE_WIDGET);
		bankEniola.setNoClickThrough(true);
	}

	private void removeBankEniolaWidget()
	{
		if (bankEniola == null)
		{
			return;
		}

		Widget parent = bankEniola.getParent();
		if (parent == null)
		{
			return;
		}

		Widget[] children = parent.getChildren();
		if (children == null || children.length <= bankEniola.getIndex() || children[bankEniola.getIndex()] != bankEniola)
		{
			return;
		}

		children[bankEniola.getIndex()] = null;
	}

	/*private void addMeleeWidget()
	{
		removeMeleeWidget();


		Widget parent = client.getWidget(161,16);
		int x = 0, y = 0;
		if (parent == null)
		{
			Widget[] roots = client.getWidgetRoots();

			parent = Stream.of(roots)
					.filter(w -> w.getType() == WidgetType.GRAPHIC && w.getContentType() == 0 && !w.isSelfHidden()).max(Comparator.comparing((Widget w) -> w.getRelativeX() + w.getRelativeY())
							.reversed()
							.thenComparing(Widget::getId)).get();
			x = 4;
			y = 4;
		}

		protMelee = parent.createChild(-1, WidgetType.GRAPHIC);

		//client.getSpriteOverrides().put(-300, ImageUtil.getImageSprite(ImageUtil.getResourceStreamFromClass(ElTestPlugin.class, "zmibutton.png"), client));
		protMelee.setSpriteId(SpriteID.PRAYER_PROTECT_FROM_MELEE);
		protMelee.setOriginalWidth(30);
		protMelee.setOriginalHeight(30);
		protMelee.setOriginalX(parent.getWidth()-50);
		protMelee.setOriginalY(parent.getHeight()-100);
		protMelee.revalidate();
		protMelee.setTargetVerb("ProtPray");
		protMelee.setName("Melee");
		protMelee.setClickMask(WidgetConfig.USE_WIDGET | WidgetConfig.DRAG);
		protMelee.setNoClickThrough(true);
	}

	private void removeMeleeWidget()
	{
		if (protMelee == null)
		{
			return;
		}

		Widget parent = protMelee.getParent();
		if (parent == null)
		{
			return;
		}

		Widget[] children = parent.getChildren();
		if (children == null || children.length <= protMelee.getIndex() || children[protMelee.getIndex()] != protMelee)
		{
			return;
		}

		children[protMelee.getIndex()] = null;
	}

	 */

	

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event)
	{
		log.debug(event.toString());
		if(targetMenu!=null){
			event.consume();
			client.invokeMenuAction(targetMenu.getOption(),targetMenu.getTarget(),targetMenu.getIdentifier(),targetMenu.getOpcode(),targetMenu.getParam0(),targetMenu.getParam1());
			targetMenu=null;
		}
		log.info(event.toString());
		if(event.getOption().equals("action3") && event.getTarget().equals("button3")){
			event.consume();
			targetNpc = utils.findNearestNpc(1560);
			if(targetNpc!=null){
				client.invokeMenuAction("","",targetNpc.getIndex(),11,0,0);
			}
			return;
		} else if (event.getOption().equals("action2") && event.getTarget().equals("button2")){
			event.consume();
			targetNpc = utils.findNearestNpc(1560);
			if(targetNpc!=null){
				client.invokeMenuAction("","",targetNpc.getIndex(),11,0,0);
			}
			return;
		} else if (event.getOption().equals("action1") && event.getTarget().equals("button1")){
			event.consume();
			targetObject = utils.findNearestGameObject(26707);
			if(targetObject!=null){
				client.invokeMenuAction("","",targetObject.getId(),3,targetObject.getSceneMinLocation().getX(),targetObject.getSceneMinLocation().getY());
			}
		}
	}

	private long sleepDelay()
	{
		return utils.randomDelay(false, 60, 350, 100, 10);
	}

	private int tickDelay()
	{
		return (int) utils.randomDelay(false,1, 3, 2, 2);
	}

	private ElTestState checkPlayerStatus()
	{
		Player player = client.getLocalPlayer();
		if(player==null){
			return NULL_PLAYER;
		}
		if(player.getPoseAnimation()!=808){
			tickTimer=2;
			return MOVING;
		}

		if(player.getAnimation()!=-1){
			tickTimer=2;
			return ANIMATING;
		}
		if(tickTimer>0){
			tickTimer--;
			return TICK_TIMER;
		}
		return PUSH;
	}

	private Point getRandomNullPoint()
	{
		if(client.getWidget(161,34)!=null){
			Rectangle nullArea = client.getWidget(161,34).getBounds();
			return new Point ((int)nullArea.getX()+utils.getRandomIntBetweenRange(0,nullArea.width), (int)nullArea.getY()+utils.getRandomIntBetweenRange(0,nullArea.height));
		}

		return new Point(client.getCanvasWidth()-utils.getRandomIntBetweenRange(0,2),client.getCanvasHeight()-utils.getRandomIntBetweenRange(0,2));
	}

	private int checkRunEnergy()
	{
		try{
			return client.getEnergy();
		} catch (Exception ignored) {

		}
		return 0;
	}

	private int checkHitpoints()
	{
		try{
			return client.getBoostedSkillLevel(Skill.HITPOINTS);
		} catch (Exception e) {
			return 0;
		}
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent mouseEvent) {
		log.info("click"+String.valueOf(clientTickCounter));
		return mouseEvent;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent mouseEvent) {
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent mouseEvent) {
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent mouseEvent) {
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent mouseEvent) {
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent mouseEvent) {
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent mouseEvent) {
		return mouseEvent;
	}

	@Override
	public void keyTyped(KeyEvent keyEvent) {
		log.info("key typed + " + keyEvent.getID());
	}

	@Override
	public void keyPressed(KeyEvent keyEvent) {
		log.info("key released + " + keyEvent.getID());
	}

	@Override
	public void keyReleased(KeyEvent keyEvent) {
		log.info("key released + " + keyEvent.getID());
		log.info("key char + " + keyEvent.getKeyChar());
	}

	private void hosidiusFavourFunction(){
		for(NPC npc : utils.getNPCs(6924)){
			log.info(npc.getWorldLocation().toString());
			if(npc.getWorldLocation().getY()==3551){
				if(npc.getWorldLocation().getX()==1763){
					log.info("checking");
					if(!client.getLocalPlayer().getWorldLocation().equals(new WorldPoint(1762,3552,0))){
						utils.walk(new WorldPoint(1762,3552,0),0,sleepDelay());
						return;
					} else {
						utils.setMenuEntry(null);
						targetMenu = new MenuEntry("","",npc.getIndex(),9,0,0,false);
						utils.delayMouseClick(npc.getConvexHull().getBounds(),sleepDelay());
						return;
					}
				} else if(npc.getWorldLocation().getX()==1777){
					if(!client.getLocalPlayer().getWorldLocation().equals(new WorldPoint(1780,3552,0))){
						utils.walk(new WorldPoint(1780,3552,0),0,sleepDelay());
						return;
					} else {
						utils.setMenuEntry(null);
						targetMenu = new MenuEntry("","",npc.getIndex(),9,0,0,false);
						utils.delayMouseClick(npc.getConvexHull().getBounds(),sleepDelay());
						return;
					}
				} else {
					utils.setMenuEntry(null);
					targetMenu = new MenuEntry("","",npc.getIndex(),9,0,0,false);
					utils.delayMouseClick(npc.getConvexHull().getBounds(),sleepDelay());
				}
			}
		}
		for(NPC npc : utils.getNPCs(6925)){
			if(npc.getWorldLocation().getY()==3551){
				utils.setMenuEntry(null);
				targetMenu = new MenuEntry("","",npc.getIndex(),9,0,0,false);
				utils.delayMouseClick(npc.getConvexHull().getBounds(),sleepDelay());
			}
		}
	}
}
