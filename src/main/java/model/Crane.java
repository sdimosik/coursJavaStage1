package model;

import utils.Constants;

import java.util.concurrent.Phaser;

public class Crane extends Thread {
    private boolean isWorking = true;
    private Ship item = null;
    private int days;
    private int minutes;
    private boolean startUnload = false;
    private final Phaser phaser;

    public Crane(Phaser phaser) {
        this.phaser = phaser;
        phaser.register();
        this.start();
    }

    public void setItem(Ship item, int day, int minutes) {
        this.item = item;
        this.days = day;
        this.minutes = minutes;
        startUnload = true;
    }

    public void kill() {
        isWorking = false;
    }

    public Ship getItem() {
        return item;
    }

    public boolean isFree() {
        return item == null;
    }

    @Override
    public void run() {
        while (true) {
            if (isWorking) {
                phaser.arriveAndAwaitAdvance();
            } else {
                phaser.arriveAndDeregister();
                break;
            }

            if (item != null) {
                synchronized (item) {
                    if (item.isEmpty()) {
                        item = null;
                    } else {
                        if (startUnload) {
                            startUnload = false;
                            item.setUploadStart(new Constants.Date(days, minutes));
                        }

                        item.setQuantity(item.getQuantity() - 1);
                        if (item.isEmpty()) {
                            item = null;
                        }
                    }
                }
            }
        }
    }
}
