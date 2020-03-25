package com.base12innovations.android.fireroad.models.doc;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class Semester {
    public static int numYears=0;
    public static Map<Semester,String> semesterNames = new LinkedHashMap<>();
    public static Map<Semester,String> semesterIDs = new LinkedHashMap<>();

    public enum Season{
        Fall(0),IAP(1),Spring(2),Summer(3),Undefined(9);
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

    private boolean isValid;
    private boolean isPriorCredit;
    private int year;
    private Season season;

    public boolean isValid() {
        return isValid;
    }
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

    public static void updateNumYears(int newNumYears){
        updateNumYears(newNumYears,false);
    }
    public static void updateNumYears(int newNumYears, boolean allowReduceNumYears){
        if(semesterNames.size() == 0){
            semesterNames.put(new Semester(true),"Prior Credit");
            semesterIDs.put(new Semester(true),SemesterID.priorCredit);
        }
        if(newNumYears > numYears){
            for (int year = numYears+1; year <= newNumYears; year++) {
                for(Season season: Season.values()){
                    Semester newSemester = new Semester(year,season);
                    semesterNames.put(newSemester,newSemester.toString());
                    semesterIDs.put(newSemester,newSemester.semesterID());
                }
            }
            numYears = newNumYears;
        }else if(allowReduceNumYears && newNumYears < numYears){
            numYears = newNumYears;
            for(Semester semester : semesterNames.keySet()){
                if(semester.getYear() > newNumYears){
                    semesterNames.remove(semester);
                    semesterIDs.remove(semester);
                }
            }
        }
    }

    public Semester(){
        this(false);
    }

    public Semester(boolean isPriorCredit){
        init(isPriorCredit,isPriorCredit);
    }

    public Semester(String semesterID, boolean updateNumYears){
        if(semesterID.equals(SemesterID.priorCredit)){
            init(true,true);
        }else {
            int year = -1;
            Season season = Season.Fall;
            if (semesterID.startsWith(SemesterID.fall)) {
                year = Integer.valueOf(semesterID.substring(SemesterID.fall.length()));
                season = Season.Fall;
            } else if (semesterID.startsWith(SemesterID.iap)) {
                year = Integer.valueOf(semesterID.substring(SemesterID.iap.length()));
                season = Season.IAP;
            } else if (semesterID.startsWith(SemesterID.spring)) {
                year = Integer.valueOf(semesterID.substring(SemesterID.spring.length()));
                season = Season.Spring;
            } else if (semesterID.startsWith(SemesterID.summer)) {
                year = Integer.valueOf(semesterID.substring(SemesterID.summer.length()));
                season = Season.Summer;
            }
            init(year,season,updateNumYears);
        }
    }

    public Semester(int oldSemesterIndex, boolean updateNumYears){
        if(oldSemesterIndex == 0){
            init(true,true);
        }else if(oldSemesterIndex > 0){
            int year = (oldSemesterIndex+2)/3;
            int season = (oldSemesterIndex+2)%3;
            init(year,Season.values()[season],updateNumYears);
        }else{
            init(false,false);
        }
    }

    public Semester(int year, Season season){
        init(year,season);
    }

    private void init(int year, Season season) {
        init(year,season,false);
    }
    private void init(int year, Season season, boolean updateNumYears){
        if(updateNumYears)
            updateNumYears(year);
        isValid = year > 0 && year <= numYears;
        if(isValid) {
            this.year = year;
            this.season = season;
        }else{
            this.year = -1;
            this.season = Season.Undefined;
        }
        isPriorCredit = false;
    }

    private void init(boolean isValid, boolean isPriorCredit){
        this.isValid = isValid;
        this.isPriorCredit = isPriorCredit;
        this.year = -1;
        this.season = Season.Undefined;
    }

    public int oldSemesterIndex(){
        if(isValid){
            if(isPriorCredit)
                return 0;
            return year*3+season.rawvalue-2;
        }
        return 0;
    }

    public Semester prevSemester(){
        if(!isValid || isPriorCredit)
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
        if(!isValid)
            return new Semester();
        if(isPriorCredit)
            return new Semester(1,Season.Fall);
        Season newSeason = season.next();
        int newYear = year;
        if(newSeason == Season.Fall)
            newYear++;
        return new Semester(newYear,newSeason);
    }

    public static Semester getLastSemester(){
        return new Semester(numYears,Season.Summer);
    }

    public String semesterID(){
        if(semesterIDs.containsKey(this))
            return semesterIDs.get(this);
        if(isValid){
            if(isPriorCredit)
                return SemesterID.priorCredit;
            switch(season){
                case Fall:
                    return SemesterID.fall + year;
                case IAP:
                    return SemesterID.iap + year;
                case Spring:
                    return SemesterID.spring + year;
                case Summer:
                    return SemesterID.summer + year;
            }
        }
        return "Invalid SemesterID";
    }

    public boolean isBefore(Semester otherSemester){
        if(isPriorCredit)
            return otherSemester.isValid && !otherSemester.isPriorCredit;
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
        return year + modifier;
    }

    @Override
    public String toString(){
        if(semesterNames.containsKey(this))
            return semesterNames.get(this);
        if(!isValid)
            return "Invalid Semester Value";
        if(isPriorCredit)
            return "Prior Credit";
        return yearString(year) + " " + season;
    }

    @Override
    public int hashCode(){
        return Objects.hash(isValid,isPriorCredit,year,season);
    }

    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof  Semester)){
            return false;
        }
        Semester semesterObj = (Semester) obj;
        return semesterObj.isPriorCredit == this.isPriorCredit
                && semesterObj.isValid == this.isValid
                && semesterObj.year == this.year
                && semesterObj.season == this.season;
    }

}

