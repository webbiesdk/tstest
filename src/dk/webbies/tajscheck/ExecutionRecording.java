package dk.webbies.tajscheck;

/**
 * Created by erik1 on 08-11-2016.
 */
public class ExecutionRecording {
    public final int[] testSequence;
    public final String seed;

    public ExecutionRecording(int[] testSequence, String seed) {
        this.testSequence = testSequence;
        this.seed = seed;
    }
}
