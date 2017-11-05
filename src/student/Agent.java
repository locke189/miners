package student;

import mas.agents.AbstractAgent;
import mas.agents.Message;
import mas.agents.SimulationApi;
import mas.agents.StringMessage;
import mas.agents.task.mining.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Agent extends AbstractAgent {
    public Agent(int id, InputStream is, OutputStream os, SimulationApi api) throws IOException, InterruptedException {
        super(id, is, os, api);
    }

    public StatusMessage status = null;

    // See also class StatusMessage
    public static String[] types = {
            "", "obstacle", "depot", "gold", "agent"
    };

    public int x,y,prevX,prevY,height,width,targetX,targetY,assistingAgent,depotX, depotY;
    public String state = "searchDepot";
    // state = s searching
    // g - gold located
    // c - call for help
    // w - waiting for help
    // t - taking gold to depot


    public boolean moveTo(int targetX, int targetY) throws Exception {
        int deltaX = x - targetX;
        int deltaY = y - targetY;

        log(String.format("I want to go to [%d,%d], deltaX %f, deltaY %f, x %d, y %d", targetX, targetY, Math.pow(deltaX,2), Math.pow(deltaY,2),x,y));
        if(Math.pow(deltaX,2) > Math.pow(deltaY,2)) {
            simpleMoveX(deltaX);
        } else {
            simpleMoveY(deltaY);
        }

        if(deltaX == 0 && deltaY ==0){
            return true;
        }
        return false;
    }

    public boolean checkMovement(int x, int y) throws Exception {
        if(map[x][y] == '0'){
            log("wont move by obstacle");
            return false;
        }
        if(x == prevX && y == prevY){
            log("wont move backwards!");
            return false;
        }

        if(x < 0 || y < 0 || x >= width || y >= height){
            log("wont move out of bounds!");
            log(String.format("x -> %d, y -> %d, height -> %d, width -> %d", x, y, height, width));
            return false;
        }
        log("I can move");
        return true;
    }

    public void simpleMoveX(int delta) throws Exception {

        if(delta > 0 && checkMovement(x - 1,y)){
            log("will move left");
            status = left();
        }
        if(delta < 0 && checkMovement(x + 1,y)){
            log("will move right");
            status = right();
        }
        log(String.format("I wont move horizontally. deltaX %d", delta));
    }
    public void simpleMoveY(int delta) throws Exception {
        if(delta > 0 && checkMovement(x, y - 1)){
            log("will move up");
            status = up();
        }
        if(delta < 0 && checkMovement(x, y + 1)){
            log("will move down");
            status = down();
        }
        log("I wont move vertically");
    }

    public void moveRandom() throws Exception{
            double rnd = Math.random();
            if(rnd < 0.25) status = left();
            else if(rnd < 0.5) status = right();
            else if(rnd < 0.75) status = up();
            else down();
    }

    public void checkSensor(StatusMessage.SensorData data) throws Exception{
        char status_c = ' ';
        if(state == "idle" || state == "searchDepot") {
            if (types[data.type] == "gold") {
                status_c = 'g';
                if(state == "idle") {
                    log("Im calling for help");
                    targetX = data.x;
                    targetY = data.y;
                    callForHelp();
                }
            }
            if (types[data.type] == "depot") {
                log("I've found the depot");
                status_c = 'd';
                if(state == "searchDepot") {
                    depotX = data.x;
                    depotY = data.y;
                    state = "idle";
                }
            }
            if (types[data.type] == "obstacle") {
                log("behold an obstacle!!!");
                status_c = 'o';
            }

            map[data.x][data.y] = status_c;
            announce(status_c, data.x, data.y);
        }


        if(state == "waitingAgentToArrive") {
            if (types[data.type] == "agent" ) {
                if(x == data.x || y == data.y) {
                    log(String.format("Agent arrived @[%d,%d] I am at [%d,%d]", data.x, data.y, x,y));
                    state = "picking";
                }

            }
        }

        if(state == "helping") {
            if (types[data.type] == "agent" ) {
                if(x == data.x || y == data.y) {
                    log(String.format("Agent arrived @[%d,%d] I am at [%d,%d]", data.x, data.y, x,y));
                    state = "stop";
                }

            }
        }
    }

    public void callForHelp() throws Exception{
        if(state == "idle"){
            for(int i=1; i<5; i++){
                if(i != getAgentId()){
                    sendMessage(i, new StringMessage("help" + ">"+x+","+y));
                    state = "callingHelp";
                }
            }
        }
    }

    public void acceptHelp(int x) throws Exception{
        if(state=="callingHelp"){
            sendMessage(x, new StringMessage("accepted"));
            state = "goToGold";
            assistingAgent = x;

        } else {
            sendMessage(x, new StringMessage("refused"));
        }

    }

    public void offerHelp(int i) throws Exception{

        if(state == "idle"){
            log(String.format("I will offer my help to agent %d!",i));
            sendMessage(i, new StringMessage("willHelp"));
            state = "waitingConfirmation";
        }

    }

    public void release() throws Exception{

        if(state == "releasingAgents"){
            log(String.format("I therefore release agent %d!", assistingAgent));
            sendMessage(assistingAgent, new StringMessage("release"));
            state = "depositGold";
            targetX = depotX;
            targetY = depotY;
        }

    }



    public void announce(char type, int x, int y) throws Exception {
        for(int i=1; i<5; i++){
            if(i != getAgentId()){
                sendMessage(i, new StringMessage(type + ">"+x+","+y));
            }
        }
    }



    public void validateMessage(Message m) throws Exception {
        String posX = "", posY ="", coordinate;
        String message = m.stringify();
        String[] parts = message.split(">");
        String query = parts[0]; //
        if(parts.length > 1){
            coordinate = parts[1]; //
            parts = coordinate.split(",");
            posX = parts[0];

            if(parts.length > 1){
                 posY = parts[1];
            }
        }

        switch(query){
            case "help":
                log(String.format("I was asked to help agent %d",m.getSender()));
                if(state == "idle"){
                    offerHelp(m.getSender());
                    targetX = Integer.parseInt(posX);
                    targetY = Integer.parseInt(posY);
                }
                break;
            case "accepted":
                if(state == "waitingConfirmation") {
                    log(String.format("my help was accepted by %d",m.getSender()));
                    state = "helping";
                }
                break;
            case "refused":
                if(state == "waitingConfirmation"){
                    log(String.format("my help was refused by %d",m.getSender() ));
                    state = "idle";
                }
                break;
            case "willHelp":
                log(String.format("Agent #%d offered to help :D ", m.getSender()));
                acceptHelp(m.getSender());
                break;
            case "release":
                if(state == "stop"){
                    log(String.format("I have been released from #%d", m.getSender()));
                    state = "idle";
                }
                break;
            case "g":
                log(String.format("Agent #%d found gold at [%s,%s]", m.getSender(), posX, posY));
                break;
            case "d":
                if(state=="searchDepot"){
                    log(String.format("Agent #%d found depot at [%s,%s]", m.getSender(), posX, posY));
                    depotX = Integer.parseInt(posX);
                    depotY = Integer.parseInt(posY);
                    state = "idle";
                }
                break;


        }

    }


    public char[][] map = new char[30][30];

    @Override
    public void act() throws Exception {
        boolean onTarget = false;

        sendMessage(1, new StringMessage("Hello"));

        prevX = -1;
        prevY = -1;


        while(true) {
            while (messageAvailable()) {
                Message m = readMessage();
                log("I have received " + m);
                validateMessage(m);
            }

            if(state=="idle" || state == "searchDepot"){
                moveRandom();
            }
            if(state=="helping" || state=="goToGold" || state=="depositGold" ){
               onTarget = moveTo(targetX,targetY);

               if( onTarget && state == "goToGold"){
                   log("Im on Gold!");
                   state = "waitingAgentToArrive";
               }
                if( onTarget && state == "depositGold"){
                    log("Im on depot! dropping gold");
                    drop();
                    state = "idle";
                }
            }
            if(state=="picking"){
                log("Im picking gold!");
                pick();
                state = "releasingAgents";
                release();
            }

            status = sense();

            if(status != null){
                log(String.format("I am now on position [%d,%d] of a %dx%d map. state %s",
                        status.agentX, status.agentY, status.width, status.height, state));

                x = status.agentX;
                y = status.agentY;
                height = status.height;
                width = status.width;

                for(StatusMessage.SensorData data : status.sensorInput) {
                    checkSensor(data);
                    log(String.format("I see %s at [%d,%d]", types[data.type], data.x, data.y));
                }
            }




            try {
                Thread.sleep(200);
            } catch(InterruptedException ie) {}
        }
    }
}
