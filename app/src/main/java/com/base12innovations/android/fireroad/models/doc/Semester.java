package com.base12innovations.android.fireroad.models.doc;

import java.util.Objects;

public class Semester {

    public enum Season{
        Fall(0),IAP(1),Spring(2),Summer(3);
        private int rawvalue;
        Season(int rawvalue){
            this.rawvalue = rawvalue;
        }
        public Season next(){
            return values()[(rawvalue+1)%4];
        }
        public Season prev(){
            return values()[(rawvalue+3)%4];
        }
    }

    private boolean isPriorCredit;
    private int year;
    private Season season;

    public boolean isPriorCredit() {
        return isPriorCredit;
    }
    public Season getSeason() {
        return season;
    }
    public int getYear() {
        return year;
    }

    public static class SemesterID{
        public static final String fall = "fall-";
        public static final String spring = "spring-";
        public static final String iap = "iap-";
        public static final String summer = "summer-";
        public static final String priorCredit = "prior-credit";
    }

    public Semester(){
        this(false);
    }

    public Semester(boolean isPriorCredit){
        init(isPriorCredit);
    }

    public Semester(String semesterID){
        if(semesterID.equals(SemesterID.priorCredit)){
            init(true);
        }else {
            int year = -1;
            Season season = null;
            if (semesterID.startsWith(SemesterID.fall)) {
                year = Integer.parseInt(semesterID.substring(SemesterID.fall.length()));
                season = Season.Fall;
            } else if (semesterID.startsWith(SemesterID.iap)) {
                year = Integer.parseInt(semesterID.substring(SemesterID.iap.length()));
                season = Season.IAP;
            } else if (semesterID.startsWith(SemesterID.spring)) {
                year = Integer.parseInt(semesterID.substring(SemesterID.spring.length()));
                season = Season.Spring;
            } else if (semesterID.startsWith(SemesterID.summer)) {
                year = Integer.parseInt(semesterID.substring(SemesterID.summer.length()));
                season = Season.Summer;
            }
            init(year,season);
        }
    }

    public Semester(int year, Season season){
        init(year,season);
    }

    private void init(int year, Season season){
        isPriorCredit = false;
        this.year = year;
        this.season = season;
    }

    private void init(boolean isPriorCredit){
        this.isPriorCredit = isPriorCredit;
        this.year = -1;
        this.season = null;
    }
    public Semester(int oldSemesterIndex){
        if(oldSemesterIndex == 0){
            init(true);
        }else if(oldSemesterIndex > 0){
            int year = (oldSemesterIndex+2)/3;
            int season = (oldSemesterIndex+2)%3;
            init(year,Season.values()[season]);
        }else{
            init(false);
        }
    }
    int oldSemesterIndex(){
        if(isPriorCredit)
            return 0;
        if(season == Season.Summer)
            return 0;
        if(year > 5)
            return 0;
        return year*3+season.rawvalue-2;
    }

    public Semester prevSemester(){
        if(isPriorCredit)
            return new Semester();
        if(year == 1 && season == Season.Fall)
            return new Semester(true);
        Season newSeason = season.prev();
        int newYear = year;
        if(newSeason == Season.Summer)
            newYear--;
        return new Semester(newYear,newSeason);
    }

    public Semester nextSemester(){
        if(isPriorCredit)
            return new Semester(1,Season.Fall);
        Season newSeason = season.next();
        int newYear = year;
        if(newSeason == Season.Fall)
            newYear++;
        return new Semester(newYear,newSeason);
    }

    public String semesterID(){
        if(isPriorCredit)
            return SemesterID.priorCredit;
        switch(season) {
            case Fall:
                return SemesterID.fall + year;
            case IAP:
                return SemesterID.iap + year;
            case Spring:
                return SemesterID.spring + year;
            case Summer:
                return SemesterID.summer + year;
        }
        return "Invalid Semester ID";
    }

    public boolean isBefore(Semester otherSemester){
        if(isPriorCredit)
            return !otherSemester.isPriorCredit;
        if(year < otherSemester.year){
            return true;
        }else return year == otherSemester.year && season.rawvalue < otherSemester.season.rawvalue;
    }
    public boolean isBeforeOrEqual(Semester otherSemester){
        if(isPriorCredit)
            return true;
        if(year < otherSemester.year){
            return true;
        }else return year == otherSemester.year && season.rawvalue <= otherSemester.season.rawvalue;
    }

    public static String yearString(int year){
        int ones = year % 10;
        int tens = year / 10;
        String modifier = "th";
        if(tens != 1){
            switch(ones){
                case 1:{
                    modifier = "st";
                    break;
                }
                case 2:{
                    modifier = "nd";
                    break;
                }
                case 3:{
                    modifier = "rd";
                    break;
                }
            }
        }
        return year + modifier + " Year";
    }

    @Override
    public String toString(){
        if(isPriorCredit)
            return "Prior Credit";
        return yearString(year) + " " + season;
    }

    @Override
    public int hashCode(){
        return Objects.hash(isPriorCredit,year,season);
    }

    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof  Semester)){
            return false;
        }
        Semester semesterObj = (Semester) obj;
        return semesterObj.isPriorCredit == this.isPriorCredit
                && semesterObj.year == this.year
                && semesterObj.season == this.season;
    }

}

