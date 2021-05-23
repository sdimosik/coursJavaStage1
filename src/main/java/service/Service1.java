package service;

import model.Schedule;
import model.Ship;
import utils.Constants;
import utils.Utils;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import utils.Constants.Date;
import static utils.Utils.generateInt;

public class Service1 {

    public Schedule generateSchedule(int count) {
        LinkedList<Ship> list = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            list.add(generateItemInSchedule(i));
            System.out.print(list.get(i));
        }

        return new Schedule(list);
    }

    private Ship generateItemInSchedule(int position) {
        int dateD = generateInt(1, 30);
        int dateM = generateInt(0, 1439);
        String name = generateName(position);
        Ship.TYPES_CARGO type = generateType();
        int quantity = generateInt(1, 2000);
        int parkingTime = quantity;

        return new Ship(
            new Date(dateD, dateM),
            name,
            type,
            quantity,
            parkingTime
        );
    }

    private String generateName(int number) {
        return "ship-" + number;
    }

    private Ship.TYPES_CARGO generateType() {
        int pos = (int) (Math.random() * 3);
        if (pos == 0) {
            return Ship.TYPES_CARGO.CONTAINER;
        } else if (pos == 1) {
            return Ship.TYPES_CARGO.LIQUID;
        } else {
            return Ship.TYPES_CARGO.LOOSE;
        }
    }
}

