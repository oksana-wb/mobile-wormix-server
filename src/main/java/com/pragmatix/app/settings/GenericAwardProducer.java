
package com.pragmatix.app.settings;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.12.2015 17:17
 */
public abstract class GenericAwardProducer {

    private static final ThreadLocal<Long> RANDOM_AWARD_SEED = new ThreadLocal<>();

    public abstract GenericAward getGenericAward();

    public boolean compressResult = true;

    public int key;

    public int price;

    protected long getRandomAwardSeed(){
        Long seed = RANDOM_AWARD_SEED.get();
        return seed != null  ? seed : 0L;
    }

    public static void setRandomAwardSeed(){
        RANDOM_AWARD_SEED.set(System.currentTimeMillis());
    }

    public static void removeRandomAwardSeed(){
        RANDOM_AWARD_SEED.remove();
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public boolean isCompressResult() {
        return compressResult;
    }

    public void setCompressResult(boolean compressResult) {
        this.compressResult = compressResult;
    }
}
