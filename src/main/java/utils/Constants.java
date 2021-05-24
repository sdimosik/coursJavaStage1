package utils;

import lombok.Data;

public class Constants {
    public static final String JSON_PATH = "src\\main\\java\\json\\";

    public static final int CRANE_COAST = 30000;

    public static final int TAX_PER_HOURS = 100;

    public static final int MINUTES_IN_30_DAYS = 43200;
    public static final int MINUTES_IN_HOUR = 60;

    @Data
    public static class Date implements Comparable<Date> {
        private Integer days;
        private Integer minutes;

        public Date(Integer days, Integer minutes) {
            this.days = days;
            this.minutes = minutes;
        }

        public Date(int minutes) {
            this.days = minutes / 1440;
            this.minutes = minutes % 1440;
        }

        @Override
        public String toString() {
            return "Date{" +
                "days=" + days +
                ", minutes=" + minutes +
                '}';
        }

        @Override
        public int compareTo(Date o) {
            int d = this.days.compareTo(o.days);
            if (d != 0) {
                return d;
            } else {
                return this.minutes.compareTo(o.minutes);
            }
        }
    }

    @Data
    public static class DateDetail implements Comparable<DateDetail> {
        private Integer days;
        private Integer hours;
        private Integer minutes;

        public DateDetail(Integer days, Integer hours, Integer minutes) {
            this.days = days;
            this.hours = hours;
            this.minutes = minutes;
        }

        public DateDetail(Date date) {
            this.days = date.days;
            this.hours = date.minutes / 60;
            this.minutes = date.minutes % 60;
        }

        public DateDetail(int minutes) {
            this.days = minutes / 1440;
            this.hours = minutes / 60;
            this.minutes = minutes % 60;
        }

        @Override
        public String toString() {
            return "DateDetail{" +
                "days=" + days +
                ", hours=" + hours +
                ", minutes=" + minutes +
                '}';
        }

        @Override
        public int compareTo(DateDetail o) {
            int d = this.days.compareTo(o.days);
            int h = this.hours.compareTo(o.hours);
            if (d != 0) {
                return d;
            } else if (h != 0) {
                return h;
            } else {
                return this.minutes.compareTo(o.minutes);
            }
        }
    }
}
