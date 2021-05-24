package service;

import com.google.gson.Gson;
import model.Schedule;
import model.Ship;
import utils.Constants.Date;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import static utils.Constants.JSON_PATH;

public class Service2 {
    private final Gson gson = new Gson();
    private final Scanner in = new Scanner(System.in);
    private final Service1 service1 = new Service1();
    private final Service3 service3 = new Service3();
    private Schedule schedule;

    public void run(int count) {
        schedule = service1.generateSchedule(count);
        addingSomeRecords();
        writeScheduleToJson();
        Service3.TYPES_RETURN typesReturn = service3.run();
    }

    private void addingSomeRecords() {
        System.out.print("\nEnter count of adding records: ");
        int count = in.nextInt();

        for (int i = 0; i < count; i++) {
            System.out.println("Enter records: ");
            System.out.print("ArrivalTime (days [-8; 30], minutes[0;1439]). ONLY INTEGER ");
            int dateD, dateM;
            do {
                System.out.println("Enter dateD: ");
                dateD = in.nextInt();
            } while (dateD < -8 || dateD > 30);
            do {
                System.out.println("Enter dateM: ");
                dateM = in.nextInt();
            } while (dateM < 0 || dateM > 1439);

            System.out.print("Name: ");
            String name = in.next();
            System.out.print("Type cargo (CONTAINER(0)/LIQUID(1)/LOOSE(2)). ONLY INTEGER");
            int pos;
            do {
                System.out.println("Enter type: ");
                pos = in.nextInt();
            } while (pos < 0 || pos > 2);


            Ship.TYPES_CARGO type;
            if (pos == 0) {
                type = Ship.TYPES_CARGO.CONTAINER;
            } else if (pos == 1) {
                type = Ship.TYPES_CARGO.LIQUID;
            } else {
                type = Ship.TYPES_CARGO.LOOSE;
            }
            System.out.print("Quantity [1; 2000]. ONLY INTEGER ");
            int quantity;

            do {
                System.out.println("Enter Quantity: ");
                quantity = in.nextInt();
            } while (quantity < 1 || quantity > 2000);

            int parkingTime = quantity;

            Ship ship = new Ship(
                new Date(dateD, dateM),
                name,
                type,
                quantity,
                parkingTime
            );

            schedule.add(ship);
        }
    }

    private void writeScheduleToJson() {
        String scheduleJson = gson.toJson(schedule);
        try {
            File f = new File(JSON_PATH + "schedule.json");
            if (f.isFile() && !f.delete()) {
                System.out.println("schedule.json not updated. Some error");
            }
            FileWriter file = new FileWriter(JSON_PATH + "schedule.json");
            file.write(scheduleJson);
            System.out.println("schedule.json updated");
            file.flush();
            file.close();
        } catch (
            IOException e) {
            e.printStackTrace();
        }
    }
}

