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

    public int x,y,prevX,prevY,height,width;
    public char state = 's';
    // state = s searching
    // g - gold located
    // c - call for help
    // w - waiting for help
    // t - taking gold to depot
    

    public void moveTo(int targetX, int targetY) throws Exception {
        int deltaX = x - targetX;
        int deltaY = y - targetY;

        if(deltaX > deltaY) {
            simpleMoveX(deltaX);
        } else {
            simpleMoveY(deltaY);
        }
    }

    public boolean checkMovement(int x, int y) throws Exception {
        if(map[x][y] == '0'){
            return false;
        }
        if(x == prevX && y == prevY){
            return false;
        }

        if(x < 0 || y < 0 || x >= width || y >= height){
            return false;
        }
        return true;
    }

    public void simpleMoveX(int delta) throws Exception {
        if(delta > 0 && checkMovement(x - 1,y)){
            left();
        }
        if(delta < 0 && checkMovement(x + 1,y)){
            right();
        }
    }
    public void simpleMoveY(int delta) throws Exception {
        if(delta > 0 && checkMovement(x, y - 1)){
            down();
        }
        if(delta < 0 && checkMovement(x, y + 1)){
            up();
        }
    }

    public void moveRandom() throws Exception{
        double rnd = Math.random();
        if(rnd < 0.25) status = left();
        else if(rnd < 0.5) status = right();
        else if(rnd < 0.75) status = up();
        else down();
    }

    public void checkSensor(StatusMessage.SensorData data) throws Exception{
        char status = ' ';

        if(types[data.type] == "gold"){
            status = 'g';
        }
        if(types[data.type] == "depot"){
            status = 'd';
        }
        if(types[data.type] == "obstacle"){
            status = 'o';
        }

        map[data.x][data.y] = status;
        announce(status,data.x,data.y);

    }

    public void callForHelp() throws Exception{

    }

    public void acceptHelp() throws Exception{

    }

    public void offerHelp() throws Exception{

    }


    public void announce(char type, int x, int y) throws Exception {
        for(int i=1; i<5; i++){
            if(i == getAgentId()){
                break;
            }
            sendMessage(i, new StringMessage(type + "->"+x+","+y));
        }
    }



    public char[][] map = new char[30][30];

    @Override
    public void act() throws Exception {
        sendMessage(1, new StringMessage("Hello"));



        while(true) {
            while (messageAvailable()) {
                Message m = readMessage();
                log("I have received " + m);
            }

            moveRandom();

            log(String.format("I am now on position [%d,%d] of a %dx%d map.",
                    status.agentX, status.agentY, status.width, status.height));

            for(StatusMessage.SensorData data : status.sensorInput) {
                checkSensor(data);
                log(String.format("I see %s at [%d,%d]", types[data.type], data.x, data.y));
            }

            try {
                Thread.sleep(200);
            } catch(InterruptedException ie) {}
        }
    }
}
