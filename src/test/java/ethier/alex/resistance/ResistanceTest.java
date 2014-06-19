package ethier.alex.resistance;

import ethier.alex.world.addon.FilterListBuilder;
import ethier.alex.world.core.data.*;
import ethier.alex.world.core.processor.SimpleProcessor;
import ethier.alex.world.processor.Query;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

/**

 @author alex
 */
public class ResistanceTest {

    private static Logger logger = Logger.getLogger(ResistanceTest.class);

    @BeforeClass
    public static void setUpClass() {
        BasicConfigurator.configure();
    }

    @Test
    public void testResistance() throws Exception {
        System.out.println("");
        System.out.println("");
        System.out.println("********************************************");
        System.out.println("********         Basic Test        *********");
        System.out.println("********************************************");
        System.out.println("");
        System.out.println("");

        List<String> players = new ArrayList<String>();
        players.add("alex");
        players.add("john");
        players.add("liz");
        players.add("liban");
        players.add("jeanie");

        int[] rounds = {2, 3, 2, 3, 3};

        Game game = new Game(players, 2, rounds);

        Set<String> round1Players = new HashSet<String>();
        round1Players.add("alex");
        round1Players.add("john");
        game.playRound(round1Players, 1);

        Set<String> round2Players = new HashSet<String>();
        round2Players.add("jeanie");
        round2Players.add("liban");
        round2Players.add("liz");
        game.playRound(round2Players, 1);
        
//        Set<String> round3Players = new HashSet<String>();
//        round3Players.add("alex");
//        round3Players.add("liban");
//        game.playRound(round3Players, 1);

        Partition gamePartition = game.createRootPartition();

        SimpleProcessor simpleProcessor = new SimpleProcessor(gamePartition);
        simpleProcessor.runAll();
        Collection<ElementList> elements = simpleProcessor.getCompletedPartitions();
        for (ElementList element : elements) {
            System.out.println("Element Found: " + element);
        }
        
        
        Query queryManager = new Query(gamePartition.getRadices(), elements);
        for(int i=0; i < players.size();i++) {
            String player = players.get(i);
            
            String queryStr = StringUtils.leftPad("", i, "*");
            queryStr += "1";
            queryStr = StringUtils.rightPad(queryStr, gamePartition.getRadices().length, "*");
                        
            FilterList query = FilterListBuilder.newInstance()
                    .setQuick(queryStr)
                    .getFilterList();
            
            logger.info("Query: " + query);
            
            double probSpy = queryManager.query(query);
            logger.info("Player: " + player + " has probability of being a spy: " + probSpy);
        }
        
    }
}
