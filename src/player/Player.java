package player;

import javax.crypto.SecretKey;
import javax.net.ssl.*;

import communication.MessageDispatcher;
import game.Cards;
import game.GameLogic;
import ui.GameUI;
import ui.ServerUI;
import utils.SecretKeyGenerator;
import utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;



public class Player {

    static PrintStream out;
    static BufferedReader in;
    
    private static MessageDispatcher dispatcher;
    private static SecretKey secret_key;
    
    private static int player_id = -1;
    private static String username;
    private static boolean owner = false;
    
    private static GameLogic game;
    private static Cards cards = new Cards();
      
    private static ArrayList<String> white_cards = new ArrayList<String>();
    private static String black_card = "";
    private static boolean jury = false;
    private static ArrayList<String>answers= new ArrayList<String>();
    private static ArrayList<Integer>answers_id =new ArrayList<Integer>();
    private static ArrayList<Integer>scores =new ArrayList<Integer>();
    private static ArrayList<Integer>scores_id =new ArrayList<Integer>();
    private static int points=0;
 
    

	public static void main(String[] args) throws Exception {
		String host = Utils.HOST;
		int port = Utils.PORT;

		if (args.length > 2) {
			System.out.println("BAD USAGE- The arguments are: Server Ip and Server Port");
		} else if(args.length==2){
			host = args[0];
			port = Integer.parseInt(args[1]);
		}

		SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
		SSLSocket socket = (SSLSocket) sf.createSocket(InetAddress.getByName(host), port);
		socket.setEnabledCipherSuites(sf.getSupportedCipherSuites());

		out = new PrintStream(socket.getOutputStream());
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

		String request = null;
		while (((request = in.readLine()) != null)) {
			paseRequest(request);
		}

		in.close();
		out.close();
		socket.close();
	}

    public static int getPlayer_id() {
		return player_id;
	}

	private static void paseRequest(String request) throws IOException {
    	String data;
    	
        switch (request){
            case "WELCOME":
                username = ServerUI.welcome();
                sendResponse(username);
                player_id = Integer.parseInt(in.readLine());
                break;
                
            case "MENU":
            	data = ServerUI.menu();
            	sendResponse(data);
            	break;
            	
            case "NEW_ROOM":
            	owner = true;
            	game = new GameLogic();
            	
            	data = ServerUI.newRoom();
            	sendResponse(data);
            	
            	data = ServerUI.password();
            	sendResponse(data);
            	break;
            	
            case "SHOW_ROOMS":
            	ServerUI.showRooms();
            	
            	data = in.readLine();
                while (!data.equals("STOP")){
                	System.out.println(data);
                	data = in.readLine();
                }
            	break;
            	
            case "ENTER_ROOM":
            	data = ServerUI.enterRoom();
            	sendResponse(data);
            	
            	data = in.readLine();

            	if(data.equals("PW")) {
            		data = ServerUI.password();
            		sendResponse(data);
            		data = in.readLine();
            	}
            	
            	System.out.println(data);
            	break;
            case "ROOM":
            	ServerUI.showRoom();
            	
            	data = in.readLine();
                while (!data.equals("STOP")){
                	System.out.println(data);
                	data = in.readLine();
                }
                                
                String port = in.readLine();
                String mcast_addr = in.readLine();
                secret_key = SecretKeyGenerator.decodeKeyFromString(in.readLine());
                
                dispatcher = new MessageDispatcher(Integer.parseInt(port),mcast_addr,secret_key,player_id);
                new Thread(dispatcher).start();
                
                dispatcher.sendMessage("NEWPLAYER",username, player_id);
                
                if(owner) 
                	System.out.println("Wainting for players to be ready ...");
                else {
                	ServerUI.ready();
                	dispatcher.sendMessage("READY",username,player_id);
                }
	             
            	break;
            	
            default:
                break;
        }
    }


	private static void sendResponse(String response){
        out.println(response);
        out.flush();
    }
	
    public static MessageDispatcher getDispatcher() {
		return dispatcher;
	}


	public static void setBlackCard(String card) {
		black_card = card;
	}

	public static void isJury(boolean b) {
		jury= b;
	}

	public static void startRound() {
		String choosen_card;
		String winner_card;
		if(jury) {
			System.out.println("Waiting for Players answers");
			try {
				Thread.sleep(10000); 
				winner_card=GameUI.printAnswers(answers);
				int i=answers.indexOf(winner_card);
				int winner_id= answers_id.get(i);
				dispatcher.sendMessage("ROUNDWINNER","You Won this round! Congrats!",winner_id);
				Thread.sleep(100); 
				dispatcher.sendMessage("ROUNDEND", "round ended", player_id);
				
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else {
			choosen_card=GameUI.makePlay(black_card, white_cards,points);
			white_cards.remove(choosen_card);
			dispatcher.sendMessage("PICKWHITECARD", choosen_card, player_id);
		}
	}

	public static boolean isJury() {
		return jury;
	}
	public static void endJury() {
		 jury=false;
	}
	public static int getPoints() {
		return points;
	}
	public static void setPoints() {
		
		points ++;
	}
	

	public static boolean isOwner() {
		return owner;
	}
	public static GameLogic getGame() {
		return game;
	}
	
	public static void getInitialCards() {
		for(int i =0; i<5; i++) 
			white_cards.add(cards.drawWhiteCard());	
	}
	
	public static void drawWhiteCard() {
			white_cards.add(cards.drawWhiteCard());	
	}
	public static String getBlackCard() {
		return cards.drawBlackCard();
	}
	public static void endGame() {
		GameUI.printResults();
		
	}
	public static void sendScore() {
		Player.dispatcher.sendMessage("SCORE", ""+points, player_id);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
	}
	public static void addScore(int i, int sender_id) {
		scores.add(i);
		scores_id.add(sender_id);
		if(scores.size()==GameLogic.getNumberPlayers()-1) {
			scores.add(points);
			scores_id.add(player_id);
			int max=Collections.max(scores);
			System.out.println("THE MAX IS"+ max);
			int winner_id= scores.indexOf(max);
			System.out.println("THE Winner IS"+ winner_id);
			Player.dispatcher.sendMessage("WINNER","you won with "+max+" points",winner_id);
		}
			
	}
	

    public static void addAnswer(String card,int player_id) {
    	answers.add(card);
    	answers_id.add(player_id);
    }
	
	

}
