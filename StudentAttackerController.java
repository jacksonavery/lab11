package edu.ufl.cise.cs1.controllers;

import game.controllers.AttackerController;
import game.models.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public final class StudentAttackerController implements AttackerController
{
	//arbitrary comment

	int state;

	public void init(Game game) {
        state = 0;
	}

	public void shutdown(Game game) { }

	public int update(Game game,long timeDue)
	{
		int action = -1;

		// state machine, 0 is standard pill eating and 1 is retreat //
		if (state == 0) {               //default state
            //this part just moves towards the nearest pill, i realize it's like a 7 IQ play but this bit alone
            //scores an average of 4098.7
            List<Node> pillList = game.getPillList();
            if (pillList.size() > 0) {
                Node bestAction = game.getAttacker().getTargetNode(pillList, true);
                action = game.getAttacker().getNextDir(bestAction, true);
            }

            //stall in front of pill for max safe time
            if (game.checkPowerPill(game.getAttacker().getLocation().getNeighbor(action))) {
                //wait around till enemy approaches
                boolean reverse = true;
                for (int i = 0; i < 4; i++) {
                    if (game.getAttacker().getLocation().getPathDistance(game.getDefender(i).getLocation()) < 5) {
                        reverse = false;
                        break;  //continue moving if enemy too close, otherwise call code below and stall
                    }
                }
                //this is ugly but it successfully reverses action so don't judge me
                if (reverse) {
                    action = (action + 2) % 4;
                }
            }
        } else if (state == 1) {
            List<Integer> possibleDirs = game.getAttacker().getPossibleDirs(true);
            int badAction;
            for (int i = 0; i < 4; i++) {                                                                    //iterates through all enemies
                if (game.getAttacker().getLocation().getPathDistance(game.getDefender(i).getLocation()) < 20 && game.getAttacker().getLocation().getPathDistance(game.getDefender(i).getLocation()) > 0) {     //make sure we only tst against close enemies outside the lair
                    badAction = game.getAttacker().getNextDir(game.getDefender(i).getLocation(), true);    //set the fastest route towards an enemy as "badAction"
                    for (int j = 0; j < possibleDirs.size(); j++) {                                          //this for loop removes "badAction" from the list of possibleDirs
                        if (badAction == possibleDirs.get(j)) {
                            possibleDirs.remove(new Integer(badAction));
                            break;
                        }
                    }
                }
            }
            if (possibleDirs.size() == 1) {   //if there is only 1 action that has not been removed, it must be the only good action
                action = possibleDirs.get(0);

            } else if (possibleDirs.size() > 1) { //need choose an action if there are multiple not bad actions
                //find the fastest route to (power) pill
                int ppAction = -1;
                if (game.getPowerPillList().size() > 0) {
                    ppAction = game.getAttacker().getNextDir(game.getAttacker().getTargetNode(game.getPowerPillList(), true), true);
                }
                int pillAction = game.getAttacker().getNextDir(game.getAttacker().getTargetNode(game.getPillList(), true), true);

                //make sure the route to pill isn't suicidal, and if it isn't then go with it, then after check the same way for power pills bc they are higher priority and will override this decision
                boolean hasChosen = false;
                for (int i = 0; i < possibleDirs.size(); i++) {
                    if (pillAction == possibleDirs.get(i)) {
                        action = pillAction;
                        hasChosen = true;
                        break;
                    }
                }
                for (int i = 0; i < possibleDirs.size(); i++) {
                    if (ppAction == possibleDirs.get(i)) {
                        action = ppAction;
                        hasChosen = true;
                        break;
                    }
                }
                //in case neither option is good, just go w/ an arbitrary one
                if (!hasChosen)
                    action = possibleDirs.get(0);

            } else { //if the possible dirs size is 0, then there are two (or more) enemies closing on pacman. in this case it makes sense to run towards the nearest intersection so that pacman has more options
                List<Node> nodes = game.getCurMaze().getPillNodes();
                List<Node> junctionNodes = new ArrayList<>();
                for (int i = 0; i < nodes.size(); i++) { //this for loop only adds nodes that are junctions to junctionnodes
                    if (nodes.get(i).isJunction()) {
                        junctionNodes.add(nodes.get(i));
                    }
                }
                action = game.getAttacker().getNextDir(game.getAttacker().getTargetNode(junctionNodes, true), true);
            }

        } else { //state 2
            Node[] enemyLocs = new Node[4]; //this bit finds the nearest enemy so pacman chases the right one
            for (int i = 0; i < 4; i++) {
                enemyLocs[i] = game.getDefender(i).getLocation();
            }
            Node closest = game.getAttacker().getTargetNode(Arrays.asList(enemyLocs), true);
            action = game.getAttacker().getNextDir(closest, true);
        }


		//       runs state machine      //
		state = 0;

        //state 2, chasing enemies
        for (int i = 0; i < 4; i++) {
            if (game.getAttacker().getLocation().getPathDistance(game.getDefender(i).getLocation()) < 15) {
                if (game.getDefender(i).isVulnerable()) { //test for vulnerability
                    state = 2;
                    break;
                }
            }
        }

        //state 1, running from enemies
        HashMap<Integer, Defender> enemiesOut = new HashMap<>(); //this bit filters out enemies in spawn from list of enemies to test
        int offset = 0;
        for (int i = 0; i < 4; i++) {
            if (game.getDefender(i).getLairTime() < 1 ) {
                enemiesOut.put(offset, game.getDefender(i));
                offset++;
            }
        }
        for (int i = 0; i < enemiesOut.size(); i++) {
            if (game.getAttacker().getLocation().getPathDistance(enemiesOut.get(i).getLocation()) < 20) {
                if (game.getPowerPillList().size() != 0 && game.getAttacker().getLocation().getPathDistance(game.getAttacker().getTargetNode(game.getPowerPillList(),true)) > 8) { //checks if state 0 is already handling it w/ stalling by a pill
                    if (!enemiesOut.get(i).isVulnerable()) { //no need to run away if enemy is disabled
                        state = 1;
                        break;
                    }
                }       //code is duplicated so we don't test powerpill list if there are no pills in it
                else if (game.getPowerPillList().size() == 0) {
                    if (!enemiesOut.get(i).isVulnerable()) { //no need to run away if enemy is disabled
                        state = 1;
                        break;
                    }
                }
            }
        }

		return action;
    }
}