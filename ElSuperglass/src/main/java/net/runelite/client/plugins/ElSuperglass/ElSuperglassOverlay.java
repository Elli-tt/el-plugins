package net.runelite.client.plugins.ElSuperglass;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import static net.runelite.api.MenuOpcode.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.ui.overlay.components.table.TableAlignment;
import net.runelite.client.ui.overlay.components.table.TableComponent;
import net.runelite.client.util.ColorUtil;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDuration;

@Slf4j
@Singleton
class ElSuperglassOverlay extends OverlayPanel
{
    private final Client client;
    private final ElSuperglassPlugin plugin;
    private final ElSuperglassConfig config;

    String timeFormat;

    @Inject
    private ElSuperglassOverlay(final Client client, final ElSuperglassPlugin plugin, final ElSuperglassConfig config)
    {
        super(plugin);
        setPosition(OverlayPosition.DYNAMIC);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "superglass maker overlay"));
        setPriority(OverlayPriority.HIGHEST);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (plugin.botTimer == null || !plugin.startSuperglassMaker || !config.enableUI())
        {
            log.debug("Overlay conditions not met, not starting overlay");
            return null;
        }

        TableComponent tableComponent = new TableComponent();
        tableComponent.setColumnAlignments(TableAlignment.LEFT, TableAlignment.RIGHT);
        Duration duration = Duration.between(plugin.botTimer, Instant.now());
        timeFormat = (duration.toHours() < 1) ? "mm:ss" : "HH:mm:ss";
        tableComponent.addRow("Time:", formatDuration(duration.toMillis(), timeFormat));
        tableComponent.addRow("Status:", plugin.status);
        tableComponent.addRow("Sand:", Integer.toString(plugin.bucketsOfSandLeft)+"("+Integer.toString(plugin.bucketsOfSandLeft-plugin.bucketsOfSandStart)+")");
        tableComponent.addRow("Seaweed:", Integer.toString(plugin.seaweedLeft)+"("+Integer.toString(plugin.seaweedLeft-plugin.seaweedStart)+")");
        tableComponent.addRow("Glass:", Integer.toString(plugin.glassLeft)+"(+"+Integer.toString(plugin.glassLeft-plugin.glassStart)+")");


        if (!tableComponent.isEmpty())
        {
            panelComponent.setBackgroundColor(ColorUtil.fromHex("#121212")); //Material Dark default
            panelComponent.setPreferredSize(new Dimension(200, 200));
            panelComponent.setBorder(new Rectangle(5, 5, 5, 5));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("El Superglass")
                    .color(ColorUtil.fromHex("#40C4FF"))
                    .build());
            panelComponent.getChildren().add(tableComponent);
        }
        return super.render(graphics);
    }
}
