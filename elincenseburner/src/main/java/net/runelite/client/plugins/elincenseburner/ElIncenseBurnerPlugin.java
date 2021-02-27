package net.runelite.client.plugins.elincenseburner;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.events.*;
import net.runelite.api.widgets.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import org.jetbrains.annotations.NotNull;
import org.pf4j.Extension;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

import static net.runelite.client.plugins.elincenseburner.ElIncenseBurnerState.*;

@Extension
@PluginDescriptor(
		name = "El Splash",
		description = "Splash plugin"
)
@Slf4j
public class ElIncenseBurnerPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	@Inject
	OverlayManager overlayManager;

	@Inject
	ItemManager itemManager;

	@Inject
	private ElIncenseBurnerConfig config;

	@Inject
	private ElIncenseBurnerOverlay overlay;

	@Inject
	ExecutorService executorService;

	int clientTickBreak = 0;
	int tickTimer;
	boolean startIncenseBurner;
	ElIncenseBurnerState status;

	protected static final java.util.Random random = new java.util.Random();

	Instant botTimer;

	MenuEntry targetMenu;
	GameObject targetObject;

	int afkTimer = -1;

	NPC targetNpc;


	// Provides our config
	@Provides
	ElIncenseBurnerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ElIncenseBurnerConfig.class);
	}

	@Override
	protected void startUp()
	{
		botTimer = Instant.now();
		setValues();
		startIncenseBurner=false;
		log.info("Plugin started");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		setValues();
		startIncenseBurner=false;
		log.info("Plugin stopped");
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (!configButtonClicked.getGroup().equalsIgnoreCase("ElIncenseBurner"))
		{
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton"))
		{
			if (!startIncenseBurner)
			{
				startIncenseBurner = true;
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
		if (!event.getGroup().equals("ElIncenseBurner"))
		{
			return;
		}
		startIncenseBurner = false;
	}

	private void setValues()
	{
		afkTimer=getRandomIntBetweenRange(config.minAFK(),config.maxAFK());
	}

	@Subscribe
	private void onGameTick(GameTick gameTick)
	{
		targetMenu=null;
		if (!startIncenseBurner)
		{
			return;
		}
		afkTimer--;
		status = checkPlayerStatus();
		switch (status) {
			case ANIMATING:
			case NULL_PLAYER:
			case TICK_TIMER:
			case SPLASHING:
				break;
			case ATTACKING:
				attackNPC();
				afkTimer=getRandomIntBetweenRange(config.minAFK(),config.maxAFK());
				break;
		}
	}

	private void attackNPC(){
		targetMenu = new MenuEntry("Attack","NPC",config.NPCIDX(),10,0,0,false);
		delayMouseClick(new Point(0,0),sleepDelay());
	}
	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event)
	{
		log.debug(event.toString());
		if(targetMenu!=null){
			event.consume();
			client.invokeMenuAction(targetMenu.getOption(),targetMenu.getTarget(),targetMenu.getIdentifier(),targetMenu.getOpcode(),targetMenu.getParam0(),targetMenu.getParam1());
			targetMenu=null;
		}
	}

	private long sleepDelay()
	{
		return randomDelay(false, 60, 350, 5, 40);
	}

	private int tickDelay()
	{
		return (int) randomDelay(false,1, 3, 2, 2);
	}

	private ElIncenseBurnerState checkPlayerStatus()
	{
		Player player = client.getLocalPlayer();
		if(player==null){
			return NULL_PLAYER;
		}
		else if(client.getWidget(WidgetInfo.LEVEL_UP)!=null && !client.getWidget(WidgetInfo.LEVEL_UP).isHidden()){
			return ATTACKING;
		}
		else if(client.getWidget(WidgetInfo.LEVEL_UP_LEVEL)!=null && !client.getWidget(WidgetInfo.LEVEL_UP_LEVEL).isHidden()){
			return ATTACKING;
		}
		else if(client.getWidget(WidgetInfo.LEVEL_UP_SKILL)!=null && !client.getWidget(WidgetInfo.LEVEL_UP_SKILL).isHidden()){
			return ATTACKING;
		}
		else if(afkTimer==0){
			return ATTACKING;
		}
		else if(player.getAnimation()==1162){
			tickTimer=5;
			return SPLASHING;
		}
		else if(player.getPoseAnimation()!=808 && player.getPoseAnimation()!=813){
			tickTimer=2;
			return MOVING;
		}
		else if(player.getAnimation()!=-1){
			tickTimer=2;
			return ANIMATING;
		}
		else if(tickTimer>0){
			tickTimer--;
			return TICK_TIMER;
		}
		else if(player.getAnimation()==-1){
			return ATTACKING;
		}
		return UNKNOWN;
	}

	private Point getRandomNullPoint()
	{
		if(client.getWidget(161,34)!=null){
			Rectangle nullArea = client.getWidget(161,34).getBounds();
			return new Point ((int)nullArea.getX()+getRandomIntBetweenRange(0,nullArea.width), (int)nullArea.getY()+getRandomIntBetweenRange(0,nullArea.height));
		}

		return new Point(client.getCanvasWidth()-getRandomIntBetweenRange(0,2),client.getCanvasHeight()-getRandomIntBetweenRange(0,2));
	}

	public int getRandomIntBetweenRange(int min, int max)
	{
		return ThreadLocalRandom.current().nextInt(min, max + 1);
	}

	//Ganom's function, generates a random number allowing for curve and weight
	public long randomDelay(boolean weightedDistribution, int min, int max, int deviation, int target)
	{
		if (weightedDistribution)
		{
			/* generate a gaussian random (average at 0.0, std dev of 1.0)
			 * take the absolute value of it (if we don't, every negative value will be clamped at the minimum value)
			 * get the log base e of it to make it shifted towards the right side
			 * invert it to shift the distribution to the other end
			 * clamp it to min max, any values outside of range are set to min or max */
			return (long) clamp((-Math.log(Math.abs(random.nextGaussian()))) * deviation + target, min, max);
		}
		else
		{
			/* generate a normal even distribution random */
			return (long) clamp(Math.round(random.nextGaussian() * deviation + target), min, max);
		}
	}

	private double clamp(double val, int min, int max)
	{
		return Math.max(min, Math.min(max, val));
	}

	public void delayMouseClick(Point point, long delay)
	{
		executorService.submit(() ->
		{
			try
			{
				sleep(delay);
				handleMouseClick(point);
			}
			catch (RuntimeException e)
			{
				e.printStackTrace();
			}
		});
	}

	public void handleMouseClick(Point point)
	{
		assert !client.isClientThread();
		final int viewportHeight = client.getViewportHeight();
		final int viewportWidth = client.getViewportWidth();

		if (point.getX() > viewportWidth || point.getY() > viewportHeight || point.getX() < 0 || point.getY() < 0)
		{
			Point rectPoint = new Point(client.getCenterX() + getRandomIntBetweenRange(-100, 100), client.getCenterY() + getRandomIntBetweenRange(-100, 100));
			click(rectPoint);
			return;
		}
			click(point);
	}

	public void click(Point point)
	{
		assert !client.isClientThread();

		if (client.isStretchedEnabled())
		{
			final Dimension stretched = client.getStretchedDimensions();
			final Dimension real = client.getRealDimensions();
			final double width = (stretched.width / real.getWidth());
			final double height = (stretched.height / real.getHeight());
			point = new Point((int) (point.getX() * width), (int) (point.getY() * height));
		}
		mouseEvent(MouseEvent.MOUSE_PRESSED, point);
		mouseEvent(MouseEvent.MOUSE_RELEASED, point);
		mouseEvent(MouseEvent.MOUSE_CLICKED, point);
	}

	private void mouseEvent(int id, @NotNull Point point)
	{
		MouseEvent e = new MouseEvent(
				client.getCanvas(), id,
				System.currentTimeMillis(),
				0, point.getX(), point.getY(),
				1, false, 1
		);

		client.getCanvas().dispatchEvent(e);
	}

	public void sleep(long toSleep)
	{
		try
		{
			long start = System.currentTimeMillis();
			Thread.sleep(toSleep);

			// Guarantee minimum sleep
			long now;
			while (start + toSleep > (now = System.currentTimeMillis()))
			{
				Thread.sleep(start + toSleep - now);
			}
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
}
