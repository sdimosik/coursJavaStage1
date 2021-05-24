package service;

import com.google.gson.Gson;
import model.Crane;
import model.Report;
import model.Schedule;
import model.Ship;
import model.Ship.TYPES_CARGO;
import utils.Constants;
import utils.Constants.Date;
import utils.Constants.DateDetail;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Phaser;

import static utils.Constants.CRANE_COAST;
import static utils.Constants.JSON_PATH;
import static utils.Utils.generateInt;

public class Service3 {
    public enum TYPES_EXIT {
        ERROR(-2),
        GOOD(-1),
        ADD_CRANE_LOOSE(0),
        ADD_CRANE_LIQUID(1),
        ADD_CRANE_CONTAINER(2);

        private final int value;

        private TYPES_EXIT(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static enum TYPES_RETURN {
        ERROR(0),
        GOOD(1);

        private final int value;

        private TYPES_RETURN(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private final Gson gson = new Gson();
    private Schedule scheduleStock;
    private final List<Ship> report = new LinkedList<>();

    private final ArrayList<Integer> tax = new ArrayList<>(3);
    private double sumLengthQueue = 0;
    private int countSumLengthQueue = 0;
    private int maxLengthQueue = 0;

    private final ArrayList<List<Crane>> pool = new ArrayList<>(3);
    private final ArrayList<Queue<Ship>> queue = new ArrayList<>(3);
    private final ArrayList<LinkedList<Ship>> shipWorking = new ArrayList<>(3);
    private Phaser phaser;

    public TYPES_RETURN run() {
        readSchedule();
        generateAndSetData();
        scheduleStock.sortByArrivalTime(true);

        System.out.println(scheduleStock);

        return simulator();
    }

    private void fillField() {
        for (int i = 0; i < 3; i++) {
            pool.add(new LinkedList<>());
            queue.add(new LinkedList<>());
            shipWorking.add(new LinkedList<>());
            tax.add(0);
        }
    }

    private TYPES_RETURN simulator() {
        fillField();
        ArrayList<Integer> countCrane = new ArrayList<>(3);
        for (int type = 0; type < 3; type++) {
            countCrane.add(1);
        }

        while (true) {
            fillSetting(countCrane);
            Schedule schedule = new Schedule(scheduleStock.getList());
            TYPES_EXIT result = calculate(schedule);
            System.out.println(result);

            switch (result) {
                case ERROR:
                    return TYPES_RETURN.ERROR;
                case GOOD:
                    System.out.println(calculateAndGetReport(countCrane));
                    return TYPES_RETURN.GOOD;
                case ADD_CRANE_LIQUID:
                case ADD_CRANE_LOOSE:
                case ADD_CRANE_CONTAINER:
                    countCrane.set(result.getValue(), countCrane.get(result.getValue()) + 1);
            }
        }
    }

    Report calculateAndGetReport(ArrayList<Integer> countCrane) {
        Report r = new Report();

        int sumDelayQueue = report.get(0).getTax().getMinutes();
        int maxDelay = report.get(0).getTax().getMinutes();
        for (Ship ship : report) {
            Report.Item item = new Report.Item(
                ship.getName(),
                new DateDetail(ship.getArrivalTime()),
                new DateDetail(ship.getTax().getMinutes()),
                ship.getUploadStart(),
                ship.getUploadTime()
            );
            r.getShips().add(item);

            sumDelayQueue += ship.getTax().getMinutes();
            if (ship.getTax().getMinutes() > maxDelay) {
                maxDelay = ship.getTax().getMinutes();
            }
        }

        r.setCountUnload(report.size());
        r.setSumTax(tax.get(0) + tax.get(1) + tax.get(2));
        r.setLengthUnloadQueueAvg(sumLengthQueue / countSumLengthQueue);
        r.setAvgTimeUnloadQueue((double) (sumDelayQueue / report.size()));
        r.setAvgDelayUnload(sumLengthQueue / countSumLengthQueue);
        r.setMaxDelayUnload(maxLengthQueue);
        r.setCountCraneLOOSE(countCrane.get(0));
        r.setCountCraneLIQUID(countCrane.get(1));
        r.setCountCraneCONTAINER(countCrane.get(2));

        return r;
    }

    TYPES_EXIT calculate(Schedule schedule) {
        for (int daysLeft = -8; daysLeft < Integer.MAX_VALUE; daysLeft++) {
            for (int minutesLeft = 0; minutesLeft < 1440; minutesLeft++) {
                if (calculateTax(TYPES_CARGO.LOOSE)) {
                    clear(true);
                    return TYPES_EXIT.ADD_CRANE_LOOSE;
                } else if (calculateTax(TYPES_CARGO.LIQUID)) {
                    clear(true);
                    return TYPES_EXIT.ADD_CRANE_LIQUID;
                } else if (calculateTax(TYPES_CARGO.CONTAINER)) {
                    clear(true);
                    return TYPES_EXIT.ADD_CRANE_CONTAINER;
                }


                if (zeroingShip(schedule, daysLeft, minutesLeft)) {
                    clear(false);
                    return TYPES_EXIT.GOOD;
                }

                addShipQueue(schedule, daysLeft, minutesLeft);
                clearNullQueueWorking();
                distributionShip(daysLeft, minutesLeft);
                calculateSomeStatistic();

                phaser.arriveAndAwaitAdvance();
            }
        }
        return TYPES_EXIT.ERROR;
    }

    private void calculateSomeStatistic() {
        for (int type = 0; type < 3; type++) {
            if (queue.get(type).size() > maxLengthQueue) {
                maxLengthQueue = queue.get(type).size();
            }
            sumLengthQueue += queue.size();
            countSumLengthQueue++;
        }
    }

    private void distributionShip(int daysLeft, int minutesLeft) {
        for (int type = 0; type < 3; type++) {
            for (Crane crane : pool.get(type)) {
                if (crane.isFree()) {
                    if (!shipWorking.get(type).isEmpty()) {
                        synchronized (shipWorking.get(type).peek()) {
                            Ship ship = shipWorking.get(type).getLast();
                            shipWorking.get(type).remove(shipWorking.get(type).size() - 1);
                            crane.setItem(ship, daysLeft, minutesLeft);
                        }
                    } else if (queue.get(type).peek() != null) {
                        Ship ship = queue.get(type).poll();
                        shipWorking.get(type).offerFirst(ship);
                        crane.setItem(ship, daysLeft, minutesLeft);
                    }
                }
            }
        }
    }

    private void clearNullQueueWorking() {
        for (int type = 0; type < 3; type++) {
            for (int i = shipWorking.get(type).size() - 1; i >= 0; i--) {
                if (shipWorking.get(type).get(i) == null) {
                    shipWorking.get(type).remove(i);
                }
            }
        }
    }

    private void clear(boolean isAllClear) {
        for (int type = 0; type < 3; type++) {
            for (int i = 0; i < pool.get(type).size(); i++) {
                synchronized (pool.get(type).get(i)) {
                    pool.get(type).get(i).kill();
                }
            }
        }
        phaser.arriveAndAwaitAdvance();


        for (int type = 0; type < 3; type++) {
            pool.get(type).clear();
            queue.get(type).clear();
            shipWorking.get(type).clear();

            if (isAllClear) {
                tax.set(type, 0);
                countSumLengthQueue = 0;
                sumLengthQueue = 0;
                maxLengthQueue = 0;
                report.clear();
            }
        }
    }

    private boolean calculateTax(TYPES_CARGO type) {
        final int pos = type.getValue();
        for (Ship ship : queue.get(pos)) {
            if (ship.checkAndCalculateTax()) {
                tax.set(pos, tax.get(pos) + 100);
            }
        }
        int sum = 0;
        for (int i = 0; i < 3; i++) {
            sum += tax.get(i);
        }
        return sum >= CRANE_COAST;
    }

    private boolean zeroingShip(Schedule schedule, int daysLeft, int minutesLeft) {
        for (int i = 0; i < schedule.getList().size(); i++) {
            synchronized (schedule.getList().get(i)) {
                if (schedule.getList().get(i) != null) {
                    if (schedule.getList().get(i).isEmpty()) {
                        Constants.Date startTime = schedule.getList().get(i).getUploadStart();
                        int d = Math.abs(daysLeft - startTime.getDays());
                        int m = minutesLeft - startTime.getMinutes();

                        if (d > 0) {
                            m += 1440 ;
                        }

                        schedule.getList().get(i).setUploadTime(new Date(d, m));
                        report.add(schedule.getList().get(i));
                        schedule.getList().remove(i);
                    }
                }
            }
        }
        return schedule.getList().isEmpty();
    }

    private void readSchedule() {
        try {
            scheduleStock = gson.fromJson(new FileReader(JSON_PATH + "schedule.json"), Schedule.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void generateAndSetData() {
        for (Ship item : scheduleStock.getList()) {
            int type = item.getType().getValue();
            int weight = item.getQuantity();
            int k = generateInt(0, 9);
            int unloadDelayMinutes = k * type * weight; // max 1440
            int arrivalErrorDays = generateInt(-7, 7);

            item.setArrivalTime(new Date(item.getArrivalTime().getDays()
                - arrivalErrorDays, item.getArrivalTime().getMinutes()));

            int minutesToUnload = unloadDelayMinutes + item.getParkingTime();
            item.setParkingTime(minutesToUnload);
        }
    }

    private void fillSetting(ArrayList<Integer> countCrane) {
        phaser = new Phaser(1);
        for (int type = 0; type < 3; type++) {
            for (int i = 0; i < countCrane.get(type); i++) {
                Crane w = new Crane(phaser);
                pool.get(type).add(w);
            }
        }
    }

    private void addShipQueue(Schedule schedule, int daysLeft, int minutesLeft) {
        for (int i = 0; i < schedule.getList().size(); i++) {
            synchronized (schedule.getList().get(i)) {
                if (schedule.getList().get(i).getArrivalTime().equals(new Date(daysLeft, minutesLeft))) {
                    TYPES_CARGO type = schedule.getList().get(i).getType();
                    queue.get(type.getValue()).offer(schedule.getList().get(i));
                }
            }
        }
    }
}
