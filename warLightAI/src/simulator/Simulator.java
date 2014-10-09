/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator;

import bot.*;
import java.util.*;
import main.*;
import move.*;

/**
 *
 * @author agnius
 */
public class Simulator {
    final long TIMEOUT = 2000;
    private BotParser parser;
    private BotState state;
    public Bot myBot;
    public Bot opponentBot;
    private Bot playingBot;
    private Random rnd;
    final String initSettings = "settings your_bot player1\n" +
                                "\n" +
                                "settings opponent_bot player2\n" +
                                "\n" +
                                "setup_map super_regions 1 5 2 2 3 5 4 3 5 7 6 2\n" +
                                "\n" +
                                "setup_map regions 1 1 2 1 3 1 4 1 5 1 6 1 7 1 8 1 9 1 10 2 11 2 12 2 13 2 14 3 15 3 16 3 17 3 18 3 19 3 20 3 21 4 22 4 23 4 24 4 25 4 26 4 27 5 28 5 29 5 30 5 31 5 32 5 33 5 34 5 35 5 36 5 37 5 38 5 39 6 40 6 41 6 42 6\n" +
                                "\n" +
                                "setup_map neighbors 1 2,4,30 2 4,3,5 3 5,6,14 4 5,7 5 6,7,8 6 8 7 8,9 8 9 9 10 10 11,12 11 12,13 12 13,21 14 15,16 15 16,18,19 16 17 17 19,20,27,32,36 18 19,20,21 19 20 20 21,22,36 21 22,23,24 22 23,36 23 24,25,26,36 24 25 25 26 27 28,32,33 28 29,31,33,34 29 30,31 30 31,34,35 31 34 32 33,36,37 33 34,37,38 34 35 36 37 37 38 38 39 39 40,41 40 41,42 41 42\n" +
                                "\n" +
                                "pick_starting_regions 10000 6 4 12 13 19 16 22 23 34 32 41 39"
                                ;

    public enum MyGameOutcome {
        WIN,
        LOSE,
        DRAW,
        UNDEFINED
    }

    private void initializeGameSettings() {
        rnd = new Random();
        myBot = new BotSpartacus();
        opponentBot = new oldBot();
        playingBot = myBot;
        parser = new BotParser(myBot, initSettings);
        parser.run();
        state = parser.currentState;
    }
    
    public Simulator() {
        initializeGameSettings();
    }
    
    private boolean continentRegionsHaveNeighbors(SuperRegion continent) {
        for (Region r : state.getFullMap().getRegions()) {
            if (r.getSuperRegion() == continent && r.getNeighbors().size() > 0)
                return true;
        }
        return false;
    }

    private String checkMapSetupForTest() {
        if (state.getFullMap().superRegions.size() != 6)
            return "Super region count is not 6 !";
        
        if (state.getFullMap().regions.size() != 42)
            return "Region count is not 42 !";
        
        for (SuperRegion continent : state.getFullMap().getSuperRegions()) {
            if (!continentRegionsHaveNeighbors(continent))
                return String.format("Continent %d regions doesn't have neighbors !", continent.getId());
        }
        
        return "";
    }
    
    private boolean listHasDuplicates(List<?> col) {
        for (Object o : col) {
            if (col.indexOf(o) != col.lastIndexOf(o))
                return true;
        }
        return false;
    }
    
    private ArrayList<Region> chooseThreeStartingRegions(ArrayList<Region> regions, boolean isFirstBot ) {
        ArrayList<Region> selectedReg = new ArrayList<>();
        int startIx = (isFirstBot)? 0 : 1;
                
        for (int i = startIx ; selectedReg.size() != 3 ; i+=2) 
            selectedReg.add(regions.get(i));
        
        return selectedReg;
    } 
    
    private String setInitialRegionsForPlayers() {
        ArrayList<Region> regions1 = myBot.getPreferredStartingRegions(state, TIMEOUT);
        ArrayList<Region> regions2 = opponentBot.getPreferredStartingRegions(state, TIMEOUT);        
        ArrayList<Region> regionsSelected1 = chooseThreeStartingRegions(regions1, true);
        ArrayList<Region> regionsSelected2 = chooseThreeStartingRegions(regions2, false);
        
        if (regions1.size() != 6 || regions2.size() != 6)
            return "Bots have chosen less then 6 regions";
        
        if (listHasDuplicates(regions1) || listHasDuplicates(regions2))
            return "Bots have duplicates in their chosen starting regions";
        
        if (regionsSelected1.size() != 3 || regionsSelected2.size() != 3)
            return "Not 3 distinct regions for bot is chosen !";
        
        // assign 3 regions to each player
        for (int i = 0; i < 3; i++) {
            regionsSelected1.get(i).setPlayerName(state.getMyPlayerName());
            regionsSelected2.get(i).setPlayerName(state.getOpponentPlayerName());
            
            regionsSelected1.get(i).setArmies(2);
            regionsSelected2.get(i).setArmies(2);
        }
        
        // set rest regions to neutral
        for (Region reg : state.getFullMap().getRegions()) {
            if (reg.ownedByPlayer("unknown")) {
                reg.setPlayerName("neutral");
                reg.setArmies(2);
            }
        }
        
        return "";
    }

