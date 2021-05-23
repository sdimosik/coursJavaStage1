package model;

import lombok.Data;
import utils.Constants.Date;

@Data
public class Ship implements Comparable<Ship> {
    public static enum TYPES_CARGO {
        LOOSE(0),
        LIQUID(1),
        CONTAINER(2);

        private final int value;

        TYPES_CARGO(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    @Data
    public static class Tax {
        private int minutes = 0;
        private int cash;

        @Override
        public String toString() {
            return "Tax{" +
                "minutes=" + minutes +
                ", cash=" + cash +
                '}';
        }
    }

    private Date arrivalTime;
    private String name;
    private TYPES_CARGO type;
    private Integer parkingTime;
    private Integer countCraneExecuting = 0;
    private Tax tax = new Tax();
    private Integer quantity;

    private Date uploadStart;
    private Date uploadTime;

    public Ship(Date arrivalTime, String name, TYPES_CARGO type, Integer quantity, int parkingTime) {
        this.arrivalTime = arrivalTime;
        this.name = name;
        this.type = type;
        this.quantity = quantity;
        this.parkingTime = parkingTime;
    }

    public boolean checkAndCalculateTax() {
        tax.minutes++;
        if (tax.minutes % 60 == 0) {
            tax.cash += 100;
            return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return quantity <= 0;
    }

    @Override
    public int compareTo(Ship o) {
          /*  final int diff = this.getShip().getType().compareTo(o.getShip().getType());
            return diff != 0 ? diff : this.getShip().getArrivalTime()
                .compareTo(o.getShip().getArrivalTime());*/
        return this.getArrivalTime().compareTo(o.getArrivalTime());
    }

    @Override
    public String toString() {
        return "\nShip{" +
            "arrivalTime=" + arrivalTime +
            ", name='" + name + '\'' +
            ", type=" + type +
            ", parkingTime=" + parkingTime +
            ", countCraneExecuting=" + countCraneExecuting +
            ", tax=" + tax +
            ", quantity=" + quantity +
            '}';
    }
}
