
import com.sasha.eventsys.SimpleEventHandler;
import com.sasha.eventsys.SimpleListener;
import com.sasha.reminecraft.Configuration;
import com.sasha.reminecraft.api.RePlugin;
import com.sasha.reminecraft.api.event.ChatReceivedEvent;
import com.sasha.reminecraft.logging.ILogger;
import com.sasha.reminecraft.logging.LoggerBuilder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Main extends RePlugin implements SimpleListener {

    private boolean inQueue = true;
    private boolean connecting = false;


    private Config CFG = new Config();


    private long lastMsg;
    private boolean lastMsgValid = false;


    public ILogger logger = LoggerBuilder.buildProperLogger("AniQueue");

    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);


    @Override
    public void onPluginInit() {
        this.getReMinecraft().EVENT_BUS.registerListener(this);
    }

    @Override
    public void onPluginEnable() {
        ScheduledFuture<?> scheduledFuture = executor.scheduleAtFixedRate(() -> {
            if(inQueue && !connecting)
                testQueueTimeOut();

        }, 1L, 5L, TimeUnit.SECONDS);
    }


    public synchronized void testQueueTimeOut(){
        if(queueTimeOut())
            timeOutAction(); // extra method so when the null msg comes its also reconnecting

    }

    public boolean queueTimeOut(){
        /*
            the other time outs r in testWetherInQueue:
                - Position in queue: 0
                - Exception Connecting:ReadTimeoutException : null

         */

        if(!lastMsgValid)
            return false;

        long now = System.currentTimeMillis();

        return now - lastMsg > CFG.var_acceptedWaitTime;
    }

    public void timeOutAction(){
        logger.log("AntiQueue decided to RECONNECT now because last queue msg is " + (System.currentTimeMillis() - lastMsg) + " away.");

        reconnect();

    }




    public void reconnect(){
        ReconnectManager.reconnect();
    }


    @SimpleEventHandler
    public void onEvent(ChatReceivedEvent e){
        lastMsg = e.getTimeRecieved();
        lastMsgValid = true;
        testWetherInQueue(e.getMessageText());

    }

    private boolean testWetherInQueue(String msg) {
        connecting = false;

        if (msg.startsWith("<"))
            return inQueue = false;


        if (msg.startsWith("2b2t is full"))
            return inQueue = true;

        if(msg.equals("Connecting to the server..."))
            connecting = true;

        if(msg.equals("Position in queue: 0"))
            timeOutAction(); // still sets true cuz starts with Pos in queue

        if (msg.startsWith("Position in queue: "))
            return inQueue = true;

        logger.log("msg is: \"" + msg + "\"");
        logger.log("is ==?  \"Exception Connecting:ReadTimeoutException : null\"");
        //             Exception Connecting:ReadTimeoutException : null
        if(msg.equals("Exception Connecting:ReadTimeoutException : null")){
            timeOutAction();
            return inQueue = true;
        }

        return inQueue;
    }


    @Override
    public void onPluginDisable() {
        this.getReMinecraft().EVENT_BUS.deregisterListener(this);
        lastMsgValid = false;

    }

    @Override
    public void onPluginShutdown() {
        lastMsgValid = false;
    }

    @Override
    public void registerCommands() {

    }

    @Override
    public void registerConfig() {
        this.getReMinecraft().configurations.add(CFG);
    }
}

class Config extends Configuration {
    @ConfigSetting
    public int var_acceptedWaitTime; // 0 does nothing, 1 removes it completely, 2 points to the msg that is repeated



    public Config() {
        super("AntiQueue");

        this.var_acceptedWaitTime = 60 * 1000; // 1 minute
    }



}