    public void setActivePlayer(String playerName) {
        playingBot = (playerName.equals("player1"))? myBot : opponentBot;

        if (state.getMyPlayerName().equals(playerName))
            return;

        state.setOpponentPlayerName(state.getMyPlayerName());
        state.setMyPlayerName(playerName);
    }    
    
    private int placeArmiesForPlayer(PlaceArmiesMove placeArmiesMove, int armiesPerRound) {
        Region visibleMapRegion = placeArmiesMove.getRegion();
        
        if (visibleMapRegion.ownedByPlayer(placeArmiesMove.getPlayerName())) {
            if (placeArmiesMove.getArmies() <= armiesPerRound) {
                Region fullMapRegion = state.getFullMap().getRegion(visibleMapRegion.getId());
                int newArmiesCount = visibleMapRegion.getArmies() + placeArmiesMove.getArmies();
                fullMapRegion.setArmies(newArmiesCount);
                visibleMapRegion.setArmies(newArmiesCount);
                return placeArmiesMove.getArmies();
            }
        }        
        return 0;
    }

    private void placeArmiesForPlayerTransfer(AttackTransferMove placeArmiesMove) {
        Region toVisibleMapRegion = placeArmiesMove.getToRegion();
        Region fromVisibleMapRegion = placeArmiesMove.getFromRegion();        

        if (!fromVisibleMapRegion.ownedByPlayer(placeArmiesMove.getPlayerName()))
            return;
        
        if (!toVisibleMapRegion.ownedByPlayer(placeArmiesMove.getPlayerName()))
            return;
        
        Region toFullMapRegion = state.getFullMap().getRegion(toVisibleMapRegion.getId());
        Region fromFullMapRegion = state.getFullMap().getRegion(fromVisibleMapRegion.getId());

        int toArmiesCount = toVisibleMapRegion.getArmies() + placeArmiesMove.getArmies();
        int fromArmiesCount = fromVisibleMapRegion.getArmies() - placeArmiesMove.getArmies();
        if (fromArmiesCount < 1)
            return;

        toFullMapRegion.setArmies(toArmiesCount);
        toVisibleMapRegion.setArmies(toArmiesCount);

        fromFullMapRegion.setArmies(fromArmiesCount);
        fromVisibleMapRegion.setArmies(fromArmiesCount);            
    }    

    private void placeArmiesForPlayerAttack(AttackTransferMove placeArmiesMove) {
        Region toVisibleMapRegion = placeArmiesMove.getToRegion();
        Region fromVisibleMapRegion = placeArmiesMove.getFromRegion();        

        if (!fromVisibleMapRegion.ownedByPlayer(placeArmiesMove.getPlayerName()))
            return;
        
        if (toVisibleMapRegion.ownedByPlayer(placeArmiesMove.getPlayerName()))
            return;
        
        Region toFullMapRegion = state.getFullMap().getRegion(toVisibleMapRegion.getId());
        Region fromFullMapRegion = state.getFullMap().getRegion(fromVisibleMapRegion.getId());

        // updating from region
        int fromArmiesCount = fromVisibleMapRegion.getArmies() - placeArmiesMove.getArmies();
        if (fromArmiesCount < 1)
            return;
        else {
            fromFullMapRegion.setArmies(fromArmiesCount);
            fromVisibleMapRegion.setArmies(fromArmiesCount);                        
        }

        // updating to region
        int attackingArmies = placeArmiesMove.getArmies();
        int defendingArmies = toVisibleMapRegion.getArmies();
        
        // attack
        while (true) {
            if (defendingArmies == 0 || attackingArmies == 0)
                break;
            defendingArmies -= (rnd.nextInt(101) <= 60)? 1 : 0;
            attackingArmies -= (rnd.nextInt(101) <= 70)? 1 : 0;
        }

        // attack outcome
        if (defendingArmies > 0 && attackingArmies == 0) {
            toFullMapRegion.setArmies(defendingArmies);
            toVisibleMapRegion.setArmies(defendingArmies);            
        }
        if (defendingArmies == 0 && attackingArmies > 0) {
            toFullMapRegion.setArmies(attackingArmies);
            toVisibleMapRegion.setArmies(attackingArmies);
            toFullMapRegion.setPlayerName(placeArmiesMove.getPlayerName());
            toVisibleMapRegion.setPlayerName(placeArmiesMove.getPlayerName());            
        }
        if (defendingArmies == 0 && attackingArmies == 0) {
            toFullMapRegion.setArmies(1);
            toVisibleMapRegion.setArmies(1);
            toFullMapRegion.setPlayerName("neutral");
            toVisibleMapRegion.setPlayerName("neutral");            
        }

    }        

