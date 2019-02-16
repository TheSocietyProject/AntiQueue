
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


    private Config CFG = new Config();


    private long lastMsg;

    public ILogger logger = LoggerBuilder.buildProperLogger("AniQueueLoggerPlugin");

    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);


    @Override
    public void onPluginInit() {
        this.getReMinecraft().EVENT_BUS.registerListener(this);
    }

    @Override
    public void onPluginEnable() {
        ScheduledFuture<?> scheduledFuture = executor.scheduleAtFixedRate(() -> {
            if(inQueue)
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


        long now = System.currentTimeMillis();

        return now - lastMsg > CFG.var_acceptedWaitTime;
    }

    

    public void timeOutAction(){
        logger.log("AntiQueue: reconnecting now");

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
        if (msg.startsWith("<"))
            return inQueue = false;


        if (msg.startsWith("2b2t is full"))
            return inQueue = true;


        if(msg.equals("Position in queue: 0"))
            timeOutAction(); // still sets true cuz starts with Pos in queue

        if (msg.startsWith("Position in queue: "))
            return inQueue = true;


        if(msg.equals("Exception Connecting:ReadTimeoutException : null")){
            timeOutAction();
            return inQueue = true;
        }

        return inQueue;
    }


    @Override
    public void onPluginDisable() {
        this.getReMinecraft().EVENT_BUS.deregisterListener(this);
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

class Config extends Configuration {
    @ConfigSetting
    public int var_acceptedWaitTime; // 0 does nothing, 1 removes it completely, 2 points to the msg that is repeated



    public Config() {
        super("AntiQueue");

        this.var_acceptedWaitTime = 60 * 1000; // 1 minute
    }



}
