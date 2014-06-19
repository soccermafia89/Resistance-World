/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ethier.alex.resistance;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import ethier.alex.world.addon.FilterListBuilder;
import ethier.alex.world.addon.PartitionBuilder;
import ethier.alex.world.core.data.FilterList;
import ethier.alex.world.core.data.Partition;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**

 String schema:
 1=spy, 0=resistance
 2=did not go on mission, 1=spy vote, 0=resistance vote

 For a 3 player game:

 100|103|130|300|300|300 is a game where the first player is a spy and on the first mission they went on with the last player.

 The spy threw a fail. On the second mission the spy went on a mission with the second player and threw a fail.
 On subsequent missions the spy was left off the missions.


 @author alex
 */
public class Game {

    private static Logger logger = Logger.getLogger(Game.class);
    Collection<FilterList> filters;
    int roundCount = 0;
    private BiMap<String, Integer> players;
    private int[] roundVotes;
    private int[] radices;
    private int spies;

    public Game(List<String> myPlayers, int mySpies, int[] myRoundVotes) {
        roundVotes = myRoundVotes;
        filters = new ArrayList<FilterList>();
        players = HashBiMap.create();
        spies = mySpies;

        logger.info("Player Map");
        for (int i=0; i < myPlayers.size(); i++) {
            String player = myPlayers.get(i);
            logger.info(player + " <=> " + i);
            
            players.put(player, i);
        }

        radices = new int[6 * myPlayers.size()];

        for (int i = 0; i < myPlayers.size(); i++) {
            radices[i] = 2;
        }

        for (int i = myPlayers.size(); i < radices.length; i++) {
            radices[i] = 3;
        }

        this.createGameFilters();
    }
    
    public void createGameFilters() {
        // Create the rule that only x number of spies exist

        int[] playerRadices = new int[players.size()];

        String breakStr = "";
        for (int i = 0; i < players.size(); i++) {
            breakStr += '1';
            playerRadices[i] = 2;
        }

        String genFilterStr = "";
        int count = 0;
        while (true) {
            if (genFilterStr.equals(breakStr)) {
                break;
            }

            genFilterStr = "" + Integer.toBinaryString(count);
            genFilterStr = StringUtils.leftPad(genFilterStr, players.size(), '0');
            int combOnes = StringUtils.countMatches(genFilterStr, "1");
            if (combOnes != spies) {
                String filterStr = StringUtils.rightPad(genFilterStr, radices.length, "*");
                FilterList newFilter = FilterListBuilder.newInstance()
                        .setQuick(filterStr)
                        .getFilterList();
                
                filters.add(newFilter);
                logger.info("Adding base filter: " + newFilter);
            }

            count++;
        }  
    }
    
    

    public Partition createRootPartition() {
        return PartitionBuilder.newInstance().setBlankWorld().setRadices(radices).addFilters(filters).getPartition();
    }

//    public Collection<FilterList> getFilters() {
//        return filters;
//    }
    public void playRound(Set<String> agents, int failures) {

        if (agents.size() != roundVotes[roundCount]) {
            logger.error("Round Vote Limit: " + roundVotes[roundCount]);
            logger.error("Players Sent: " + agents.size());
            throw new RuntimeException("Number of players on mission does not match mission count!");
        }

        Collection<FilterList> filters = new ArrayList<FilterList>();
        int votes = roundVotes[roundCount];

        // Add filters due to vote outcome
        HashSet<Integer> voteFilterOffsets = this.generateVoteFilterOffsets(agents);
        Collection<String> voteFilterStrings = this.generateVoteFilterStrings(failures);
        this.createVoteFilters(voteFilterStrings, voteFilterOffsets);

        //Add filters due to the players sent on the mission.
        this.createMissionFilters(agents);
        roundCount++;
    }

    //2 == player did not go on mission.  We remove these cases for the players that DID go.
    public void createMissionFilters(Set<String> agents) {

        BiMap<Integer, String> inversePlayers = players.inverse();

        for (int i = 0; i < players.size(); i++) {
            String agentStr = inversePlayers.get(i);
            
            if (!agents.contains(agentStr)) {
                
                StringBuilder filterStringBuilder = new StringBuilder();
                
                int missionOffset = (roundCount + 1) * players.size() + i;
                filterStringBuilder.append(StringUtils.leftPad("", missionOffset, "*"));
                filterStringBuilder.append("2");
                filterStringBuilder.append(StringUtils.leftPad("", radices.length - missionOffset - 1, "*"));
                
                FilterList newFilter = FilterListBuilder.newInstance()
                        .setQuick(filterStringBuilder.toString())
                        .getFilterList();
                
                logger.info("Adding Miss Filter: " + newFilter);
                filters.add(newFilter);
            }
        }
    }

//    public HashSet<Integer> generateMissionOffsets(Set<String> agents) {
//
//        int baseMissionOffset = players.size() * roundCount;
//        HashSet<Integer> missionOffsets = new HashSet<Integer>();
//
//        for (String agent : agents) {
//            int agentOffset = players.get(agent);
//
//            int missionOffset = baseMissionOffset + agentOffset;
//            missionOffsets.add(missionOffset);
//        }
//
//        return missionOffsets;
//    }
    public void createVoteFilters(Collection<String> voteFilterStrings, HashSet<Integer> voteFilterOffsets) {

        for (String voteFilterString : voteFilterStrings) {
            StringBuilder filterStringBuilder = new StringBuilder();
            int agentCount = 0;

            for (int i = 0; i < radices.length; i++) {

                if (voteFilterOffsets.contains(i)) {
                    String agentState = voteFilterString.substring(agentCount, agentCount + 1);
                    filterStringBuilder.append(agentState);
                    agentCount++;
                } else {
                    filterStringBuilder.append("*");
                }
            }

            FilterList newFilter = FilterListBuilder.newInstance().setQuick(filterStringBuilder.toString()).getFilterList();
            logger.info("Adding vote Filter: " + newFilter);
            filters.add(newFilter);
        }
    }

    //Used in conjunction with the output of 'generateVoteFilterStrings'
    //Gives the offset values to transform the strings into filters.
    public HashSet<Integer> generateVoteFilterOffsets(Set<String> agents) {

        HashSet<Integer> voteFilterOffsets = new HashSet<Integer>();

        for (String agent : agents) {
            int agentOffset = players.get(agent);
            voteFilterOffsets.add(agentOffset);
        }

        return voteFilterOffsets;
    }

    //Generates a string that represents the filters that should be created.  1 = spy, 0 = resistance.
    //These set of filters are based on the outcome of the votes after a mission outcome is seen.
    public Collection<String> generateVoteFilterStrings(int failures) {

        int numVotes = roundVotes[roundCount];

        Collection<String> filterStrings = new ArrayList<String>();
        int[] voteRadices = new int[numVotes];

        String breakStr = "";
        for (int i = 0; i < numVotes; i++) {
            breakStr += '1';
            voteRadices[i] = 2;
        }

        String filterStr = "";
        int count = 0;
        while (true) {
            if (filterStr.equals(breakStr)) {
                break;
            }

            filterStr = "" + Integer.toBinaryString(count);
            filterStr = StringUtils.leftPad(filterStr, numVotes, '0');
            int combOnes = StringUtils.countMatches(filterStr, "1");
            if (combOnes < failures) {
                filterStrings.add(filterStr);
            }


            count++;
        }

        return filterStrings;
    }
}
