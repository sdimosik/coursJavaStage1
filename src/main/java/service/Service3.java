package service;

import com.google.gson.Gson;
import model.Report;
import model.Schedule;
import model.Ship;
import model.Ship.TYPES_CARGO;
import utils.Constants.Date;
import utils.Constants.DateDetail;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

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

    private final ArrayList<List<Worker>> pool = new ArrayList<>(3);
    private final ArrayList<Queue<Ship>> queue = new ArrayList<>(3);
    private final ArrayList<LinkedList<Ship>> shipWorking = new ArrayList<>(3);
    private CyclicBarrier barrierStart;

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

            if (result == TYPES_EXIT.ERROR) {
                System.out.println("----------------------ERROR----------------------");
                break;
            } else if (result == TYPES_EXIT.ADD_CRANE_LOOSE) {
                System.out.println("----------------------ADD_CRANE_LOOSE----------------------");
                countCrane.set(result.getValue(), countCrane.get(result.getValue()) + 1);
            } else if (result == TYPES_EXIT.ADD_CRANE_LIQUID) {
                System.out.println("----------------------ADD_CRANE_LIQUID----------------------");
                countCrane.set(result.getValue(), countCrane.get(result.getValue()) + 1);
            } else if (result == TYPES_EXIT.ADD_CRANE_CONTAINER) {
                System.out.println("----------------------ADD_CRANE_CONTAINER----------------------");
                countCrane.set(result.getValue(), countCrane.get(result.getValue()) + 1);
            } else if (result == TYPES_EXIT.GOOD) {
                System.out.println("----------------------GOOD----------------------");
                Report r = calculateAndGetReport(countCrane);
                System.out.println(r);
                return TYPES_RETURN.GOOD;
            }
        }
        return TYPES_RETURN.ERROR;
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
               /* System.out.println("QUEUE size: " + queue.size());
                System.out.println("WORKING size: " + shipWorking.size());
                System.out.println("TAX sum: " + tax);*/
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


                if (zeroingShip(schedule)) {
                    System.out.println("Pool size: " + pool.size());
                    System.out.println("Tax sum: " + tax);
                    System.out.println("Day: " + daysLeft);
                    System.out.println("Min: " + minutesLeft);
                    clear(false);
                    return TYPES_EXIT.GOOD;
                }

                addShipQueue(schedule, daysLeft, minutesLeft);
                clearNullQueueWorking();
                for (int type = 0; type < 3; type++) {
                    for (Worker worker : pool.get(type)) {
                        if (worker.isFree()) {
                            if (!shipWorking.get(type).isEmpty()) {
                                synchronized (shipWorking.get(type).peek()) {
                                    Ship ship = shipWorking.get(type).getLast();
                                    shipWorking.get(type).remove(shipWorking.get(type).size() - 1);
                                    worker.setItem(ship, daysLeft, minutesLeft);
                                }
                            } else if (queue.get(type).peek() != null) {
                                Ship ship = queue.get(type).poll();
                                shipWorking.get(type).offerFirst(ship);
                                worker.setItem(ship, daysLeft, minutesLeft);
                            }
                        }
                    }
                }

                for (int type = 0; type < 3; type++) {
                    if (queue.get(type).size() > maxLengthQueue) {
                        maxLengthQueue = queue.get(type).size();
                    }
                    sumLengthQueue += queue.size();
                    countSumLengthQueue++;
                }
                try {
                    barrierStart.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            }
          /*  System.out.println("------------------(" + daysLeft + ")------------------");
            System.out.println("Pool size: " + pool.size());
            System.out.println("TAX sum: " + tax);*/
            //System.out.println("QUEUE: " + queue);
            //System.out.println(schedule);
        }
        clear(true);
        return TYPES_EXIT.ERROR;
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


        System.out.println("Waiting: " + barrierStart.getNumberWaiting());
        System.out.println("Part: " + barrierStart.getParties());
        try {
            barrierStart.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }


        for (int type = 0; type < 3; type++) {
            for (int i = 0; i < pool.get(type).size(); i++) {
                synchronized (pool.get(type).get(i)) {
                    pool.get(type).get(i).kill();
                }
            }
        }


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

    private boolean zeroingShip(Schedule schedule) {
        for (int i = 0; i < schedule.getList().size(); i++) {
            synchronized (schedule.getList().get(i)) {
                if (schedule.getList().get(i) != null) {
                    if (schedule.getList().get(i).isEmpty()) {
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
        barrierStart = new CyclicBarrier(countCrane.get(0) + countCrane.get(1) + countCrane.get(2) + 1);
        for (int type = 0; type < 3; type++) {
            for (int i = 0; i < countCrane.get(type); i++) {
                Worker w = new Worker(barrierStart);
                pool.get(type).add(w);
            }
        }
    }

    private void addShipQueue(Schedule schedule, int day, int minutes) {
        for (int i = 0; i < schedule.getList().size(); i++) {
            synchronized (schedule.getList().get(i)) {
                if (schedule.getList().get(i).getArrivalTime().equals(new Date(day, minutes))) {
                    TYPES_CARGO type = schedule.getList().get(i).getType();
                    queue.get(type.getValue()).offer(schedule.getList().get(i));
                }
            }
        }
    }

    private static class Worker extends Thread {
        private final CyclicBarrier start;
        private boolean isWorking = true;
        private Ship item = null;
        private int days;
        private int minutes;
        private boolean startUnload = false;

        public Worker(CyclicBarrier start) {
            this.start = start;
            //this.setDaemon(true);
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
            while (isWorking) {
                if (item != null) {
                    synchronized (item) {
                        if (item.isEmpty()) {
                            item = null;
                        } else {
                            if (startUnload) {
                                startUnload = false;
                                item.setUploadStart(new Date(days, minutes));
                            }

                            item.setQuantity(item.getQuantity() - 1);
                            if (item.isEmpty()) {
                                Date t = item.getUploadStart();
                                item.setUploadTime(
                                    new Date(days - t.getDays(), minutes - t.getMinutes()));
                                item = null;
                            }
                        }
                    }
                    days++;
                    minutes++;
                }

                try {
                    start.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
