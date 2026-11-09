package com.example.chessbot;
public final class MoveTrackerTest {
    private static void require(boolean condition,String message) {
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args) {
        MoveTracker tracker=new MoveTracker();
        require(tracker.history().isEmpty(),"Initial history must be empty");
        require(tracker.fen().startsWith("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w"),"Initial board wrong");
        String synced=tracker.syncHistory("g1f3 g8f6");
        require(synced.contains("2 hamle"),"Sync failed: "+synced);
        require(tracker.history().equals("g1f3 g8f6"),"History lost");
        require(tracker.fen().startsWith("rnbqkb1r/pppppppp/5n2/8/8/5N2/PPPPPPPP/RNBQKB1R w"),"Knight FEN wrong");
        tracker.reset();
        double[] changes=new double[64];
        changes[7*8+6]=80; // g1
        changes[5*8+5]=68; // f3
        changes[0*8+6]=150; // g8
        changes[2*8+5]=95; // f6
        String result=tracker.observeSequence(changes);
        require(result.contains("g1f3 g8f6"),"Two-move recognition failed: "+result);
        require(tracker.history().equals("g1f3 g8f6"),"Recognized moves not recorded");
        System.out.println("MoveTracker tests passed");
    }
}
