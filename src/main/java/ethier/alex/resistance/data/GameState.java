/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ethier.alex.resistance.data;

/**

 @author alex
 */
public class GameState {
    
    private int[] state;
    
    public GameState(int mySlots) {
        state = new int[mySlots];
        
        for(int i=0; i < state.length;i++) {
            state[i] = -1;
        }
    }
}
