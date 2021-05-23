package model;

import lombok.Data;
import utils.Constants.DateDetail;
import utils.Constants.Date;

import java.util.LinkedList;
import java.util.List;

@Data
public class Report {

    @Data
    public static class Item {
        private String name;
        private DateDetail arrivalTime;
        private DateDetail waitingTime;
        private Date uploadStart;
        private Date uploadTime;

        public Item(String name, DateDetail arrivalTime, DateDetail waitingTime, Date uploadStart, Date uploadTime) {
            this.name = name;
            this.arrivalTime = arrivalTime;
            this.waitingTime = waitingTime;
            this.uploadStart = uploadStart;
            this.uploadTime = uploadTime;
        }

        @Override
        public String toString() {
            return "\nItem{" +
                "name='" + name + '\'' +
                ", arrivalTime=" + arrivalTime +
                ", waitingTime=" + waitingTime +
                ", uploadStart=" + uploadStart +
                ", uploadTime=" + uploadTime +
                '}';
        }
    }

    private List<Item> ships = new LinkedList<>();

    private Integer countUnload;
    private Double lengthUnloadQueueAvg;
    private Double avgTimeUnloadQueue;
    private Double avgDelayUnload;
    private Integer maxDelayUnload;
    private Integer sumTax;
    private Integer countCraneLOOSE;
    private Integer countCraneLIQUID;
    private Integer countCraneCONTAINER;

    @Override
    public String toString() {
        return "Report{" +
            "ships=" + ships +
            ",\n countUnload=" + countUnload +
            ",\n lengthUnloadQueueAvg=" + lengthUnloadQueueAvg +
            ",\n avgTimeUnloadQueue=" + avgTimeUnloadQueue +
            ",\n avgDelayUnload=" + avgDelayUnload +
            ",\n maxDelayUnload=" + maxDelayUnload +
            ",\n sumTax=" + sumTax +
            ",\n countCraneLOOSE=" + countCraneLOOSE +
            ",\n countCraneLIQUID=" + countCraneLIQUID +
            ",\n countCraneCONTAINER=" + countCraneCONTAINER +
            '}';
    }
}
