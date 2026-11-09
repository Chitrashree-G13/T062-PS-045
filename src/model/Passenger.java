
package model;

public class Passenger {

    public enum Gender {
        MALE,
        FEMALE,
        OTHER
    }

    private String passengerId;
    private String name;
    private int age;
    private Gender gender;
    private String adultPassengerId;

    public Passenger(String passengerId, String name,
                     int age, Gender gender) {
        this(passengerId, name, age, gender, null);
    }

    public Passenger(String passengerId, String name,
                     int age, Gender gender,
                     String adultPassengerId) {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Passenger name is required");
        }

        if (age < 0) {
            throw new IllegalArgumentException(
                    "Invalid passenger age");
        }

        if (gender == null) {
            throw new IllegalArgumentException(
                    "Gender is required");
        }

        this.passengerId = passengerId;
        this.name = name.trim();
        this.age = age;
        this.gender = gender;
        this.adultPassengerId = adultPassengerId;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public Gender getGender() {
        return gender;
    }

    public String getAdultPassengerId() {
        return adultPassengerId;
    }

    public boolean isFreeChild() {
        return age < 5;
    }

    public boolean isChild() {
        return age >= 5 && age <= 12;
    }

    public boolean isAdult() {
        return age >= 13;
    }

    @Override
    public String toString() {
        return passengerId + " - " + name
                + " - Age: " + age
                + " - " + gender;
    }
}