    private void performPlayerPlaceArmies(int armiesPerRound) {
        ArrayList<PlaceArmiesMove> placeArmiesMoves = playingBot.getPlaceArmiesMoves(state, TIMEOUT);
        for (PlaceArmiesMove placeArmiesMove : placeArmiesMoves) {
            if (armiesPerRound <= 0)
                break;
            armiesPerRound -= placeArmiesForPlayer(placeArmiesMove, armiesPerRound);
        }
    }
    
    private void performPlayerTransferArmies() {
        ArrayList<AttackTransferMove> transferMoves = playingBot.getAttackTransferMoves(state, TIMEOUT);
        for (AttackTransferMove transferMove : transferMoves) {
            if (transferMove.getToRegion().ownedByPlayer(transferMove.getPlayerName()))
                placeArmiesForPlayerTransfer(transferMove);
            else
                placeArmiesForPlayerAttack(transferMove);
        }        
    }
    
    private MyGameOutcome calculateGameOutcome(BotState state, String myPlayerName, String opponentPlayerName) {
        int myRegions = 0;
        int enemyRegions = 0;

        for (Region r : state.getFullMap().getRegions()) {
            if (r.ownedByPlayer(myPlayerName))
                myRegions++;
            if (r.ownedByPlayer(opponentPlayerName))
                enemyRegions++;
        }
        
        if (myRegions > 0 && enemyRegions == 0)
            return MyGameOutcome.WIN;
        if (myRegions == 0 && enemyRegions > 0)
            return MyGameOutcome.LOSE;
        if (myRegions > 0 && enemyRegions > 0)
            return MyGameOutcome.DRAW;
        
        return MyGameOutcome.UNDEFINED;
    }
    
    private MyGameOutcome simulateTestGame() {
        final int TOTAL_ROUNDS = 100;
        final int NUMBER_OF_PLAYERS = 2;
        final String[] playerNames = {"player1", "player2"};  // first player is mine
        
        for (int i = 0; i < TOTAL_ROUNDS; i++) {
            
            for (int j = 0; j < NUMBER_OF_PLAYERS; j++) {
                setActivePlayer(playerNames[j]);

                state.updateFogOfWar(playerNames[j]);
                
                state.initializeArmiesPerTurnAmount(playerNames[j]);
                
                performPlayerPlaceArmies(state.getStartingArmies());
                
                performPlayerTransferArmies();
            }
            
        }
        
        return calculateGameOutcome(state, playerNames[0], playerNames[1]);
    }
    
    private int calculateBotImprovement() {
        int myWinProbability = 0;
        int opponentWinProbability = 0;
        final int TOTAL_GAMES = 100;

        // return less than -1000 for errors
        
        for (int i = 0; i < TOTAL_GAMES; i++) {
            initializeGameSettings();
            
            if (checkMapSetupForTest().length() != 0 )
                return -1001;

            if (setInitialRegionsForPlayers().length() != 0)
                return -1002;

            MyGameOutcome outcome = simulateTestGame();            
            if (outcome == MyGameOutcome.WIN)
                myWinProbability++;
            if (outcome == MyGameOutcome.LOSE)
                opponentWinProbability++;
        }
        
        myWinProbability = (int)(100.0 * ((double)myWinProbability / (double)TOTAL_GAMES));
        opponentWinProbability = (int)(100.0 * ((double)opponentWinProbability / (double)TOTAL_GAMES));
        
        return myWinProbability - opponentWinProbability;
    }
    
    public String getSimulatedGameFailMessage() {
        if (myBot.getBotVersion() <= opponentBot.getBotVersion())
            return String.format("New bot version is not greater than old bot version (%d,%d)", myBot.getBotVersion(), opponentBot.getBotVersion());

        int botImprovement = calculateBotImprovement();
        
        if (botImprovement < -1000)
            return String.format("Some game failed with specific error (error no = %d)", botImprovement);
        
        if (botImprovement <= 0)
            return String.format("New bot version don't have non-negative improvement ! (improvement = %d%%)", botImprovement);
        
        return "";
    }
    
}
