package UDP;

public class sensor {
    private float sum = 0;
    private int count = 0;

    public synchronized void addValue(float val) {
        sum += val;
        count++;
    }

    public synchronized float getAverage() {
        return count == 0 ? 0 : sum / count;
    }
}

