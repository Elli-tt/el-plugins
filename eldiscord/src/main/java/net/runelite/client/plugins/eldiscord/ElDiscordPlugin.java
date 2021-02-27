package net.runelite.client.plugins.eldiscord;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;
import net.runelite.client.plugins.elutils.ElUtils;
import net.runelite.client.plugins.loottracker.LootTrackerPlugin;

import java.awt.*;
import java.util.List;
import com.google.common.base.Strings;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import javax.imageio.ImageIO;
import net.runelite.api.ItemComposition;
import net.runelite.api.NPC;
import net.runelite.api.util.Text;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.*;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.ImageCapture;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.client.util.WildcardMatcher;
import static net.runelite.http.api.RuneLiteAPI.GSON;
import net.runelite.http.api.loottracker.LootRecordType;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import net.runelite.client.util.HotkeyListener;

import net.runelite.client.plugins.elbreakhandler.ElBreakHandler;

@Extension
@PluginDependency(ElUtils.class)
@PluginDescriptor(
		name = "El Discord",
		description = "Sends messages through Discord"
)
@Slf4j
public class ElDiscordPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ElUtils utils;

	@Inject
	private ConfigManager configManager;

	@Inject
	OverlayManager overlayManager;

	@Inject
	ItemManager itemManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ElDiscordConfig config;

	@Inject
	private ElBreakHandler elBreakHandler;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private ImageCapture imageCapture;

	@Inject
	private DrawManager drawManager;

	private List<String> lootNpcs;
	private GameState previousGameState = GameState.LOADING;
	private String previousLevelUp = "";
	private int idleTime = 0;

	private static String itemImageUrl(int itemId)
	{
		return "https://static.runelite.net/cache/item/icon/" + itemId + ".png";
	}

	@Override
	protected void startUp()
	{
		keyManager.registerKeyListener(hotkeyListener);
		lootNpcs = Collections.emptyList();
		idleTime = 0;
	}

	@Override
	protected void shutDown()
	{
		keyManager.unregisterKeyListener(hotkeyListener);
	}

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.hotkeyToggle())
	{
		@Override
		public void hotkeyPressed()
		{
			WebhookBody webhookBody = new WebhookBody();
			if(config.mentionUser()){
				webhookBody.setContent("<@" + config.userID() + "> pressed the hotkey!\n");
			} else {
				webhookBody.setContent(config.name() + " pressed the hotkey!\n");
			}
			sendWebhook(webhookBody);
		}
	};

	@Provides
	ElDiscordConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ElDiscordConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (configChanged.getGroup().equalsIgnoreCase(ElDiscordConfig.GROUP))
		{
			String s = config.lootNpcs();
			lootNpcs = s != null ? Text.fromCSV(s) : Collections.emptyList();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		client.setUsername("zezima");
		if(client.getLocalPlayer().getAnimation()!=-1){
			idleTime=0;
		} else {
			idleTime++;
		}
		WebhookBody webhookBody = new WebhookBody();
		if(idleTime>config.idleMessage()){
			webhookBody.setContent("<@" + config.userID() + "> your account has been idle for a minute!\n");
			createWebhookWithScreenshot(webhookBody);
			idleTime=0;
		}
		if(client.getTickCount()>10 && client.getTickCount() % config.updateInterval() == 0 && config.sendUpdateScreenshot() && client.getGameState().equals(GameState.LOGGED_IN)){
			webhookBody.setContent("<@" + config.userID() + "> sending you an update on your account.\n");
			createWebhookWithScreenshot(webhookBody);
		}

		if (client.getWidget(WidgetInfo.LEVEL_UP_LEVEL) != null && config.sendLevelUp())
		{
			String levelUpText = client.getWidget(WidgetInfo.LEVEL_UP_LEVEL).getText();
			if(!previousLevelUp.equals(levelUpText)){
				webhookBody.setContent(config.name() + ": " + levelUpText + "\n");
				previousLevelUp=levelUpText;
				sendWebhook(webhookBody);
			}

		}
	}

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived npcLootReceived)
	{
		NPC npc = npcLootReceived.getNpc();
		Collection<ItemStack> items = npcLootReceived.getItems();

		if (!lootNpcs.isEmpty())
		{
			for (String npcName : lootNpcs)
			{
				if (WildcardMatcher.matches(npcName, npc.getName()))
				{
					processLoot(npc.getName(), items);
					return;
				}
			}
		}
		else
		{
			processLoot(npc.getName(), items);
		}
	}

	@Subscribe
	public void onPlayerLootReceived(PlayerLootReceived playerLootReceived)
	{
		Collection<ItemStack> items = playerLootReceived.getItems();
		processLoot(playerLootReceived.getPlayer().getName(), items);
	}

	@Subscribe
	public void onLootReceived(LootReceived lootReceived)
	{
		if (lootReceived.getType() != LootRecordType.EVENT && lootReceived.getType() != LootRecordType.PICKPOCKET)
		{
			return;
		}

		processLoot(lootReceived.getName(), lootReceived.getItems());
	}

	private void processLoot(String name, Collection<ItemStack> items)
	{
		if(!config.sendLoot()){
			return;
		}
		WebhookBody webhookBody = new WebhookBody();

		long totalValue = 0;
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("**").append(config.name()).append(" got a drop worth more than ").append(config.lootValue()).append(" gp:**\n");
		final int targetValue = config.lootValue();
		for (ItemStack item : stack(items))
		{
			int itemId = item.getId();
			int qty = item.getQuantity();

			int price = itemManager.getItemPrice(itemId);
			long total = (long) price * qty;

			totalValue += total;

			if (config.includeLowValueItems() || total >= targetValue)
			{
				ItemComposition itemComposition = itemManager.getItemComposition(itemId);
				stringBuilder.append(qty).append(" x ").append(itemComposition.getName());
				if (config.stackValue())
				{
					stringBuilder.append(" (").append(QuantityFormatter.quantityToStackSize(total)).append(")");
				}
				stringBuilder.append("\n");
				//webhookBody.getEmbeds().add(new WebhookBody.Embed(new WebhookBody.UrlEmbed(itemImageUrl(itemId))));
			}
		}

		if (targetValue == 0 || totalValue >= targetValue)
		{
			webhookBody.setContent(stringBuilder.toString());
			sendWebhook(webhookBody);
		}
	}

	private void sendWebhook(WebhookBody webhookBody)
	{
		String configUrl = config.webhook();
		if (Strings.isNullOrEmpty(configUrl))
		{
			return;
		}

		HttpUrl url = HttpUrl.parse(configUrl);
		MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("payload_json", GSON.toJson(webhookBody));

		if (config.sendScreenshot())
		{
			sendWebhookWithScreenshot(url, requestBodyBuilder);
		}
		else
		{
			buildRequestAndSend(url, requestBodyBuilder);
		}
	}

	private void sendTextOnlyWebhook(WebhookBody webhookBody)
	{
		String configUrl = config.webhook();
		if (Strings.isNullOrEmpty(configUrl))
		{
			return;
		}

		HttpUrl url = HttpUrl.parse(configUrl);
		MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("payload_json", GSON.toJson(webhookBody));
		buildRequestAndSend(url, requestBodyBuilder);
	}

	private void createWebhookWithScreenshot(WebhookBody webhookBody)
	{
		String configUrl = config.webhook();
		if (Strings.isNullOrEmpty(configUrl))
		{
			return;
		}

		HttpUrl url = HttpUrl.parse(configUrl);
		MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("payload_json", GSON.toJson(webhookBody));
		sendWebhookWithScreenshot(url, requestBodyBuilder);
	}

	private void sendWebhookWithScreenshot(HttpUrl url, MultipartBody.Builder requestBodyBuilder)
	{
		drawManager.requestNextFrameListener(image ->
		{
			BufferedImage bufferedImage = (BufferedImage) image;
			Graphics2D g2d = bufferedImage.createGraphics();
			g2d.setColor(new Color(202,185,148));
			log.info("height:" + bufferedImage.getHeight() + ", width:" + bufferedImage.getWidth());
			g2d.fillRect(7,bufferedImage.getHeight()-44,100,15);
			g2d.dispose();

			byte[] imageBytes;
			try
			{
				imageBytes = convertImageToByteArray(bufferedImage);
			}
			catch (IOException e)
			{
				log.warn("Error converting image to byte array", e);
				return;
			}

			requestBodyBuilder.addFormDataPart("file", "image.png",
				RequestBody.create(MediaType.parse("image/png"), imageBytes));
			buildRequestAndSend(url, requestBodyBuilder);
		});
	}

	private void buildRequestAndSend(HttpUrl url, MultipartBody.Builder requestBodyBuilder)
	{
		RequestBody requestBody = requestBodyBuilder.build();
		Request request = new Request.Builder()
			.url(url)
			.post(requestBody)
			.build();
		sendRequest(request);
	}

	private void sendRequest(Request request)
	{
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Error submitting webhook", e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				response.close();
			}
		});
	}

	private static byte[] convertImageToByteArray(BufferedImage bufferedImage) throws IOException
	{
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
		return byteArrayOutputStream.toByteArray();
	}

	private static Collection<ItemStack> stack(Collection<ItemStack> items)
	{
		final List<ItemStack> list = new ArrayList<>();

		for (final ItemStack item : items)
		{
			int quantity = 0;
			for (final ItemStack i : list)
			{
				if (i.getId() == item.getId())
				{
					quantity = i.getQuantity();
					list.remove(i);
					break;
				}
			}
			if (quantity > 0)
			{
				list.add(new ItemStack(item.getId(), item.getQuantity() + quantity, item.getLocation()));
			}
			else
			{
				list.add(item);
			}
		}

		return list;
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		WebhookBody webhookBody = new WebhookBody();
		switch(event.getGameState()){
			case LOGIN_SCREEN:
				if(previousGameState.equals(GameState.LOGGED_IN)){
					webhookBody.setContent("**<@" + config.userID() + "> has logged out.**\n");
				}
				break;
			case LOGGED_IN:
				if(previousGameState.equals(GameState.HOPPING)){
					if(config.sendWorld()){
						webhookBody.setContent("**<@" + config.userID() + "> has hopped to world " + client.getWorld() + ".**\n");
					} else {
						webhookBody.setContent("**<@" + config.userID() + "> has hopped.**\n");
					}
				} else if(previousGameState.equals(GameState.LOGGING_IN)) {
					if(config.sendWorld()){
						webhookBody.setContent("**<@" + config.userID() + "> has connected to world " + client.getWorld() + ".**\n");
					} else {
						webhookBody.setContent("**<@" + config.userID() + "> has connected.**\n");
					}
				}
				break;
		}
		if(event.getGameState()!=GameState.LOADING){
			previousGameState=event.getGameState();
		}
		sendTextOnlyWebhook(webhookBody);
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event){
		if(event.getActor().equals(client.getLocalPlayer())){
			WebhookBody webhookBody = new WebhookBody();
			if(event.getHitsplat().getAmount()>0 && config.sendDamage()){
				webhookBody.setContent(config.name() + " has taken " + event.getHitsplat().getAmount() + " damage. (" + (client.getBoostedSkillLevel(Skill.HITPOINTS)-event.getHitsplat().getAmount()) + "/" + client.getRealSkillLevel(Skill.HITPOINTS) + ")\n");
				//Elliott has taken X damage. (Current/Full)
				sendWebhook(webhookBody);
			}
		} else if(event.getActor().equals(client.getLocalPlayer().getInteracting())){
			WebhookBody webhookBody = new WebhookBody();
			if(event.getHitsplat().getAmount()>0 && config.sendDamage()){
				webhookBody.setContent(config.name() + " has dealt " + event.getHitsplat().getAmount() + " damage.\n");
				//Elliott has dealt X damage. (Current/Full)
				sendWebhook(webhookBody);
			}
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		Actor actor = event.getActor();
		WebhookBody webhookBody = new WebhookBody();
		if (actor instanceof Player)
		{
			Player player = (Player) actor;
			if (player == client.getLocalPlayer() && config.sendDeath())
			{
				webhookBody.setContent("**<@" + config.userID() + "> has died!**\n");
				//**@Elliott has died!**
				sendWebhook(webhookBody);
			}
			if (config.sendKills() && client.getLocalPlayer().getInteracting()!=null && client.getLocalPlayer().getInteracting() == player)
			{
				webhookBody.setContent(config.name() + " has killed " + player.getName() + "!\n");
				//Elliott has killed Opponent!
				sendWebhook(webhookBody);
			}
		}
	}

	@Subscribe
	public void onPlayerSpawned(PlayerSpawned event){
		if(!config.sendPlayers() || event.getPlayer().getName().equals(client.getLocalPlayer().getName())){
			return;
		}
		WebhookBody webhookBody = new WebhookBody();
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(config.name()).append(" spotted: \n");
		//Elliott spotted:
		stringBuilder.append(event.getPlayer().getName()).append(" (level-").append(event.getPlayer().getCombatLevel()).append(") \n");
		//Target (level-X)
		if(event.getPlayer().getSkullIcon()!=null && event.getPlayer().getSkullIcon().equals(SkullIcon.SKULL)){
			stringBuilder.append("Skull: ✔ \n");
		} else {
			stringBuilder.append("Skull: ❌ \n");
		}
		//Skull: ✔/❌
		WorldPoint targetWorldPoint = event.getPlayer().getWorldLocation();
		stringBuilder.append("World Location: (").append(targetWorldPoint.getX()).append(",").append(targetWorldPoint.getY()).append(",").append(targetWorldPoint.getPlane()).append(")");
		//World Location: (X,Y,Z)
		webhookBody.setContent(stringBuilder.toString());
		sendWebhook(webhookBody);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event){
		if(event.getMessage().toLowerCase().contains("bot") && event.getType().equals(ChatMessageType.PUBLICCHAT)){
			WebhookBody webhookBody = new WebhookBody();
			webhookBody.setContent("<@" + config.userID() + "> someone has said bot in the chat!\n");
			createWebhookWithScreenshot(webhookBody);
		}
	}
}