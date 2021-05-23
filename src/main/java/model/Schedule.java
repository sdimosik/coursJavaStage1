package model;

import lombok.Data;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Data
public class Schedule {

    private List<Ship> list = new LinkedList<>();

    public Schedule(List<Ship> list) {
        this.list.addAll(list);
    }

    public void add(Ship item) {
        list.add(item);
    }

    public void sortByArrivalTime(boolean isAsc) {
        if (isAsc) {
            Collections.sort(list);
        } else {
            Collections.sort(list, Collections.reverseOrder());
        }
    }

    public boolean isAllNull() {
        for (Ship ship : list) {
            if (ship != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "Schedule{" +
            "list=" + list +
            '}';
    }
}
