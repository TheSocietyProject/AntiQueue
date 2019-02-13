import com.github.steveice10.mc.protocol.data.message.ChatColor;
import com.github.steveice10.mc.protocol.data.message.Message;
import com.github.steveice10.mc.protocol.data.message.MessageStyle;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.sasha.eventsys.SimpleEventHandler;
import com.sasha.eventsys.SimpleListener;
import com.sasha.reminecraft.ReMinecraft;
import com.sasha.reminecraft.api.RePlugin;
import com.sasha.reminecraft.api.event.ChatReceivedEvent;
import com.sasha.reminecraft.client.ChildReClient;
import com.sasha.reminecraft.client.ReClient;
import com.sasha.reminecraft.logging.ILogger;
import com.sasha.reminecraft.logging.LoggerBuilder;

import javax.swing.event.ChangeListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Style;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Main extends RePlugin implements SimpleListener {

private boolean inQueue = true;

    public final int acceptedMsgTime = 5 * 60 * 1000; // 5 mins


    private long lastMsg;

    public ILogger logger = LoggerBuilder.buildProperLogger("AniQueueLoggerPlugin");

    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);


    @Override
    public void onPluginInit() {
        this.getReMinecraft().EVENT_BUS.registerListener(this);
    }

    @Override
    public void onPluginEnable() {
        ScheduledFuture<?> scheduledFuture = executor.scheduleAtFixedRate(() -> {
            if (ReClient.ReClientCache.INSTANCE.playerListEntries.size() != 0) {
                if(inQueue)
                    testQueueTimeOut();
            }
        }, 5L, 60L, TimeUnit.SECONDS);
    }


    public void testQueueTimeOut(){
        if(queueTimeOut())
            timeOutAction(); // extra method so when the null msg comes its also reconnecting

    }

    public boolean queueTimeOut(){
        /*
            the other time outs r in testWetherInQueue:
                - Position in queue: 0
                - Exception Connecting:ReadTimeoutException : null

         */

        long now = System.currentTimeMillis();
        return now - lastMsg > acceptedMsgTime && !inQueue;
    }

    public void timeOutAction(){
        reconnect();

    }




    public void reconnect(){
        // TODO search for a better way! also with millis to wait
        this.getReMinecraft().reLaunch();
    }


    @SimpleEventHandler
    public void onEvent(ChatReceivedEvent e){
        lastMsg = e.getTimeRecieved();
        testWetherInQueue(e.getMessageText());

    }

    private boolean testWetherInQueue(String msg) {
        if (msg.startsWith("<")) {
            return inQueue = false;
        }

        if (msg.startsWith("2b2t is full")) {
            return inQueue = true;
        }

        if(msg.equals("Position in queue: 0"))
            timeOutAction(); // still sets true cuz starts with Pos in queue

        if (msg.startsWith("Position in queue: "))
            return inQueue = true;


        if(msg.equals("Exception Connecting:ReadTimeoutException : null")){ // TODO
            timeOutAction();
            return inQueue = true;
        }

        return inQueue;
    }


    @Override
    public void onPluginDisable() {

    }

    @Override
    public void onPluginShutdown() {

    }

    @Override
    public void registerCommands() {

    }

    @Override
    public void registerConfig() {

    }
}
